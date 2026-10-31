package controller;

import dao.ProductDAO;
import dao.ServiceDAO;
import dao.OrderDAO;
import controller.orders.OrderSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Account;
import model.Product;
import model.WorkshopService;
import model.Order;
import model.OrderDetail;

/** Form, tạo đơn và lịch sử/hủy/đánh giá của tài khoản đăng nhập. */
@WebServlet(name = "OrderServlet", urlPatterns = {"/request", "/orders"})
public class OrderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Account user = session != null && session.getAttribute("user") instanceof Account
                ? (Account) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!"CUSTOMER".equals(user.getRole()) || !user.isActive()) {
            reject(request, response, 403, "Bạn không có quyền gửi yêu cầu dịch vụ.");
            return;
        }

        if ("/orders".equals(request.getServletPath())) {
            showHistory(request, response, user);
            return;
        }

        Integer serviceId = parseServiceId(request.getParameterValues("serviceId"));
        if (serviceId == null) {
            reject(request, response, 400, "Vui lòng chọn một dịch vụ hợp lệ từ trang dịch vụ.");
            return;
        }
        loadForm(request, response, user, serviceId, true);
        render(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Account user = session != null && session.getAttribute("user") instanceof Account
                ? (Account) session.getAttribute("user") : null;
        if (user == null || !"CUSTOMER".equals(user.getRole()) || !user.isActive()) {
            reject(request, response, 403, "Bạn không có quyền gửi yêu cầu dịch vụ.");
            return;
        }
        if ("/orders".equals(request.getServletPath())) {
            if ("review".equals(request.getParameter("action"))) {
                reviewOrder(request, response, user);
            } else {
                cancelOrder(request, response, user);
            }
            return;
        }
        Order input = new Order();
        Set<Integer> selectedIds = new LinkedHashSet<>();
        Map<Integer, String> quantities = new HashMap<>();
        request.setAttribute("selectedPartIds", selectedIds);
        request.setAttribute("quantities", quantities);
        for (String name : List.of("contactName", "contactPhone", "deviceDescription", "customerNote")) {
            request.setAttribute(name, request.getParameter(name));
        }
        request.setAttribute("preferredDateInput", request.getParameter("preferredDate"));
        try {
            input.setServiceId(OrderSupport.positiveInt(
                    OrderSupport.parameter(request, "serviceId", true), "Mã dịch vụ"));
            if (!"create".equals(OrderSupport.parameter(request, "action", true))) {
                throw new IllegalArgumentException("Thao tác biểu mẫu không hợp lệ.");
            }
            input.setContactName(OrderSupport.parameter(request, "contactName", false));
            input.setContactPhone(OrderSupport.parameter(request, "contactPhone", false));
            input.setDeviceDescription(OrderSupport.parameter(request, "deviceDescription", false));
            input.setCustomerNote(OrderSupport.parameter(request, "customerNote", false));
            List<OrderDetail> parts = new ArrayList<>();
            String[] productIds = request.getParameterValues("productIds");
            if (productIds != null) {
                for (String rawId : productIds) {
                    int productId = OrderSupport.positiveInt(rawId, "Mã linh kiện");
                    if (!selectedIds.add(productId)) {
                        throw new IllegalArgumentException("Không chọn trùng một linh kiện trong yêu cầu.");
                    }
                    quantities.put(productId, request.getParameter("quantity_" + productId));
                }
                // Thu draft của mọi dòng trước khi kiểm tra, để lỗi một ô không mất các lựa chọn khác.
                for (int productId : selectedIds) {
                    String rawQuantity = OrderSupport.parameter(request, "quantity_" + productId, true);
                    int quantity = OrderSupport.positiveInt(rawQuantity, "Số lượng");
                    if (quantity > 200) {
                        throw new IllegalArgumentException("Số lượng mỗi linh kiện phải từ 1 đến 200.");
                    }
                    OrderDetail detail = new OrderDetail();
                    detail.setProductId(productId);
                    detail.setQuantity(quantity);
                    parts.add(detail);
                }
            }
            String date = OrderSupport.parameter(request, "preferredDate", false);
            if (!date.isBlank()) {
                try {
                    input.setPreferredDate(LocalDate.parse(date));
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Ngày mong muốn không đúng định dạng ngày hợp lệ.");
                }
            }
            String formToken = OrderSupport.parameter(request, "orderFormToken", false);
            synchronized (session) {
                if (!OrderSupport.validFormToken(session, formToken)) {
                    reject(request, response, 409, "Biểu mẫu đã gửi hoặc đã được thay thế. Vui lòng kiểm tra lịch sử hoặc mở lại dịch vụ.");
                    return;
                }
                try (OrderDAO dao = new OrderDAO()) {
                    dao.createOrder(input, parts, user.getAccountId());
                } catch (SQLException ex) {
                    if (input.getOrderId() == 0) {
                        throw ex;
                    }
                    // Commit đã thành công nhưng khôi phục/đóng connection lỗi: không cho tạo lại.
                    getServletContext().log("Đơn đã commit nhưng đóng connection có lỗi.", ex);
                }
                session.removeAttribute("orderFormToken");
                session.setAttribute("orderMessage", "Đã gửi yêu cầu #" + input.getOrderId() + ".");
            }
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        } catch (IllegalArgumentException ex) {
            response.setStatus(400);
            request.setAttribute("errorMessage", ex.getMessage());
        } catch (SQLException ex) {
            getServletContext().log("Không thể lưu yêu cầu dịch vụ.", ex);
            response.setStatus(503);
            request.setAttribute("errorMessage", "Chưa thể lưu yêu cầu lúc này. Vui lòng thử lại sau.");
        }
        if (input.getServiceId() > 0) {
            loadForm(request, response, user, input.getServiceId(), false);
        }
        render(request, response);
    }

    private void reviewOrder(HttpServletRequest request, HttpServletResponse response, Account user)
            throws ServletException, IOException {
        try {
            if (!"review".equals(OrderSupport.parameter(request, "action", true))) {
                throw new IllegalArgumentException("Thao tác đánh giá không hợp lệ.");
            }
            int orderId = OrderSupport.positiveInt(OrderSupport.parameter(request, "orderId", true), "Mã đơn");
            request.setAttribute("reviewOrderId", orderId);
            request.setAttribute("ratingInput", request.getParameter("rating"));
            request.setAttribute("commentInput", request.getParameter("comment"));
            int rating = OrderSupport.positiveInt(OrderSupport.parameter(request, "rating", true), "Số sao");
            if (rating > 5) {
                throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5 sao.");
            }
            String comment = OrderSupport.parameter(request, "comment", false);
            boolean inserted;
            try (OrderDAO dao = new OrderDAO()) {
                inserted = dao.insertReview(orderId, user.getAccountId(), rating, comment);
            }
            if (inserted) {
                HttpSession session = request.getSession(false);
                synchronized (session) {
                    session.setAttribute("orderMessage", "Đã gửi đánh giá cho yêu cầu #" + orderId + ".");
                }
                response.sendRedirect(request.getContextPath() + "/orders#order-" + orderId);
                return;
            }
            response.setStatus(409);
            request.setAttribute("errorMessage", "Chỉ có thể đánh giá một lần cho đơn đã hoàn tất của chính bạn.");
        } catch (IllegalArgumentException ex) {
            response.setStatus(400);
            request.setAttribute("errorMessage", ex.getMessage());
        } catch (SQLException ex) {
            getServletContext().log("Không thể lưu đánh giá dịch vụ.", ex);
            response.setStatus(503);
            request.setAttribute("errorMessage", "Chưa thể lưu đánh giá lúc này. Vui lòng thử lại sau.");
        }
        showHistory(request, response, user);
    }

    private void cancelOrder(HttpServletRequest request, HttpServletResponse response, Account user)
            throws ServletException, IOException {
        try {
            if (!"cancel".equals(OrderSupport.parameter(request, "action", true))) {
                throw new IllegalArgumentException("Thao tác đơn không hợp lệ.");
            }
            int orderId = OrderSupport.positiveInt(OrderSupport.parameter(request, "orderId", true), "Mã đơn");
            boolean cancelled;
            try (OrderDAO dao = new OrderDAO()) {
                cancelled = dao.cancelPending(orderId, user.getAccountId());
            }
            if (cancelled) {
                HttpSession session = request.getSession(false);
                synchronized (session) {
                    session.setAttribute("orderMessage", "Đã hủy yêu cầu #" + orderId + ".");
                }
                response.sendRedirect(request.getContextPath() + "/orders");
                return;
            }
            response.setStatus(409);
            request.setAttribute("errorMessage", "Không thể hủy: đơn không thuộc tài khoản của bạn hoặc không còn chờ tiếp nhận.");
        } catch (IllegalArgumentException ex) {
            response.setStatus(400);
            request.setAttribute("errorMessage", ex.getMessage());
        } catch (SQLException ex) {
            getServletContext().log("Không thể hủy yêu cầu dịch vụ.", ex);
            response.setStatus(503);
            request.setAttribute("errorMessage", "Chưa thể hủy yêu cầu lúc này. Vui lòng thử lại sau.");
        }
        showHistory(request, response, user);
    }

    private void showHistory(HttpServletRequest request, HttpServletResponse response, Account user)
            throws ServletException, IOException {
        request.setAttribute("page", "orders");
        request.setAttribute("dateFormat", DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        request.setAttribute("dateTimeFormat", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        try (OrderDAO dao = new OrderDAO()) {
            // Không nhận accountId/orderId từ URL; chi tiết chỉ đọc theo các đơn thuộc owner này.
            List<Order> orders = dao.getOrdersByAccount(user.getAccountId());
            request.setAttribute("orders", orders);
            request.setAttribute("historyLoaded", true);
            if (request.getAttribute("errorMessage") == null) {
                HttpSession session = request.getSession(false);
                synchronized (session) {
                    Object message = session.getAttribute("orderMessage");
                    if (message instanceof String) {
                        request.setAttribute("message", message);
                        session.removeAttribute("orderMessage");
                    }
                }
            }
        } catch (SQLException ex) {
            getServletContext().log("Không thể đọc lịch sử yêu cầu dịch vụ.", ex);
            response.setStatus(503);
            request.removeAttribute("orders");
            request.setAttribute("historyLoaded", false);
            request.setAttribute("errorMessage", "Chưa thể tải lịch sử yêu cầu lúc này. Vui lòng thử lại sau.");
        }
        request.getRequestDispatcher("/WEB-INF/views/request-history.jsp").forward(request, response);
    }

    private void loadForm(HttpServletRequest request, HttpServletResponse response, Account user,
            int serviceId, boolean newForm) {
        try (ServiceDAO services = new ServiceDAO()) {
            WorkshopService selectedService = services.getById(serviceId);
            if (selectedService == null || !selectedService.isActive()) {
                if (response.getStatus() == 200) {
                    response.setStatus(404);
                }
                request.setAttribute("errorMessage", "Dịch vụ không tồn tại hoặc hiện đã ngừng nhận yêu cầu.");
                return;
            }
            List<Product> products;
            try (ProductDAO parts = new ProductDAO()) {
                products = parts.getActiveProducts();
            }
            request.setAttribute("selectedService", selectedService);
            request.setAttribute("products", products);
            if (newForm) {
                request.setAttribute("contactName", user.getFullName());
                request.setAttribute("contactPhone", user.getPhone());
            }
            if (request.getAttribute("selectedPartIds") == null) {
                request.setAttribute("selectedPartIds", new HashSet<Integer>());
                request.setAttribute("quantities", new HashMap<Integer, String>());
            }
            request.setAttribute("today", LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString());
            HttpSession session = request.getSession(false);
            synchronized (session) {
                request.setAttribute("orderFormToken", newForm ? OrderSupport.issueFormToken(session)
                        : session.getAttribute("orderFormToken"));
            }
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải biểu mẫu yêu cầu dịch vụ.", ex);
            // Không hiển thị form với catalog thiếu hoặc dữ liệu giả khi DB lỗi.
            request.removeAttribute("selectedService");
            request.removeAttribute("products");
            response.setStatus(503);
            request.setAttribute("errorMessage", "Chưa thể tải biểu mẫu lúc này. Vui lòng thử lại sau.");
        }
    }

    private Integer parseServiceId(String[] values) {
        if (values == null || values.length != 1 || !values[0].matches("[0-9]{1,10}")) {
            return null;
        }
        try {
            int id = Integer.parseInt(values[0]);
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("errorMessage", message);
        render(request, response);
    }

    private void render(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("page", "request");
        request.getRequestDispatcher("/WEB-INF/views/request-form.jsp").forward(request, response);
    }
}
