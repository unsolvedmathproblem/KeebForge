package controller.admin;

import controller.orders.OrderSupport;
import dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import model.Account;

@WebServlet(name = "AdminOrderServlet", urlPatterns = {"/admin/orders"})
public class AdminOrderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(403);
            return;
        }
        try {
            request.setAttribute("statusFilter", filter(request));
            loadOrders(request, response);
        } catch (IllegalArgumentException ex) {
            response.setStatus(400);
            request.setAttribute("errorMessage", ex.getMessage());
        }
        render(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            response.sendError(403);
            return;
        }
        try {
            String status = filter(request);
            request.setAttribute("statusFilter", status);
            int orderId = OrderSupport.positiveInt(OrderSupport.parameter(request, "orderId", true), "Mã đơn");
            String action = OrderSupport.parameter(request, "action", true);
            boolean updated;
            if ("status".equals(action)) {
                String newStatus = OrderSupport.parameter(request, "newStatus", true);
                try (OrderDAO dao = new OrderDAO()) {
                    updated = dao.updateStatus(orderId, newStatus);
                }
            } else if ("note".equals(action)) {
                request.setAttribute("noteOrderId", orderId);
                request.setAttribute("noteInput", request.getParameter("adminNote"));
                String note = OrderSupport.parameter(request, "adminNote", false);
                try (OrderDAO dao = new OrderDAO()) {
                    updated = dao.updateAdminNote(orderId, note);
                }
            } else {
                throw new IllegalArgumentException("Thao tác quản lý đơn không hợp lệ.");
            }
            if (updated) {
                HttpSession session = request.getSession(false);
                synchronized (session) {
                    session.setAttribute("adminOrderMessage", "Đã cập nhật "
                            + ("status".equals(action) ? "trạng thái" : "ghi chú") + " yêu cầu #" + orderId + ".");
                }
                response.sendRedirect(request.getContextPath() + "/admin/orders"
                        + (status.isEmpty() ? "" : "?status=" + status) + "#order-" + orderId);
                return;
            }
            response.setStatus(409);
            request.setAttribute("errorMessage", "Không thể cập nhật: đơn không tồn tại hoặc trạng thái đã thay đổi.");
        } catch (IllegalArgumentException ex) {
            response.setStatus(400);
            request.setAttribute("errorMessage", ex.getMessage());
        } catch (SQLException ex) {
            getServletContext().log("Không thể cập nhật đơn tại AdminOrderServlet.", ex);
            response.setStatus(503);
            request.setAttribute("errorMessage", "Chưa thể cập nhật yêu cầu lúc này. Vui lòng thử lại sau.");
        }
        if (request.getAttribute("statusFilter") != null) {
            loadOrders(request, response);
        }
        render(request, response);
    }

    private String filter(HttpServletRequest request) {
        String status = OrderSupport.parameter(request, "status", false).strip();
        if (!status.isEmpty() && !List.of("PENDING", "PROCESSING", "COMPLETED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Bộ lọc trạng thái không hợp lệ.");
        }
        return status;
    }

    private void loadOrders(HttpServletRequest request, HttpServletResponse response) {
        try (OrderDAO dao = new OrderDAO()) {
            request.setAttribute("orders", dao.getAllOrders((String) request.getAttribute("statusFilter")));
            request.setAttribute("ordersLoaded", true);
            if (request.getAttribute("errorMessage") == null) {
                HttpSession session = request.getSession(false);
                synchronized (session) {
                    Object message = session.getAttribute("adminOrderMessage");
                    if (message instanceof String) {
                        request.setAttribute("message", message);
                        session.removeAttribute("adminOrderMessage");
                    }
                }
            }
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh sách đơn Admin.", ex);
            request.removeAttribute("orders");
            request.setAttribute("ordersLoaded", false);
            response.setStatus(503);
            request.setAttribute("errorMessage", "Chưa thể tải danh sách yêu cầu lúc này. Vui lòng thử lại sau.");
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("page", "admin-orders");
        request.setAttribute("dateFormat", DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        request.setAttribute("dateTimeFormat", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        request.getRequestDispatcher("/WEB-INF/views/admin/orders.jsp").forward(request, response);
    }

    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        return value instanceof Account && ((Account) value).isActive() && "ADMIN".equals(((Account) value).getRole());
    }
}
