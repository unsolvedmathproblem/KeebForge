package controller.admin;

import dao.AccountDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import model.Account;

@WebServlet(name = "AdminUserServlet", urlPatterns = "/admin/users")
public class AdminUserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if ("1".equals(request.getParameter("updated"))) {
            request.setAttribute("message", "Đã cập nhật trạng thái tài khoản.");
        }
        showUsers(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Account admin = currentAdmin(request);
        if (admin == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int accountId;
        String activeValue = request.getParameter("active");
        try {
            accountId = Integer.parseInt(request.getParameter("accountId"));
            if (accountId <= 0 || !"toggle".equals(request.getParameter("action"))
                    || !("true".equals(activeValue) || "false".equals(activeValue))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", "Thao tác hoặc thông tin tài khoản không hợp lệ.");
            showUsers(request, response);
            return;
        }
        boolean active = "true".equals(activeValue);
        if (accountId == admin.getAccountId() && !active) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", "Bạn không thể tự khóa tài khoản đang đăng nhập.");
            showUsers(request, response);
            return;
        }
        try (AccountDAO dao = new AccountDAO()) {
            if (!dao.updateStatus(accountId, active)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("errorMessage", "Không tìm thấy tài khoản cần cập nhật.");
            } else {
                // Gửi trạng thái đích, không đảo BIT; gửi lại cùng form không mở/khóa ngược.
                response.sendRedirect(request.getContextPath() + "/admin/users?updated=1");
                return;
            }
        } catch (SQLException ex) {
            getServletContext().log("Không thể đổi trạng thái tài khoản.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể cập nhật trạng thái. Vui lòng thử lại sau.");
        }
        showUsers(request, response);
    }

    private void showUsers(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (AccountDAO dao = new AccountDAO()) {
            request.setAttribute("accounts", dao.getAllAccounts());
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh sách tài khoản.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể tải danh sách tài khoản. Vui lòng thử lại sau.");
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(request, response);
    }

    private Account currentAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") instanceof Account) {
            Account user = (Account) session.getAttribute("user");
            if ("ADMIN".equals(user.getRole())) {
                return user;
            }
        }
        return null;
    }
}
