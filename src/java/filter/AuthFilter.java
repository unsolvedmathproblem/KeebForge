package filter;

import dao.AccountDAO;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import model.Account;

/** Một filter chung cho encoding, CSRF và quyền truy cập. */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/*")
public class AuthFilter implements Filter {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        String path = request.getServletPath();
        boolean readOnly = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        if (readOnly && (path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || "/favicon.ico".equals(path))) {
            chain.doFilter(request, response);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession();
        synchronized (session) {
            if (!(session.getAttribute("csrfToken") instanceof String)) {
                byte[] random = new byte[32];
                RANDOM.nextBytes(random);
                session.setAttribute("csrfToken", Base64.getUrlEncoder().withoutPadding().encodeToString(random));
            }
        }

        boolean adminPath = "/admin".equals(path) || path.startsWith("/admin/");
        boolean customerPath = "/request".equals(path) || "/orders".equals(path);
        if (adminPath || customerPath || "/profile".equals(path)) {
            if (!(session.getAttribute("user") instanceof Account)) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            Account user = (Account) session.getAttribute("user");
            try (AccountDAO dao = new AccountDAO()) {
                Account current = dao.getById(user.getAccountId());
                if (current == null || !current.isActive()) {
                    session.invalidate();
                    response.sendRedirect(request.getContextPath() + "/login?inactive=1");
                    return;
                }
                current.setPasswordHash(null);
                session.setAttribute("user", current);
                if ((adminPath && !"ADMIN".equals(current.getRole()))
                        || (customerPath && !"CUSTOMER".equals(current.getRole()))) {
                    reject(request, response, 403, "Bạn không có quyền truy cập trang này.");
                    return;
                }
            } catch (SQLException ex) {
                request.getServletContext().log("Không thể kiểm tra tài khoản tại AuthFilter.", ex);
                reject(request, response, 503, "Chưa thể kiểm tra tài khoản lúc này. Vui lòng thử lại sau.");
                return;
            }
        }

        if ("POST".equals(request.getMethod())) {
            String expected = (String) session.getAttribute("csrfToken");
            String submitted = request.getParameter("csrf");
            if (expected == null || submitted == null || submitted.length() != expected.length()
                    || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                            submitted.getBytes(StandardCharsets.UTF_8))) {
                reject(request, response, 403, "Phiên biểu mẫu không hợp lệ hoặc đã hết hạn. Vui lòng mở lại trang và thử lại.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/WEB-INF/views/common/error.jsp").forward(request, response);
    }
}
