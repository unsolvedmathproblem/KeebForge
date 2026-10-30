import dao.AccountDAO;
import dao.OrderDAO;
import dao.ProductDAO;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.Account;
import model.Order;
import model.OrderDetail;
import model.Product;
import util.PasswordUtil;

/** Kiểm tra owner/history/cancel trên JDBC và HTTP thật; fixture riêng, luôn dọn trong finally. */
public class VerifyP304 extends OrderDAO {

    private static int checks;
    private final String marker = "P304_" + UUID.randomUUID().toString().replace("-", "");
    private final List<Integer> owners = new ArrayList<>();
    private final List<Integer> products = new ArrayList<>();
    private int service;
    private int pending;
    private int processing;
    private int completed;
    private int cancelled;
    private int concurrent;
    private int otherPending;
    private String baseUrl;
    private HttpClient a;
    private HttpClient b;

    public VerifyP304() throws SQLException {
        super();
    }

    public static void main(String[] args) throws Exception {
        try (VerifyP304 test = new VerifyP304()) {
            test.baseUrl = args.length == 0 ? "http://127.0.0.1:18080/KeebForce" : args[0];
            String before = test.counts();
            try {
                test.setup();
                test.daoChecks();
                test.httpChecks();
                if (args.length > 1 && !"-".equals(args[1])) test.dbFailure(Path.of(args[1]));
                if (List.of(args).contains("--preview")) {
                    test.preparePreview();
                    System.out.println("PREVIEW_USERNAME=" + test.marker + "_a");
                    System.out.println("PREVIEW_READY (Enter to clean up fixtures)");
                    System.in.read();
                }
            } finally {
                test.cleanup();
            }
            check(before.equals(test.counts()), "Fixture cleanup restores row counts of all 8 tables");
        }
        System.out.println("PASS " + checks + " JDBC/HTTP checks.");
    }

    private void setup() throws Exception {
        String hash = PasswordUtil.hashPassword("Fixture@123");
        try (AccountDAO accounts = new AccountDAO()) {
            for (String suffix : List.of("a", "b", "empty")) {
                Account account = new Account();
                account.setUsername(marker + "_" + suffix);
                account.setEmail(account.getUsername() + "@example.invalid");
                account.setFullName("Khách " + suffix + " " + marker);
                account.setPhone("0901234567");
                account.setPasswordHash(hash);
                if (!accounts.register(account)) throw new AssertionError("Fixture registration failed");
                owners.add(account.getAccountId());
            }
        }
        service = insert("INSERT INTO dbo.WorkshopService (ServiceName, Description, BasePrice) VALUES (?, ?, 200000)",
                marker + " Service <script>", "Dịch vụ thử");
        Product seed;
        try (ProductDAO parts = new ProductDAO()) {
            seed = parts.getActiveProducts().get(0);
        }
        for (String price : List.of("5000", "300000")) {
            products.add(insert("INSERT INTO dbo.Product (ProductName, CategoryID, BrandID, Price) VALUES (?, ?, ?, ?)",
                    marker + " Part <&> " + products.size(), seed.getCategoryId(), seed.getBrandId(), new BigDecimal(price)));
        }
        pending = fixtureOrder(0, "PENDING", true);
        processing = fixtureOrder(0, "PROCESSING", false);
        completed = fixtureOrder(0, "COMPLETED", false);
        cancelled = fixtureOrder(0, "CANCELLED", false);
        concurrent = fixtureOrder(0, "PENDING", false);
        otherPending = fixtureOrder(1, "PENDING", false);
        update("UPDATE dbo.WorkshopService SET IsActive=0, BasePrice=999000 WHERE ServiceID=?", service);
        for (int id : products) update("UPDATE dbo.Product SET IsActive=0, Price=990000 WHERE ProductID=?", id);
    }

    private int fixtureOrder(int ownerIndex, String status, boolean withParts) throws SQLException {
        Order input = new Order();
        input.setServiceId(service);
        input.setContactName((ownerIndex == 0 ? "A_PRIVATE_" : "B_PRIVATE_") + marker);
        input.setContactPhone("0901234567");
        input.setDeviceDescription("Bàn phím <script>alert(1)</script> & thiết bị riêng");
        input.setCustomerNote("Ghi chú <img src=x onerror=alert(1)>");
        input.setPreferredDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2));
        List<OrderDetail> details = new ArrayList<>();
        if (withParts) {
            for (int i = 0; i < products.size(); i++) {
                OrderDetail detail = new OrderDetail();
                detail.setProductId(products.get(i));
                detail.setQuantity(i == 0 ? 70 : 1);
                details.add(detail);
            }
        }
        int id = createOrder(input, details, owners.get(ownerIndex));
        update("UPDATE dbo.ServiceOrder SET OrderStatus=?, AdminNote=?, CreatedAt='2026-10-30T09:00:00', "
                + "CompletedAt=CASE WHEN ?='COMPLETED' THEN CAST('2026-10-30T10:00:00' AS DATETIME2) ELSE NULL END "
                + "WHERE OrderID=? AND AccountID=?", status, "Workshop <b>ghi chú</b> & đã nhận thông tin", status, id, owners.get(ownerIndex));
        return id;
    }

    private void daoChecks() throws SQLException {
        List<Order> mine = getOrdersByAccount(owners.get(0));
        List<Order> theirs = getOrdersByAccount(owners.get(1));
        check(mine.size() == 5 && mine.stream().allMatch(o -> o.getAccountId() == owners.get(0))
                && theirs.size() == 1 && theirs.get(0).getOrderId() == otherPending, "DAO separates owners A/B");
        Order detailed = mine.stream().filter(o -> o.getOrderId() == pending).findFirst().orElseThrow();
        check(detailed.getDetails().size() == 2 && detailed.getTotalAmount().compareTo(new BigDecimal("850000")) == 0
                && detailed.getDetails().get(0).getUnitPrice().compareTo(new BigDecimal("5000")) == 0,
                "DAO loads detail snapshots despite inactive/changed catalog");
        check(mine.get(0).getOrderId() == concurrent && mine.get(4).getOrderId() == pending,
                "History order is CreatedAt DESC then OrderID DESC");
        check(getOrdersByAccount(owners.get(2)).isEmpty(), "Empty customer gets empty list");
        check(!cancelPending(otherPending, owners.get(0)) && "PENDING".equals(status(otherPending)), "DAO cannot cancel another owner");
        check(!cancelPending(processing, owners.get(0)), "DAO cannot cancel PROCESSING");
        check(!cancelPending(completed, owners.get(0)), "DAO cannot cancel COMPLETED");
        check(!cancelPending(cancelled, owners.get(0)), "DAO cannot cancel CANCELLED again");
        check(!cancelPending(Integer.MAX_VALUE, owners.get(0)), "DAO missing ID returns affected=false");
        update("UPDATE dbo.UserAccount SET IsActive=0 WHERE AccountID=?", owners.get(0));
        try {
            check(!cancelPending(pending, owners.get(0)), "DAO rechecks active customer before cancel");
        } finally {
            update("UPDATE dbo.UserAccount SET IsActive=1 WHERE AccountID=?", owners.get(0));
        }
    }

    private void httpChecks() throws Exception {
        a = browser(); b = browser();
        login(a, marker + "_a", "Fixture@123");
        login(b, marker + "_b", "Fixture@123");
        HttpClient empty = browser();
        login(empty, marker + "_empty", "Fixture@123");
        HttpClient admin = browser();
        login(admin, "admin", "Admin@123");
        check(get(browser(), "/orders").statusCode() == 302, "Anonymous history redirects to login");
        check(get(admin, "/orders").statusCode() == 403, "ADMIN cannot open customer history");
        HttpResponse<String> page = get(a, "/orders");
        String html = page.body();
        String csrfA = token(html);
        String csrfB = token(get(b, "/orders").body());
        check(page.statusCode() == 200 && occurrences(html, "class=\"auth-panel order-card\"") == 5
                && html.contains("A_PRIVATE_" + marker) && !html.contains("B_PRIVATE_" + marker),
                "Fresh login A sees all own history from DB, no B data");
        check(html.contains("850.000") && html.contains("350.000") && html.contains("300.000")
                && html.contains("30/10/2026 09:00") && html.contains("30/10/2026 10:00"),
                "History renders totals, subtotals and LocalDateTime formatting");
        check(html.contains("&lt;script&gt;") && html.contains("&lt;img") && html.contains("&lt;b&gt;")
                && html.contains("Part &lt;&amp;&gt;") && !html.contains("<script>alert(1)") && !html.contains("<img src=x"),
                "Snapshot/device/contact/notes escape HTML safely");
        check(html.contains("order-status-PENDING") && html.contains("order-status-PROCESSING")
                && html.contains("order-status-COMPLETED") && html.contains("order-status-CANCELLED")
                && html.contains("Không chọn thêm linh kiện"), "All four statuses and zero-parts orders render");
        check(hasCancel(html, pending) && hasCancel(html, concurrent) && !hasCancel(html, processing)
                && !hasCancel(html, completed) && !hasCancel(html, cancelled)
                && occurrences(html, "name=\"action\" value=\"cancel\"") == 2,
                "Only own PENDING cards have cancel forms");
        check(html.contains("aria-current=\"page\">Lịch sử yêu cầu")
                && occurrences(html, "<header") == 1 && occurrences(html, "<main") == 1
                && occurrences(html, "<footer") == 1 && html.contains("Assignment"), "Common shell and active customer menu");
        String emptyHtml = get(empty, "/orders").body();
        check(emptyHtml.contains("Bạn chưa có yêu cầu nào") && !emptyHtml.contains("class=\"auth-panel order-card\""), "Real empty state");
        check(!get(admin, "/profile").body().contains("aria-current=\"false\">Lịch sử yêu cầu"), "Customer history menu hidden for ADMIN");
        String query = "/orders?accountId=" + owners.get(1) + "&orderId=" + otherPending + "&action=cancel";
        check(!get(a, query).body().contains("B_PRIVATE_" + marker) && "PENDING".equals(status(otherPending)),
                "GET ignores forged owner and never cancels");
        check(!get(b, "/orders?accountId=" + owners.get(0)).body().contains("A_PRIVATE_" + marker),
                "B cannot read A even with accountId query");
        check(post(a, pairs("action", "cancel", "orderId", pending)).statusCode() == 403, "Cancel missing CSRF denied");
        check(post(a, cancelBody(pending, "fake")).statusCode() == 403, "Cancel bad CSRF denied");
        check(post(admin, cancelBody(pending, token(get(admin, "/profile").body()))).statusCode() == 403, "ADMIN cancel denied by Filter");
        check(post(b, cancelBody(pending, csrfB) + "&" + pairs("accountId", owners.get(0))).statusCode() == 409
                && "PENDING".equals(status(pending)), "B cannot cancel A by forged owner parameter");
        HttpResponse<String> conflict = post(a, cancelBody(otherPending, csrfA));
        check(conflict.statusCode() == 409 && !conflict.body().contains("B_PRIVATE_" + marker)
                && "PENDING".equals(status(otherPending)), "A cannot cancel B or see B data in error response");
        for (int id : List.of(processing, completed, cancelled)) {
            String before = status(id);
            check(post(a, cancelBody(id, csrfA)).statusCode() == 409 && status(id).equals(before), "HTTP rejects terminal/processing order " + before);
        }
        for (String raw : List.of("", "0", "-1", "abc", "1.5", "2147483648")) {
            check(post(a, pairs("action", "cancel", "orderId", raw, "csrf", csrfA)).statusCode() == 400, "Malformed order ID " + raw);
        }
        check(post(a, pairs("action", "cancel", "csrf", csrfA)).statusCode() == 400, "Missing order ID");
        check(post(a, cancelBody(pending, csrfA) + "&" + pairs("orderId", otherPending)).statusCode() == 400, "Duplicate order ID rejected");
        check(post(a, pairs("action", "review", "orderId", completed, "csrf", csrfA)).statusCode() == 400, "Unimplemented action rejected");
        check(post(a, cancelBody(Integer.MAX_VALUE, csrfA)).statusCode() == 409, "Missing order uses same conflict response");
        String unchanged = fingerprint(pending);
        HttpResponse<String> success = post(a, cancelBody(pending, csrfA));
        check(success.statusCode() == 302 && success.headers().firstValue("location").orElse("").endsWith("/orders")
                && "CANCELLED".equals(status(pending)), "Owner cancels PENDING with PRG");
        check(unchanged.equals(fingerprint(pending)), "Cancel changes only status, preserves every other field and detail");
        String after = get(a, "/orders").body();
        check(after.contains("Đã hủy yêu cầu #" + pending) && !hasCancel(after, pending)
                && card(after, pending).contains("order-status-CANCELLED"), "Redirect reloads current status and removes cancel button");
        check(!get(a, "/orders").body().contains("Đã hủy yêu cầu #" + pending), "Flash confirmation shown once, refresh read-only");
        check(post(a, cancelBody(pending, csrfA)).statusCode() == 409 && "CANCELLED".equals(status(pending))
                && unchanged.equals(fingerprint(pending)), "Repeated cancel does not update or reopen order");
        HttpRequest request = postRequest(cancelBody(concurrent, csrfA));
        CompletableFuture<HttpResponse<String>> one = a.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        CompletableFuture<HttpResponse<String>> two = a.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        int first = one.join().statusCode(); int second = two.join().statusCode();
        check(((first == 302 && second == 409) || (first == 409 && second == 302)) && "CANCELLED".equals(status(concurrent)),
                "Concurrent cancel: exactly one SQL update wins");
        String csrfLogout = token(get(a, "/orders").body());
        HttpRequest logout = HttpRequest.newBuilder(URI.create(baseUrl + "/logout"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(pairs("csrf", csrfLogout))).build();
        a.send(logout, HttpResponse.BodyHandlers.ofString());
        check(get(a, "/orders").statusCode() == 302, "Logged-out session cannot read history");
        login(a, marker + "_a", "Fixture@123");
        check(get(a, "/orders").body().contains("order-" + pending), "History persists after logout/login");
        update("UPDATE dbo.UserAccount SET IsActive=0 WHERE AccountID=?", owners.get(1));
        try {
            check(post(b, cancelBody(otherPending, csrfB)).statusCode() == 302 && "PENDING".equals(status(otherPending)),
                    "Locked customer cannot cancel with an existing session");
        } finally {
            update("UPDATE dbo.UserAccount SET IsActive=1 WHERE AccountID=?", owners.get(1));
        }
        check(get(a, "/WEB-INF/views/request-history.jsp").statusCode() == 404, "History JSP protected under WEB-INF");
    }

    private void dbFailure(Path config) throws Exception {
        Path absolute = config.toAbsolutePath().normalize();
        if (!absolute.getFileName().toString().equals("keebforce-db.properties")
                || !absolute.getParent().getParent().getFileName().toString().startsWith("keebforce-p304-")) {
            throw new IllegalArgumentException("DB outage test requires the private P304 Tomcat config");
        }
        String csrf = token(get(a, "/orders").body());
        byte[] saved = Files.readAllBytes(absolute);
        try {
            Files.writeString(absolute, "db.url=jdbc:sqlserver://127.0.0.1:1;databaseName=KeebForce_PRJ301;encrypt=false\n"
                    + "db.user=test-only\ndb.password=unavailable\n", StandardCharsets.UTF_8);
            HttpResponse<String> failed = get(a, "/orders");
            check(failed.statusCode() == 503 && !failed.body().contains("Bạn chưa có yêu cầu nào")
                    && !failed.body().contains("jdbc:sqlserver") && !failed.body().contains("SQLException"),
                    "DB failure gives 503, never fake empty history or connection details");
            check(post(a, cancelBody(otherPending, csrf)).statusCode() == 503 && "PENDING".equals(status(otherPending)),
                    "DB failure cannot change order status");
        } finally {
            Files.write(absolute, saved);
        }
        check(get(a, "/orders").statusCode() == 200, "History recovers after private config restoration");
    }

    private void preparePreview() throws SQLException {
        update("UPDATE dbo.WorkshopService SET IsActive=1, BasePrice=200000 WHERE ServiceID=?", service);
        update("UPDATE dbo.Product SET IsActive=1, Price=5000 WHERE ProductID=?", products.get(0));
        update("UPDATE dbo.Product SET IsActive=1, Price=300000 WHERE ProductID=?", products.get(1));
        fixtureOrder(0, "PENDING", true);
    }

    private String fingerprint(int id) throws SQLException {
        return queryFingerprint("SELECT AccountID,ServiceID,ServiceNameSnapshot,ServicePrice,ContactName,ContactPhone,"
                + "DeviceDescription,PreferredDate,CustomerNote,AdminNote,TotalAmount,CreatedAt,CompletedAt "
                + "FROM dbo.ServiceOrder WHERE OrderID=?", id)
                + queryFingerprint("SELECT OrderDetailID,OrderID,ProductID,ProductNameSnapshot,Quantity,UnitPrice "
                + "FROM dbo.OrderDetail WHERE OrderID=? ORDER BY OrderDetailID", id);
    }

    private String queryFingerprint(String sql, int id) throws SQLException {
        StringBuilder text = new StringBuilder();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) for (int i = 1; i <= rows.getMetaData().getColumnCount(); i++) text.append(rows.getObject(i)).append('|');
            }
        }
        return text.toString();
    }

    private String status(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT OrderStatus FROM dbo.ServiceOrder WHERE OrderID=?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new AssertionError("Fixture order missing");
                return result.getString(1);
            }
        }
    }

    private String counts() throws SQLException {
        StringBuilder result = new StringBuilder();
        for (String table : List.of("UserAccount", "Brand", "Category", "Product", "WorkshopService", "ServiceOrder", "OrderDetail", "ServiceReview")) {
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM dbo." + table)) {
                rows.next(); result.append(rows.getInt(1)).append('/');
            }
        }
        return result.toString();
    }

    private int insert(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, values); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Missing fixture ID");
                return keys.getInt(1);
            }
        }
    }

    private void update(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values); statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof String) statement.setNString(i + 1, (String) values[i]);
            else statement.setObject(i + 1, values[i]);
        }
    }

    private void cleanup() throws SQLException {
        for (int owner : owners) {
            update("DELETE d FROM dbo.OrderDetail d JOIN dbo.ServiceOrder o ON o.OrderID=d.OrderID WHERE o.AccountID=?", owner);
            update("DELETE FROM dbo.ServiceOrder WHERE AccountID=?", owner);
            update("DELETE FROM dbo.UserAccount WHERE AccountID=? AND Username LIKE ?", owner, marker + "_%");
        }
        for (int product : products) update("DELETE FROM dbo.Product WHERE ProductID=? AND ProductName LIKE ?", product, marker + "%");
        if (service > 0) update("DELETE FROM dbo.WorkshopService WHERE ServiceID=? AND ServiceName=?", service, marker + " Service <script>");
    }

    private HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest postRequest(String body) {
        return HttpRequest.newBuilder(URI.create(baseUrl + "/orders")).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private HttpResponse<String> post(HttpClient client, String body) throws Exception {
        return client.send(postRequest(body), HttpResponse.BodyHandlers.ofString());
    }

    private void login(HttpClient client, String username, String password) throws Exception {
        String csrf = token(get(client, "/login").body());
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(pairs("csrf", csrf, "username", username, "password", password))).build();
        if (client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() != 302) throw new AssertionError("Login failed");
    }

    private String token(String html) {
        Matcher match = Pattern.compile("name=\"csrf\"\\s+value=\"([^\"]+)\"").matcher(html);
        if (!match.find()) throw new AssertionError("CSRF missing");
        return match.group(1);
    }

    private String cancelBody(int id, String csrf) { return pairs("action", "cancel", "orderId", id, "csrf", csrf); }

    private String pairs(Object... values) {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < values.length; i += 2) entries.add(URLEncoder.encode(values[i].toString(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(values[i + 1].toString(), StandardCharsets.UTF_8));
        return String.join("&", entries);
    }

    private String card(String html, int id) {
        Matcher match = Pattern.compile("<article\\b[^>]*\\bid=\"order-" + id + "\"").matcher(html);
        if (!match.find()) return "";
        return html.substring(match.start(), html.indexOf("</article>", match.end()));
    }

    private boolean hasCancel(String html, int id) { return card(html, id).contains("name=\"action\" value=\"cancel\""); }

    private int occurrences(String text, String needle) {
        int total = 0; int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) { total++; from += needle.length(); }
        return total;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("OK " + (++checks) + " " + message);
    }
}
