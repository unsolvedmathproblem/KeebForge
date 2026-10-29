package controller;

import dao.AccountDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import model.Account;
import util.PasswordUtil;

@WebServlet(name = "AuthServlet", urlPatterns = {"/login", "/register", "/profile", "/logout"})
public class AuthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        noCache(response);
        switch (request.getServletPath()) {
            case "/login":
            case "/register":
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("user") instanceof Account) {
                    response.sendRedirect(request.getContextPath() + "/profile");
                    return;
                }
                if ("/login".equals(request.getServletPath())) {
                    if ("1".equals(request.getParameter("registered"))) {
                        request.setAttribute("message", "Đăng ký thành công. Bạn có thể đăng nhập ngay.");
                    } else if ("1".equals(request.getParameter("loggedOut"))) {
                        request.setAttribute("message", "Bạn đã đăng xuất.");
                    } else if ("1".equals(request.getParameter("inactive"))) {
                        request.setAttribute("errorMessage", "Tài khoản không còn hoạt động. Vui lòng liên hệ quản trị viên.");
                    }
                }
                request.getRequestDispatcher("/WEB-INF/views" + request.getServletPath() + ".jsp")
                        .forward(request, response);
                break;
            case "/profile":
                showProfile(request, response);
                break;
            default:
                response.setHeader("Allow", "POST");
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        noCache(response);
        switch (request.getServletPath()) {
            case "/login":
                login(request, response);
                break;
            case "/register":
                register(request, response);
                break;
            case "/profile":
                updateProfile(request, response);
                break;
            case "/logout":
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect(request.getContextPath() + "/login?loggedOut=1");
                break;
            default:
                response.setHeader("Allow", "GET");
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = trimmed(request, "username");
        String password = request.getParameter("password");
        request.setAttribute("username", username);
        if (username.isEmpty() || username.length() > 50 || password == null
                || password.isEmpty() || password.length() > 128) {
            showError(request, response, "login", "Vui lòng nhập tên đăng nhập và mật khẩu hợp lệ.");
            return;
        }
        try (AccountDAO dao = new AccountDAO()) {
            Account account = dao.getByUsername(username);
            if (account == null || !PasswordUtil.verifyPassword(password, account.getPasswordHash())) {
                showError(request, response, "login", "Tên đăng nhập hoặc mật khẩu không đúng.");
                return;
            }
            if (!account.isActive()) {
                showError(request, response, "login", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
                return;
            }
            // Chỉ giữ thông tin cần dùng trong session, không giữ hash mật khẩu.
            account.setPasswordHash(null);
            HttpSession session = request.getSession();
            request.changeSessionId();
            // Token trước khi đăng nhập không còn dùng cho các form sau login.
            session.removeAttribute("csrfToken");
            session.setAttribute("user", account);
            response.sendRedirect(request.getContextPath() + "/profile");
        } catch (SQLException ex) {
            getServletContext().log("Không thể truy vấn tài khoản khi đăng nhập.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            showError(request, response, "login", "Chưa thể đăng nhập lúc này. Vui lòng thử lại sau.");
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = trimmed(request, "username");
        String email = trimmed(request, "email").toLowerCase(Locale.ROOT);
        String fullName = trimmed(request, "fullName");
        String phone = trimmed(request, "phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        request.setAttribute("username", username);
        request.setAttribute("email", email);
        request.setAttribute("fullName", fullName);
        request.setAttribute("phone", phone);

        String error = null;
        if (!username.matches("[A-Za-z0-9_]{3,50}")) {
            error = "Tên đăng nhập cần 3–50 ký tự, chỉ gồm chữ không dấu, số và dấu gạch dưới.";
        } else if (email.length() > 100 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            error = "Vui lòng nhập email hợp lệ, tối đa 100 ký tự.";
        } else if (fullName.isEmpty() || fullName.length() > 100) {
            error = "Họ tên cần từ 1 đến 100 ký tự.";
        } else if (!validPhone(phone)) {
            error = "Số điện thoại cần 7–20 ký tự, gồm số và dấu +, khoảng trắng, dấu ngoặc hoặc gạch nối.";
        } else if (password == null || password.length() < 8 || password.length() > 128) {
            error = "Mật khẩu cần từ 8 đến 128 ký tự.";
        } else if (!password.equals(confirmPassword)) {
            error = "Mật khẩu nhập lại chưa khớp.";
        }
        if (error != null) {
            showError(request, response, "register", error);
            return;
        }

        try (AccountDAO dao = new AccountDAO()) {
            if (dao.existsUsernameOrEmail(username, email)) {
                showError(request, response, "register", "Tên đăng nhập hoặc email đã được sử dụng.");
                return;
            }
            Account account = new Account();
            account.setUsername(username);
            account.setEmail(email);
            account.setFullName(fullName);
            account.setPhone(phone.isEmpty() ? null : phone);
            account.setPasswordHash(PasswordUtil.hashPassword(password));
            account.setRole("CUSTOMER");
            account.setActive(true);
            if (!dao.register(account)) {
                showError(request, response, "register", "Chưa thể tạo tài khoản. Vui lòng thử lại.");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/login?registered=1");
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 2601 || ex.getErrorCode() == 2627) {
                // UNIQUE vẫn bảo vệ khi hai request đăng ký cùng lúc.
                showError(request, response, "register", "Tên đăng nhập hoặc email đã được sử dụng.");
            } else {
                getServletContext().log("Không thể tạo tài khoản.", ex);
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                showError(request, response, "register", "Chưa thể đăng ký lúc này. Vui lòng thử lại sau.");
            }
        }
    }

    private void showProfile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof Account)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        // AuthFilter đã đọc lại trạng thái/role từ DB trước request bảo vệ.
        Account account = (Account) session.getAttribute("user");
        request.setAttribute("profileAccount", account);
        request.setAttribute("joinedAt", account.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        if (request.getAttribute("fullName") == null) {
            request.setAttribute("fullName", account.getFullName());
            request.setAttribute("phone", account.getPhone());
        }
        if ("GET".equals(request.getMethod()) && "1".equals(request.getParameter("updated"))) {
            request.setAttribute("message", "Đã cập nhật thông tin hồ sơ.");
        }
        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
    }

    private void updateProfile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof Account)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String fullName = trimmed(request, "fullName");
        String phone = trimmed(request, "phone");
        request.setAttribute("fullName", fullName);
        request.setAttribute("phone", phone);
        if (!"update".equals(request.getParameter("action"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", "Thao tác cập nhật không hợp lệ.");
        } else if (fullName.isEmpty() || fullName.length() > 100) {
            request.setAttribute("errorMessage", "Họ tên cần từ 1 đến 100 ký tự.");
        } else if (!validPhone(phone)) {
            request.setAttribute("errorMessage", "Số điện thoại cần 7–20 ký tự, gồm số và dấu điện thoại thông dụng.");
        }
        if (request.getAttribute("errorMessage") != null) {
            showProfile(request, response);
            return;
        }
        Account user = (Account) session.getAttribute("user");
        try (AccountDAO dao = new AccountDAO()) {
            // Không đọc accountId, role, email hoặc password từ form cập nhật.
            if (!dao.updateProfile(user.getAccountId(), fullName, phone.isEmpty() ? null : phone)) {
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login?inactive=1");
                return;
            }
            user.setFullName(fullName);
            user.setPhone(phone.isEmpty() ? null : phone);
            session.setAttribute("user", user);
            response.sendRedirect(request.getContextPath() + "/profile?updated=1");
        } catch (SQLException ex) {
            getServletContext().log("Không thể cập nhật hồ sơ.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể cập nhật hồ sơ lúc này. Vui lòng thử lại sau.");
            showProfile(request, response);
        }
    }

    private boolean validPhone(String phone) {
        return phone.isEmpty() || (phone.length() <= 20 && phone.matches("\\+?[0-9][0-9 .()-]{6,19}"));
    }

    private String trimmed(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private void showError(HttpServletRequest request, HttpServletResponse response, String view, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    private void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }
}
