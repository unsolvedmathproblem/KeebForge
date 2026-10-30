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
import java.sql.Timestamp;
import java.time.Duration;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.Account;
import model.Order;
import model.OrderDetail;
import model.Product;
import util.PasswordUtil;

/** JDBC/HTTP thật; mọi account/catalog/order fixture có ID riêng và được dọn trong finally. */
public class VerifyP305 extends OrderDAO {
    private static int checks;
    private final String marker = "P305_" + UUID.randomUUID().toString().replace("-", "");
    private final List<Integer> accounts = new ArrayList<>();
    private final List<Integer> parts = new ArrayList<>();
    private final List<Integer> orders = new ArrayList<>();
    private String base;
    private BigDecimal initialCompletedValue;
    private int service;
    private int pending, processing, completed, cancelled, race, cancelRace, completeRace, rejectPending;
    private HttpClient admin, customer;

    public VerifyP305() throws SQLException { super(); }

    public static void main(String[] args) throws Exception {
        try (VerifyP305 test = new VerifyP305()) {
            test.base = args.length == 0 ? "http://127.0.0.1:18080/KeebForce" : args[0];
            String before = test.counts();
            Map<String, Object> initial = test.getStatistics();
            test.initialCompletedValue = (BigDecimal) initial.get("completedValue");
            try {
                test.actors();
                test.admin = test.browser(); test.customer = test.browser();
                test.login(test.admin, test.marker + "_admin"); test.login(test.customer, test.marker + "_a");
                if ((Long) initial.get("totalOrders") == 0) {
                    check(initial.get("PENDING").equals(0L) && initial.get("COMPLETED").equals(0L)
                            && ((BigDecimal) initial.get("completedValue")).signum() == 0, "Empty DB statistics are actual zeros");
                    String html = test.get(test.admin, "/admin/dashboard").body();
                    check(test.occurrences(html, "class=\"stat-value\">0</p>") == 5, "Empty dashboard renders zero counts");
                    check(test.get(test.admin, "/admin/orders").body().contains("Không có yêu cầu nào phù hợp"), "Actual empty admin list");
                }
                test.fixtures();
                test.daoChecks();
                test.httpChecks();
                if (args.length > 1 && !args[1].equals("-")) test.outage(Path.of(args[1]));
                if (List.of(args).contains("--preview")) {
                    int preview = test.create(0, true, "PENDING");
                    System.out.println("PREVIEW_USERNAME=" + test.marker + "_admin");
                    System.out.println("PREVIEW_ORDER=" + preview);
                    System.out.println("PREVIEW_COMPLETED_VALUE=" + test.getStatistics().get("completedValue"));
                    System.out.println("PREVIEW_READY (Enter to clean up fixtures)");
                    System.in.read();
                }
            } finally { test.cleanup(); }
            check(before.equals(test.counts()) && initial.equals(test.getStatistics()), "Fixture cleanup restores all 8 table counts and statistics");
        }
        System.out.println("PASS " + checks + " JDBC/HTTP checks.");
    }

    private void actors() throws Exception {
        String hash = PasswordUtil.hashPassword("Fixture@123");
        try (AccountDAO dao = new AccountDAO()) {
            for (String suffix : List.of("a", "b", "admin")) {
                Account account = new Account();
                account.setUsername(marker + "_" + suffix); account.setEmail(account.getUsername() + "@example.invalid");
                account.setFullName(marker + " " + suffix); account.setPhone("0901234567"); account.setPasswordHash(hash);
                if (!dao.register(account)) throw new AssertionError("Fixture account registration failed");
                accounts.add(account.getAccountId());
            }
        }
        update("UPDATE dbo.UserAccount SET Role='ADMIN' WHERE AccountID=?", accounts.get(2));
    }

    private void fixtures() throws SQLException {
        service = insert("INSERT INTO dbo.WorkshopService (ServiceName,Description,BasePrice) VALUES (?, ?, 200000)", marker + " Service <&>", "Dịch vụ thử");
        Product seed;
        try (ProductDAO dao = new ProductDAO()) { seed = dao.getActiveProducts().get(0); }
        for (String price : List.of("1200000", "5000", "300000")) {
            parts.add(insert("INSERT INTO dbo.Product (ProductName,CategoryID,BrandID,Price) VALUES (?,?,?,?)",
                    marker + " Part <script> " + parts.size(), seed.getCategoryId(), seed.getBrandId(), new BigDecimal(price)));
        }
        pending = create(0, true, "PENDING"); processing = create(0, false, "PROCESSING");
        completed = create(1, false, "COMPLETED"); cancelled = create(1, false, "CANCELLED");
        race = create(0, false, "PENDING"); cancelRace = create(0, false, "PENDING");
        completeRace = create(1, false, "PROCESSING"); rejectPending = create(1, false, "PENDING");
    }

    private int create(int ownerIndex, boolean withParts, String status) throws SQLException {
        Order input = new Order(); input.setServiceId(service); input.setContactName(marker + " contact " + ownerIndex);
        input.setContactPhone("0901234567"); input.setDeviceDescription("Bàn phím <script>alert(1)</script>");
        input.setCustomerNote("Ghi chú <b>khách</b>");
        List<OrderDetail> details = new ArrayList<>();
        if (withParts) for (int i = 0; i < parts.size(); i++) {
            OrderDetail detail = new OrderDetail(); detail.setProductId(parts.get(i)); detail.setQuantity(i == 1 ? 70 : 1); details.add(detail);
        }
        int id = createOrder(input, details, accounts.get(ownerIndex)); orders.add(id);
        update("UPDATE dbo.ServiceOrder SET OrderStatus=?, CompletedAt=CASE WHEN ?='COMPLETED' THEN "
                + "CAST('2026-10-30T10:00:00' AS DATETIME2) ELSE NULL END WHERE OrderID=?", status, status, id);
        return id;
    }

    private void daoChecks() throws SQLException {
        check(!updateStatus(pending, "COMPLETED") && status(pending).equals("PENDING"), "DAO blocks PENDING to COMPLETED jump");
        check(!updateStatus(completed, "PROCESSING") && !updateStatus(cancelled, "PROCESSING"), "DAO cannot reopen terminal orders");
        check(!updateStatus(processing, "CANCELLED"), "DAO cannot cancel a processing order");
        check(!updateStatus(Integer.MAX_VALUE, "PROCESSING") && !updateAdminNote(Integer.MAX_VALUE, "x"), "DAO reports affected=false for missing order");
        try { updateStatus(pending, "PENDING"); throw new AssertionError("Invalid target accepted"); }
        catch (IllegalArgumentException expected) { check(true, "DAO rejects unsupported target status"); }
        try { updateAdminNote(pending, "x".repeat(1001)); throw new AssertionError("Long note accepted"); }
        catch (IllegalArgumentException expected) { check(note(pending) == null, "DAO enforces note length"); }
        check(updateAdminNote(pending, "x".repeat(1000)) && note(pending).length() == 1000, "DAO accepts 1000-character note");
        check(updateAdminNote(pending, "   ") && note(pending) == null, "Blank note stored as NULL");
        assertStatistics("Initial grouped statistics match independent SQL");
        for (String filter : List.of("PENDING", "PROCESSING", "COMPLETED", "CANCELLED")) {
            check(getAllOrders(filter).stream().allMatch(o -> filter.equals(o.getOrderStatus())), "DAO filter " + filter);
        }
    }

    private void httpChecks() throws Exception {
        for (String path : List.of("/admin/orders", "/admin/dashboard")) {
            check(get(browser(), path).statusCode() == 302, "Anonymous blocked " + path);
            check(get(customer, path).statusCode() == 403, "Customer blocked " + path);
        }
        String page = get(admin, "/admin/orders").body(); String csrf = token(page);
        check(orders.stream().allMatch(id -> !card(page, id).isEmpty()) && page.contains("#" + accounts.get(1)), "Admin sees all owners and details");
        check(page.contains("2.050.000") && page.contains("&lt;script&gt;") && !page.contains("<script>alert(1)"), "Admin shows exact snapshots and escaped device/parts");
        check(card(page, pending).contains("value=\"PROCESSING\"") && card(page, pending).contains("value=\"CANCELLED\"")
                && !card(page, pending).contains("value=\"COMPLETED\"") && !card(page, completed).contains("name=\"newStatus\""), "UI exposes only allowed transitions");
        for (String filter : List.of("PENDING", "PROCESSING", "COMPLETED", "CANCELLED")) {
            HttpResponse<String> filtered = get(admin, "/admin/orders?status=" + filter);
            boolean correct = filtered.statusCode() == 200;
            for (int id : orders) correct &= card(filtered.body(), id).isEmpty() != status(id).equals(filter);
            check(correct, "HTTP status filter " + filter);
        }
        for (String query : List.of("?status=INVALID", "?status=PENDING&status=COMPLETED", "?status=PENDING%27%20OR%201%3D1")) {
            check(get(admin, "/admin/orders" + query).statusCode() == 400, "Malformed filter rejected " + query);
        }
        check(post(customer, "/admin/orders", statusBody(pending, "PROCESSING", token(get(customer, "/orders").body()))).statusCode() == 403
                && status(pending).equals("PENDING"), "Customer cannot forge Admin POST");
        check(post(admin, "/admin/orders", pairs("action", "status", "orderId", pending, "newStatus", "PROCESSING")).statusCode() == 403, "Missing CSRF denied");
        check(post(admin, "/admin/orders", statusBody(pending, "PROCESSING", "fake")).statusCode() == 403, "Bad CSRF denied");
        check(post(admin, "/admin/dashboard", pairs("csrf", csrf)).statusCode() == 405, "Dashboard GET only");
        for (String raw : List.of("", "0", "-1", "abc", "2147483648")) {
            check(post(admin, "/admin/orders", pairs("action", "status", "orderId", raw, "newStatus", "PROCESSING", "csrf", csrf)).statusCode() == 400, "Invalid order ID " + raw);
        }
        check(post(admin, "/admin/orders", statusBody(pending, "PROCESSING", csrf) + "&" + pairs("orderId", completed)).statusCode() == 400, "Duplicate ID rejected");
        check(post(admin, "/admin/orders", pairs("action", "delete", "orderId", pending, "csrf", csrf)).statusCode() == 400, "Unknown action rejected");
        check(post(admin, "/admin/orders", statusBody(pending, "PENDING", csrf)).statusCode() == 400, "Invalid target rejected by HTTP");
        check(post(admin, "/admin/orders", statusBody(pending, "COMPLETED", csrf)).statusCode() == 409 && status(pending).equals("PENDING"), "HTTP blocks status jump");
        check(post(admin, "/admin/orders", statusBody(completed, "PROCESSING", csrf)).statusCode() == 409
                && post(admin, "/admin/orders", statusBody(cancelled, "PROCESSING", csrf)).statusCode() == 409, "HTTP cannot reopen completed/cancelled");
        check(post(admin, "/admin/orders", statusBody(Integer.MAX_VALUE, "PROCESSING", csrf)).statusCode() == 409, "Missing order conflict");
        String fingerprint = unchanged(pending);
        String htmlNote = "Đã nhận <script>alert(2)</script> & đang kiểm tra";
        check(post(admin, "/admin/orders", noteBody(pending, htmlNote, csrf) + "&" + pairs("totalAmount", 1, "accountId", accounts.get(2), "deviceDescription", "FAKE")).statusCode() == 302
                && unchanged(pending).equals(fingerprint) && status(pending).equals("PENDING") && completedAt(pending) == null, "Note updates only AdminNote, ignores other fields");
        String customerPage = get(customer, "/orders").body();
        check(customerPage.contains("&lt;script&gt;alert(2)&lt;/script&gt;") && !customerPage.contains("<script>alert(2)"), "Customer history reads new AdminNote with HTML escape");
        check(post(admin, "/admin/orders", noteBody(pending, "Nội dung mới nhất", csrf)).statusCode() == 302
                && !get(customer, "/orders").body().contains("alert(2)"), "Note overwrites current text, no timeline");
        HttpResponse<String> tooLong = post(admin, "/admin/orders", noteBody(pending, "x".repeat(1001), csrf));
        check(tooLong.statusCode() == 400 && tooLong.body().contains("x".repeat(1001)) && note(pending).equals("Nội dung mới nhất"), "Long note rejected, draft retained, DB unchanged");
        check(post(admin, "/admin/orders", noteBody(pending, " ", csrf)).statusCode() == 302 && note(pending) == null, "Admin can clear note");
        check(post(admin, "/admin/orders", noteBody(pending, "first", csrf) + "&" + pairs("adminNote", "second")).statusCode() == 400, "Duplicate note rejected");
        check(post(admin, "/admin/orders", statusBody(pending, "PROCESSING", csrf) + "&" + pairs("status", "INVALID")).statusCode() == 400
                && status(pending).equals("PENDING"), "Bad return filter cannot mutate order");
        HttpResponse<String> accepted = post(admin, "/admin/orders", statusBody(pending, "PROCESSING", csrf) + "&" + pairs("status", "PENDING"));
        check(accepted.statusCode() == 302 && accepted.headers().firstValue("location").orElse("").contains("?status=PENDING#order-")
                && status(pending).equals("PROCESSING") && completedAt(pending) == null, "Accept PENDING with PRG and filter preserved");
        String flash = "Đã cập nhật trạng thái yêu cầu #" + pending;
        check(get(admin, "/admin/orders").body().contains(flash) && !get(admin, "/admin/orders").body().contains(flash), "Admin flash shown once");
        Timestamp before = sqlTime();
        check(post(admin, "/admin/orders", statusBody(pending, "COMPLETED", csrf)).statusCode() == 302, "PROCESSING to COMPLETED succeeds");
        Timestamp finished = completedAt(pending), after = sqlTime();
        check(finished != null && !finished.before(before) && !finished.after(after) && unchanged(pending).equals(fingerprint), "CompletedAt set by DB, snapshots/details untouched");
        check(post(admin, "/admin/orders", statusBody(pending, "COMPLETED", csrf)).statusCode() == 409 && finished.equals(completedAt(pending)), "Repeat completion does not reset CompletedAt");
        check(post(admin, "/admin/orders", statusBody(processing, "CANCELLED", csrf)).statusCode() == 409, "Cannot cancel PROCESSING through Admin");
        check(post(admin, "/admin/orders", statusBody(processing, "COMPLETED", csrf)).statusCode() == 302, "Existing PROCESSING completes");
        check(post(admin, "/admin/orders", statusBody(rejectPending, "CANCELLED", csrf)).statusCode() == 302
                && completedAt(rejectPending) == null, "Admin rejects PENDING without CompletedAt");
        check(post(admin, "/admin/orders", noteBody(cancelled, "Đã liên hệ khách", csrf)).statusCode() == 302 && status(cancelled).equals("CANCELLED"), "Terminal order note editable without reopening");
        HttpRequest same = request("/admin/orders", statusBody(race, "PROCESSING", csrf));
        int[] results = pair(admin.sendAsync(same, HttpResponse.BodyHandlers.ofString()), admin.sendAsync(same, HttpResponse.BodyHandlers.ofString()));
        check(oneWinner(results) && status(race).equals("PROCESSING"), "Simultaneous acceptance has one winner");
        String customerCsrf = token(get(customer, "/orders").body());
        results = pair(admin.sendAsync(request("/admin/orders", statusBody(cancelRace, "PROCESSING", csrf)), HttpResponse.BodyHandlers.ofString()),
                customer.sendAsync(request("/orders", pairs("action", "cancel", "orderId", cancelRace, "csrf", customerCsrf)), HttpResponse.BodyHandlers.ofString()));
        check(oneWinner(results) && List.of("PROCESSING", "CANCELLED").contains(status(cancelRace)), "Admin acceptance races customer cancellation safely");
        same = request("/admin/orders", statusBody(completeRace, "COMPLETED", csrf));
        results = pair(admin.sendAsync(same, HttpResponse.BodyHandlers.ofString()), admin.sendAsync(same, HttpResponse.BodyHandlers.ofString()));
        check(oneWinner(results) && completedAt(completeRace) != null, "Concurrent completion has one winner");
        assertStatistics("Completed count and sum match DB after transitions/races");
        String dashboard = get(admin, "/admin/dashboard").body();
        BigDecimal expectedValue = initialCompletedValue.add(new BigDecimal("2650000"));
        check(((BigDecimal) getStatistics().get("completedValue")).compareTo(expectedValue) == 0
                && dashboard.contains("Tổng giá trị đơn hoàn tất")
                && dashboard.contains(NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(expectedValue))
                && !dashboard.contains("doanh thu"), "Dashboard exact completed value includes fixture delta 2650000 and required label");
        for (String file : List.of("orders", "dashboard")) check(get(admin, "/WEB-INF/views/admin/" + file + ".jsp").statusCode() == 404, "Protected Admin JSP " + file);
        update("UPDATE dbo.UserAccount SET IsActive=0 WHERE AccountID=?", accounts.get(2));
        try { check(get(admin, "/admin/dashboard").statusCode() == 302
                && post(admin, "/admin/orders", noteBody(pending, "FAKE", csrf)).statusCode() == 302 && note(pending) == null, "Locked Admin cannot read/write using old session"); }
        finally { update("UPDATE dbo.UserAccount SET IsActive=1 WHERE AccountID=?", accounts.get(2)); }
        login(admin, marker + "_admin");
    }

    private void outage(Path config) throws Exception {
        Path path = config.toAbsolutePath().normalize();
        if (!path.getFileName().toString().equals("keebforce-db.properties") || !path.getParent().getParent().getFileName().toString().startsWith("keebforce-p305-")) throw new IllegalArgumentException("Private P305 config required");
        byte[] saved = Files.readAllBytes(path); String csrf = token(get(admin, "/admin/orders").body());
        try {
            Files.writeString(path, "db.url=jdbc:sqlserver://127.0.0.1:1;databaseName=KeebForce_PRJ301;encrypt=false\ndb.user=test-only\ndb.password=unavailable\n");
            for (String route : List.of("/admin/orders", "/admin/dashboard")) {
                HttpResponse<String> failed = get(admin, route);
                check(failed.statusCode() == 503 && !failed.body().contains("class=\"stat-value\"") && !failed.body().contains("Không có yêu cầu nào phù hợp")
                        && !failed.body().contains("jdbc:sqlserver") && !failed.body().contains("SQLException"), "DB outage gives 503 without fake data " + route);
            }
            check(post(admin, "/admin/orders", noteBody(pending, "FAKE", csrf)).statusCode() == 503 && note(pending) == null, "DB outage prevents mutation");
        } finally { Files.write(path, saved); }
        check(get(admin, "/admin/orders").statusCode() == 200 && get(admin, "/admin/dashboard").statusCode() == 200, "Admin pages recover after config restoration");
    }

    private void assertStatistics(String label) throws SQLException {
        Map<String, Object> actual = getStatistics();
        boolean correct = actual.get("totalOrders").equals(scalarCount("SELECT COUNT_BIG(*) FROM dbo.ServiceOrder"));
        for (String status : List.of("PENDING", "PROCESSING", "COMPLETED", "CANCELLED")) correct &= actual.get(status).equals(scalarCount("SELECT COUNT_BIG(*) FROM dbo.ServiceOrder WHERE OrderStatus='" + status + "'"));
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT COALESCE(SUM(TotalAmount),0) FROM dbo.ServiceOrder WHERE OrderStatus='COMPLETED'")) {
            result.next(); correct &= ((BigDecimal) actual.get("completedValue")).compareTo(result.getBigDecimal(1)) == 0;
        }
        check(correct, label);
    }

    private long scalarCount(String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) { result.next(); return result.getLong(1); }
    }

    private Timestamp sqlTime() throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT SYSDATETIME()")) { result.next(); return result.getTimestamp(1); }
    }

    private String status(int id) throws SQLException { return (String) field(id, "OrderStatus"); }
    private String note(int id) throws SQLException { return (String) field(id, "AdminNote"); }
    private Timestamp completedAt(int id) throws SQLException { return (Timestamp) field(id, "CompletedAt"); }
    private Object field(int id, String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT " + column + " FROM dbo.ServiceOrder WHERE OrderID=?")) {
            statement.setInt(1, id); try (ResultSet result = statement.executeQuery()) { if (!result.next()) throw new AssertionError("Fixture order missing"); return result.getObject(1); }
        }
    }

    private String unchanged(int id) throws SQLException {
        StringBuilder text = new StringBuilder();
        for (String sql : List.of("SELECT AccountID,ServiceID,ServiceNameSnapshot,ServicePrice,ContactName,ContactPhone,DeviceDescription,PreferredDate,CustomerNote,TotalAmount,CreatedAt FROM dbo.ServiceOrder WHERE OrderID=?",
                "SELECT OrderDetailID,OrderID,ProductID,ProductNameSnapshot,Quantity,UnitPrice FROM dbo.OrderDetail WHERE OrderID=? ORDER BY OrderDetailID")) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.setInt(1, id); try (ResultSet result = statement.executeQuery()) {
                while (result.next()) for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) text.append(result.getObject(i)).append('|');
            } }
        }
        return text.toString();
    }

    private String counts() throws SQLException {
        StringBuilder text = new StringBuilder();
        for (String table : List.of("UserAccount", "Brand", "Category", "Product", "WorkshopService", "ServiceOrder", "OrderDetail", "ServiceReview")) text.append(scalarCount("SELECT COUNT_BIG(*) FROM dbo." + table)).append('/');
        return text.toString();
    }
    private void bind(PreparedStatement statement, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) { if (values[i] instanceof String) statement.setNString(i + 1, (String) values[i]); else statement.setObject(i + 1, values[i]); }
    }
    private int insert(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { bind(statement, values); statement.executeUpdate(); try (ResultSet keys = statement.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Fixture key missing"); return keys.getInt(1); } }
    }
    private void update(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) { bind(statement, values); statement.executeUpdate(); }
    }
    private void cleanup() throws SQLException {
        for (int account : accounts) {
            update("DELETE d FROM dbo.OrderDetail d JOIN dbo.ServiceOrder o ON o.OrderID=d.OrderID WHERE o.AccountID=?", account);
            update("DELETE FROM dbo.ServiceOrder WHERE AccountID=?", account);
            update("DELETE FROM dbo.UserAccount WHERE AccountID=? AND Username LIKE ?", account, marker + "_%");
        }
        for (int id : parts) update("DELETE FROM dbo.Product WHERE ProductID=? AND ProductName LIKE ?", id, marker + "%");
        if (service > 0) update("DELETE FROM dbo.WorkshopService WHERE ServiceID=? AND ServiceName=?", service, marker + " Service <&>");
    }
    private HttpClient browser() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(10)).build(); }
    private HttpResponse<String> get(HttpClient client, String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpRequest request(String path, String body) { return HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(); }
    private HttpResponse<String> post(HttpClient client, String path, String body) throws Exception { return client.send(request(path, body), HttpResponse.BodyHandlers.ofString()); }
    private void login(HttpClient client, String username) throws Exception {
        String csrf = token(get(client, "/login").body());
        if (post(client, "/login", pairs("csrf", csrf, "username", username, "password", "Fixture@123")).statusCode() != 302) throw new AssertionError("Login failed");
    }
    private String token(String html) { Matcher match = Pattern.compile("name=\"csrf\"\\s+value=\"([^\"]+)\"").matcher(html); if (!match.find()) throw new AssertionError("CSRF missing"); return match.group(1); }
    private String statusBody(int id, String target, String csrf) { return pairs("action", "status", "orderId", id, "newStatus", target, "csrf", csrf); }
    private String noteBody(int id, String note, String csrf) { return pairs("action", "note", "orderId", id, "adminNote", note, "csrf", csrf); }
    private String pairs(Object... values) {
        List<String> result = new ArrayList<>(); for (int i = 0; i < values.length; i += 2) result.add(URLEncoder.encode(values[i].toString(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(values[i + 1].toString(), StandardCharsets.UTF_8)); return String.join("&", result);
    }
    private String card(String html, int id) { Matcher match = Pattern.compile("<article\\b[^>]*\\bid=\"order-" + id + "\"").matcher(html); if (!match.find()) return ""; return html.substring(match.start(), html.indexOf("</article>", match.end())); }
    private int occurrences(String text, String needle) { int total = 0, from = 0; while ((from = text.indexOf(needle, from)) >= 0) { total++; from += needle.length(); } return total; }
    private int[] pair(CompletableFuture<HttpResponse<String>> one, CompletableFuture<HttpResponse<String>> two) { return new int[]{one.join().statusCode(), two.join().statusCode()}; }
    private boolean oneWinner(int[] codes) { return (codes[0] == 302 && codes[1] == 409) || (codes[0] == 409 && codes[1] == 302); }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); System.out.println("OK " + (++checks) + " " + label); }
}
