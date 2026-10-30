package controller.admin;

import dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import model.Account;

@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof Account) || !((Account) value).isActive() || !"ADMIN".equals(((Account) value).getRole())) {
            response.sendError(403);
            return;
        }
        request.setAttribute("page", "admin-dashboard");
        try (OrderDAO dao = new OrderDAO()) {
            request.setAttribute("statistics", dao.getStatistics());
        } catch (SQLException ex) {
            getServletContext().log("Không thể đọc thống kê dashboard.", ex);
            request.removeAttribute("statistics");
            response.setStatus(503);
            request.setAttribute("errorMessage", "Chưa thể tải thống kê lúc này. Vui lòng thử lại sau.");
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(request, response);
    }
}
