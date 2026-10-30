package controller;

import dao.ProductDAO;
import dao.ServiceDAO;
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
import java.util.List;
import model.Account;
import model.Product;
import model.WorkshopService;

/** P3-02: đọc form yêu cầu; xử lý POST và lưu đơn thuộc bước tiếp theo. */
@WebServlet(name = "OrderServlet", urlPatterns = {"/request"})
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

        Integer serviceId = parseServiceId(request.getParameterValues("serviceId"));
        if (serviceId == null) {
            reject(request, response, 400, "Vui lòng chọn một dịch vụ hợp lệ từ trang dịch vụ.");
            return;
        }
        try (ServiceDAO services = new ServiceDAO()) {
            WorkshopService selectedService = services.getById(serviceId);
            if (selectedService == null || !selectedService.isActive()) {
                reject(request, response, 404, "Dịch vụ không tồn tại hoặc hiện đã ngừng nhận yêu cầu.");
                return;
            }
            List<Product> products;
            try (ProductDAO parts = new ProductDAO()) {
                products = parts.getActiveProducts();
            }
            request.setAttribute("selectedService", selectedService);
            request.setAttribute("products", products);
            request.setAttribute("contactName", user.getFullName());
            request.setAttribute("contactPhone", user.getPhone());
            request.setAttribute("today", LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString());
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải biểu mẫu yêu cầu dịch vụ.", ex);
            // Không hiển thị form với catalog thiếu hoặc dữ liệu giả khi DB lỗi.
            request.removeAttribute("selectedService");
            request.removeAttribute("products");
            reject(request, response, 503, "Chưa thể tải biểu mẫu lúc này. Vui lòng thử lại sau.");
            return;
        }
        render(request, response);
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
