package controller.orders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;

/** Một vài helper parse form đơn; CSRF vẫn do AuthFilter P1 kiểm tra. */
public final class OrderSupport {

    private OrderSupport() {
    }

    public static String parameter(HttpServletRequest request, String name, boolean required) {
        String[] values = request.getParameterValues(name);
        if (values == null && !required) {
            return "";
        }
        if (values == null || values.length != 1 || (required && values[0].isBlank())) {
            throw new IllegalArgumentException("Trường " + name + " thiếu hoặc có nhiều giá trị.");
        }
        return values[0];
    }

    public static int positiveInt(String raw, String label) {
        if (raw == null || !raw.matches("[0-9]{1,10}")) {
            throw new IllegalArgumentException(label + " phải là số nguyên dương.");
        }
        try {
            int value = Integer.parseInt(raw);
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ex) {
            // Cùng thông báo cho cả sai định dạng và tràn INT.
        }
        throw new IllegalArgumentException(label + " phải là số nguyên dương trong giới hạn INT.");
    }

    /** Caller giữ synchronized(session) khi phát, kiểm tra và tiêu token. */
    public static String issueFormToken(HttpSession session) {
        String token = UUID.randomUUID().toString();
        session.setAttribute("orderFormToken", token);
        return token;
    }

    public static boolean validFormToken(HttpSession session, String submitted) {
        Object expected = session.getAttribute("orderFormToken");
        return expected instanceof String && expected.equals(submitted);
    }
}
