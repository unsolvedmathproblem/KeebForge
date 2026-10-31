import dao.AccountDAO;
import dao.OrderDAO;
import dao.ProductDAO;
import dao.ServiceDAO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Duration;
import model.Account;
import model.Order;
import model.OrderDetail;
import model.Product;
import model.ServiceReview;
import util.PasswordUtil;

/** Mọi review hợp lệ được tạo qua DAO/POST thật; SQL chỉ chuẩn bị/dọn fixture ID riêng. */
public class VerifyP306 extends OrderDAO {
    private static int checks;
    private final String marker = "P306_" + UUID.randomUUID().toString().replace("-", "");
    private final String publicName = "<b>" + marker + "_NAME_" + "N".repeat(30) + "</b>";
    private final List<Integer> accounts = new ArrayList<>(), services = new ArrayList<>(), parts = new ArrayList<>();
    private int pending, processing, cancelled, main, foreign, race, empty, boundary, otherService, locked, distinctOne, distinctTwo;
    private String base;
    private HttpClient a, b, admin;

    public VerifyP306() throws SQLException { super(); }
    public static void main(String[] args) throws Exception {
        try (VerifyP306 test = new VerifyP306()) {
            test.base = args.length == 0 ? "http://127.0.0.1:18080/KeebForce" : args[0];
            String before = test.counts();
            try {
                test.setup(); test.daoChecks(); test.httpChecks();
                if (args.length > 1 && !args[1].equals("-")) test.outage(Path.of(args[1]));
                if (List.of(args).contains("--preview")) {
                    int preview = test.order(0, 0, false, "COMPLETED");
                    System.out.println("PREVIEW_USERNAME=" + test.marker + "_a");
                    System.out.println("PREVIEW_ORDER=" + preview);
                    System.out.println("PREVIEW_SERVICE=" + test.services.get(0));
                    System.out.println("PREVIEW_READY (Enter to clean up fixtures)");
                    System.in.read();
                }
            } finally { test.cleanup(); }
            check(before.equals(test.counts()), "Fixture cleanup restores all 8 table counts including reviews");
        }
        System.out.println("PASS " + checks + " JDBC/HTTP checks.");
    }

    private void setup() throws Exception {
        String hash = PasswordUtil.hashPassword("Fixture@123");
        try (AccountDAO dao = new AccountDAO()) {
            for (String suffix : List.of("a", "b", "admin")) {
                Account account = new Account(); account.setUsername(marker + "_" + suffix);
                account.setEmail(account.getUsername() + "@example.invalid"); account.setPhone("0901234567");
                account.setFullName(suffix.equals("a") ? publicName : marker + " " + suffix); account.setPasswordHash(hash);
                if (!dao.register(account)) throw new AssertionError("Fixture account failed");
                accounts.add(account.getAccountId());
            }
        }
        update("UPDATE dbo.UserAccount SET Role='ADMIN' WHERE AccountID=?", accounts.get(2));
        for (int i = 0; i < 2; i++) services.add(insert("INSERT INTO dbo.WorkshopService (ServiceName,Description,BasePrice) VALUES (?,?,?)",
                "Dịch vụ thử " + marker + " " + i, "Dịch vụ dùng kiểm tra ghép review", new BigDecimal(i == 0 ? "200000" : "150000")));
        Product seed;
        try (ProductDAO dao = new ProductDAO()) { seed = dao.getActiveProducts().get(0); }
        for (String price : List.of("1200000", "5000", "300000")) parts.add(insert("INSERT INTO dbo.Product (ProductName,CategoryID,BrandID,Price) VALUES (?,?,?,?)",
                marker + " part " + parts.size(), seed.getCategoryId(), seed.getBrandId(), new BigDecimal(price)));
        pending = order(0, 0, false, "PENDING"); processing = order(0, 0, false, "PROCESSING"); cancelled = order(0, 0, false, "CANCELLED");
        main = order(0, 0, true, "COMPLETED"); foreign = order(1, 0, false, "COMPLETED"); race = order(0, 0, false, "COMPLETED");
        empty = order(0, 0, false, "COMPLETED"); boundary = order(0, 0, false, "COMPLETED"); otherService = order(0, 1, false, "COMPLETED");
        locked = order(0, 0, false, "COMPLETED"); distinctOne = order(0, 0, false, "COMPLETED"); distinctTwo = order(0, 0, false, "COMPLETED");
    }

    private int order(int owner, int service, boolean withParts, String status) throws SQLException {
        Order input = new Order(); input.setServiceId(services.get(service)); input.setContactName(marker + " contact " + owner);
        input.setContactPhone("0901234567"); input.setDeviceDescription("Bàn phím khách mang theo");
        List<OrderDetail> details = new ArrayList<>();
        if (withParts) for (int i = 0; i < parts.size(); i++) { OrderDetail detail = new OrderDetail(); detail.setProductId(parts.get(i)); detail.setQuantity(i == 1 ? 70 : 1); details.add(detail); }
        int id = createOrder(input, details, accounts.get(owner));
        if (!status.equals("PENDING")) {
            if (status.equals("CANCELLED")) updateStatus(id, "CANCELLED");
            else { updateStatus(id, "PROCESSING"); if (status.equals("COMPLETED")) updateStatus(id, "COMPLETED"); }
        }
        return id;
    }

    private void daoChecks() throws Exception {
        check(!insertReview(pending, accounts.get(0), 5, "x"), "DAO rejects PENDING");
        check(!insertReview(processing, accounts.get(0), 5, "x"), "DAO rejects PROCESSING");
        check(!insertReview(cancelled, accounts.get(0), 5, "x"), "DAO rejects CANCELLED");
        check(!insertReview(foreign, accounts.get(0), 5, "x"), "DAO rejects wrong owner");
        check(!insertReview(Integer.MAX_VALUE, accounts.get(0), 5, "x"), "DAO rejects missing order");
        for (int rating : List.of(0, -1, 6)) {
            try { insertReview(main, accounts.get(0), rating, "x"); throw new AssertionError("Invalid rating accepted"); }
            catch (IllegalArgumentException expected) { check(reviewCount(main) == 0, "DAO rating boundary " + rating); }
        }
        try { insertReview(main, accounts.get(0), 5, "x".repeat(501)); throw new AssertionError("Long comment accepted"); }
        catch (IllegalArgumentException expected) { check(reviewCount(main) == 0, "DAO comment max 500"); }
        update("UPDATE dbo.UserAccount SET IsActive=0 WHERE AccountID=?", accounts.get(0));
        try { check(!insertReview(locked, accounts.get(0), 5, "x"), "DAO rechecks locked account"); }
        finally { update("UPDATE dbo.UserAccount SET IsActive=1 WHERE AccountID=?", accounts.get(0)); }
        check(insertReview(empty, accounts.get(0), 1, "  \n ") && reviewField(empty, "Comment") == null, "One star and empty optional comment stored as NULL");
        check(insertReview(boundary, accounts.get(0), 5, "x".repeat(500)), "Five stars and 500-character comment accepted");
        check(!insertReview(boundary, accounts.get(0), 1, "CHANGED") && (int) reviewField(boundary, "Rating") == 5, "DAO replay cannot overwrite review");
        try { insert("INSERT INTO dbo.ServiceReview (OrderID,Rating) VALUES (?,1)", boundary); throw new AssertionError("UNIQUE missing"); }
        catch (SQLException expected) { check((expected.getErrorCode() == 2627 || expected.getErrorCode() == 2601) && reviewCount(boundary) == 1, "DB UNIQUE(OrderID) still enforces one review"); }
        check(insertReview(otherService, accounts.get(0), 2, "OTHER_SERVICE_ONLY"), "Same customer can review another completed service order");
        try (ServiceDAO dao = new ServiceDAO()) {
            List<ServiceReview> list = dao.getReviews(services.get(0));
            check(list.size() == 2 && list.stream().allMatch(r -> r.getFullName().equals(publicName))
                    && list.stream().noneMatch(r -> "OTHER_SERVICE_ONLY".equals(r.getComment())), "P2 ServiceDAO uses shared model/fullName and service filter");
        }
        String before = fingerprint(main);
        update("UPDATE dbo.WorkshopService SET BasePrice=350000 WHERE ServiceID=?", services.get(0));
        update("UPDATE dbo.Product SET Price=9000 WHERE ProductID=?", parts.get(1));
        check(before.equals(fingerprint(main)), "Current catalog prices do not change order snapshots");
    }

    private void httpChecks() throws Exception {
        a = browser(); b = browser(); admin = browser(); login(a, marker + "_a"); login(b, marker + "_b"); login(admin, marker + "_admin");
        HttpResponse<String> history = get(a, "/orders"); String csrf = token(history.body()), csrfB = token(get(b, "/orders").body());
        check(history.statusCode() == 200 && hasReview(history.body(), main) && hasReview(history.body(), race)
                && !hasReview(history.body(), pending) && !hasReview(history.body(), processing) && !hasReview(history.body(), cancelled)
                && !hasReview(history.body(), boundary), "UI form only for own completed unreviewed orders");
        check(card(history.body(), boundary).contains("(5/5)") && card(history.body(), empty).contains("Bạn không để lại bình luận"), "Existing rating/comment shown without edit form");
        check(post(browser(), reviewBody(main, "5", "x", csrf)).statusCode() == 302, "Anonymous review redirects to login");
        check(post(admin, reviewBody(main, "5", "x", token(get(admin, "/profile").body()))).statusCode() == 403, "ADMIN review forbidden");
        check(post(a, pairs("action", "review", "orderId", main, "rating", 5)).statusCode() == 403, "Missing CSRF forbidden");
        check(post(a, reviewBody(main, "5", "x", "fake")).statusCode() == 403, "Bad CSRF forbidden");
        HttpResponse<String> cross = post(b, reviewBody(main, "5", "x", csrfB) + "&" + pairs("accountId", accounts.get(0)));
        check(cross.statusCode() == 409 && reviewCount(main) == 0 && card(cross.body(), main).isEmpty(), "B cannot review A with forged accountId or see A history");
        for (int id : List.of(pending, processing, cancelled, Integer.MAX_VALUE)) check(post(a, reviewBody(id, "5", "x", csrf)).statusCode() == 409 && reviewCount(id) == 0, "HTTP ineligible order " + id);
        for (String rating : List.of("", "0", "-1", "6", "1.5", "1e0", "abc", "2147483648")) {
            check(post(a, reviewBody(main, rating, "x", csrf)).statusCode() == 400 && reviewCount(main) == 0, "HTTP invalid rating " + rating);
        }
        check(post(a, pairs("action", "review", "orderId", main, "csrf", csrf)).statusCode() == 400, "Missing rating rejected");
        for (String id : List.of("", "0", "-1", "abc", "2147483648")) check(post(a, pairs("action", "review", "orderId", id, "rating", 5, "csrf", csrf)).statusCode() == 400, "Malformed orderId " + id);
        check(post(a, reviewBody(main, "5", "x", csrf) + "&" + pairs("orderId", foreign)).statusCode() == 400, "Duplicate orderId rejected");
        check(post(a, reviewBody(main, "5", "x", csrf) + "&" + pairs("rating", 1)).statusCode() == 400, "Duplicate rating rejected");
        check(post(a, reviewBody(main, "5", "x", csrf) + "&" + pairs("comment", "other")).statusCode() == 400, "Duplicate comment rejected");
        check(post(a, reviewBody(main, "5", "x", csrf) + "&" + pairs("action", "cancel")).statusCode() == 400, "Duplicate action rejected");
        String draft = "<script>alert(9)</script>" + "x".repeat(500);
        HttpResponse<String> invalid = post(a, reviewBody(main, "4", draft, csrf));
        String card = card(invalid.body(), main);
        check(invalid.statusCode() == 400 && card.contains("&lt;script&gt;alert(9)&lt;/script&gt;")
                && card.contains("value=\"4\" selected") && reviewCount(main) == 0, "Long comment keeps escaped draft and selected rating");
        String unchanged = fingerprint(main); Map<String, Object> statistics = getStatistics();
        String comment = "Rất tốt <script>alert(1)</script> & <img src=x onerror=alert(2)>\nDòng thứ hai";
        HttpResponse<String> success = post(a, reviewBody(main, "5", comment, csrf) + "&" + pairs("accountId", accounts.get(1), "serviceId", services.get(1), "fullName", "FAKE_NAME", "totalAmount", 1));
        check(success.statusCode() == 302 && success.headers().firstValue("location").orElse("").endsWith("/orders#order-" + main), "Review succeeds with PRG to own order");
        check(reviewCount(main) == 1 && (int) reviewField(main, "Rating") == 5 && comment.equals(reviewField(main, "Comment"))
                && reviewField(main, "CreatedAt") != null && unchanged.equals(fingerprint(main)) && statistics.equals(getStatistics()), "Review persists but leaves every order/detail/statistics value unchanged");
        history = get(a, "/orders"); card = card(history.body(), main);
        check(!hasReview(history.body(), main) && card.contains("&lt;script&gt;") && card.contains("(5/5)")
                && history.body().contains("Đã gửi đánh giá cho yêu cầu #" + main), "Redirect reads saved review, hides form, escapes comment");
        check(!get(a, "/orders").body().contains("Đã gửi đánh giá cho yêu cầu #" + main) && reviewCount(main) == 1, "Refresh does not repost or repeat flash");
        check(post(a, reviewBody(main, "1", "CHANGED", csrf)).statusCode() == 409 && (int) reviewField(main, "Rating") == 5, "Duplicate HTTP review cannot overwrite");
        check(post(a, pairs("action", "editReview", "orderId", main, "rating", 1, "csrf", csrf)).statusCode() == 400
                && post(a, pairs("action", "deleteReview", "orderId", main, "csrf", csrf)).statusCode() == 400 && reviewCount(main) == 1, "No edit/delete review actions");
        String publicPage = get(browser(), "/services?id=" + services.get(0)).body();
        check(publicPage.contains("★ 3.7 / 5") && publicPage.contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                && publicPage.contains("&lt;b&gt;") && !publicPage.contains("FAKE_NAME") && !publicPage.contains("OTHER_SERVICE_ONLY"), "P2 public page reads real POST review, average and escaped server fullName/comment");
        try (ServiceDAO dao = new ServiceDAO()) {
            check(dao.getReviews(services.get(0)).size() == 3 && dao.getReviews(services.get(1)).size() == 1, "Public DAO uses order service, not forged serviceId");
        }
        HttpRequest same = request(reviewBody(race, "3", "DOUBLE", csrf));
        int[] codes = pair(a.sendAsync(same, HttpResponse.BodyHandlers.ofString()), a.sendAsync(same, HttpResponse.BodyHandlers.ofString()));
        check(oneWinner(codes) && reviewCount(race) == 1, "Two concurrent submissions create exactly one review");
        codes = pair(a.sendAsync(request(reviewBody(distinctOne, "5", "Order one", csrf)), HttpResponse.BodyHandlers.ofString()),
                a.sendAsync(request(reviewBody(distinctTwo, "4", "Order two", csrf)), HttpResponse.BodyHandlers.ofString()));
        check(codes[0] == 302 && codes[1] == 302 && reviewCount(distinctOne) == 1 && reviewCount(distinctTwo) == 1, "Different completed orders can be reviewed concurrently");
        update("UPDATE dbo.UserAccount SET IsActive=0 WHERE AccountID=?", accounts.get(0));
        try { check(post(a, reviewBody(locked, "5", "x", csrf)).statusCode() == 302 && reviewCount(locked) == 0, "Locked session cannot submit review"); }
        finally { update("UPDATE dbo.UserAccount SET IsActive=1 WHERE AccountID=?", accounts.get(0)); }
        login(a, marker + "_a");
        try (ServiceDAO dao = new ServiceDAO()) {
            List<ServiceReview> reviews = dao.getReviews(services.get(0));
            String expected = String.format(Locale.US, "%.1f", reviews.stream().mapToInt(ServiceReview::getRating).average().orElseThrow());
            check(get(browser(), "/services?id=" + services.get(0)).body().contains("★ " + expected + " / 5"), "Average updates after additional real reviews");
        }
    }

    private void outage(Path config) throws Exception {
        Path path = config.toAbsolutePath().normalize();
        if (!path.getFileName().toString().equals("keebforce-db.properties") || !path.getParent().getParent().getFileName().toString().startsWith("keebforce-p306-")) throw new IllegalArgumentException("Private P306 config required");
        byte[] saved = Files.readAllBytes(path); String csrf = token(get(a, "/orders").body());
        try {
            Files.writeString(path, "db.url=jdbc:sqlserver://127.0.0.1:1;databaseName=KeebForce_PRJ301;encrypt=false\ndb.user=test-only\ndb.password=unavailable\n");
            HttpResponse<String> failed = post(a, reviewBody(locked, "5", "x", csrf));
            check(failed.statusCode() == 503 && reviewCount(locked) == 0, "DB outage does not create review");
            failed = get(a, "/orders");
            check(failed.statusCode() == 503 && !failed.body().contains("Bạn chưa có yêu cầu nào") && !failed.body().contains("SQLException"), "History outage is friendly, not fake empty");
            failed = get(browser(), "/services?id=" + services.get(0));
            check(failed.statusCode() == 503 && !failed.body().contains("Chưa có đánh giá nào") && !failed.body().contains("jdbc:sqlserver"), "Public review outage is friendly, not fake empty");
        } finally { Files.write(path, saved); }
        check(get(a, "/orders").statusCode() == 200 && get(browser(), "/services?id=" + services.get(0)).statusCode() == 200, "Review pages recover after config restoration");
    }

    private String fingerprint(int id) throws SQLException {
        StringBuilder text = new StringBuilder();
        for (String sql : List.of("SELECT * FROM dbo.ServiceOrder WHERE OrderID=?", "SELECT * FROM dbo.OrderDetail WHERE OrderID=? ORDER BY OrderDetailID")) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.setInt(1, id); try (ResultSet result = statement.executeQuery()) {
                while (result.next()) for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) text.append(result.getObject(i)).append('|');
            } }
        }
        return text.toString();
    }
    private int reviewCount(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM dbo.ServiceReview WHERE OrderID=?")) { statement.setInt(1, id); try (ResultSet result = statement.executeQuery()) { result.next(); return result.getInt(1); } }
    }
    private Object reviewField(int id, String field) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT " + field + " FROM dbo.ServiceReview WHERE OrderID=?")) { statement.setInt(1, id); try (ResultSet result = statement.executeQuery()) { if (!result.next()) throw new AssertionError("Review missing"); return result.getObject(1); } }
    }
    private String counts() throws SQLException {
        StringBuilder text = new StringBuilder(); for (String table : List.of("UserAccount", "Brand", "Category", "Product", "WorkshopService", "ServiceOrder", "OrderDetail", "ServiceReview")) {
            try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM dbo." + table)) { result.next(); text.append(result.getInt(1)).append('/'); }
        } return text.toString();
    }
    private void bind(PreparedStatement statement, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) { if (values[i] instanceof String) statement.setNString(i + 1, (String) values[i]); else statement.setObject(i + 1, values[i]); }
    }
    private int insert(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { bind(statement, values); statement.executeUpdate(); try (ResultSet keys = statement.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Fixture key missing"); return keys.getInt(1); } }
    }
    private void update(String sql, Object... values) throws SQLException { try (PreparedStatement statement = connection.prepareStatement(sql)) { bind(statement, values); statement.executeUpdate(); } }
    private void cleanup() throws SQLException {
        for (int owner : accounts) {
            update("DELETE r FROM dbo.ServiceReview r JOIN dbo.ServiceOrder o ON o.OrderID=r.OrderID WHERE o.AccountID=?", owner);
            update("DELETE d FROM dbo.OrderDetail d JOIN dbo.ServiceOrder o ON o.OrderID=d.OrderID WHERE o.AccountID=?", owner);
            update("DELETE FROM dbo.ServiceOrder WHERE AccountID=?", owner);
            update("DELETE FROM dbo.UserAccount WHERE AccountID=? AND Username LIKE ?", owner, marker + "_%");
        }
        for (int id : parts) update("DELETE FROM dbo.Product WHERE ProductID=? AND ProductName LIKE ?", id, marker + "%");
        for (int id : services) update("DELETE FROM dbo.WorkshopService WHERE ServiceID=? AND ServiceName LIKE ?", id, "%" + marker + "%");
    }
    private HttpClient browser() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(10)).build(); }
    private HttpResponse<String> get(HttpClient client, String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpRequest request(String body) { return HttpRequest.newBuilder(URI.create(base + "/orders")).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(); }
    private HttpResponse<String> post(HttpClient client, String body) throws Exception { return client.send(request(body), HttpResponse.BodyHandlers.ofString()); }
    private void login(HttpClient client, String username) throws Exception {
        String csrf = token(get(client, "/login").body());
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/login")).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(pairs("username", username, "password", "Fixture@123", "csrf", csrf))).build();
        if (client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() != 302) throw new AssertionError("Login failed");
    }
    private String token(String html) { Matcher match = Pattern.compile("name=\"csrf\"\\s+value=\"([^\"]+)\"").matcher(html); if (!match.find()) throw new AssertionError("CSRF missing"); return match.group(1); }
    private String reviewBody(int id, String rating, String comment, String csrf) { return pairs("action", "review", "orderId", id, "rating", rating, "comment", comment, "csrf", csrf); }
    private String pairs(Object... values) { List<String> entries = new ArrayList<>(); for (int i = 0; i < values.length; i += 2) entries.add(URLEncoder.encode(values[i].toString(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(values[i + 1].toString(), StandardCharsets.UTF_8)); return String.join("&", entries); }
    private String card(String html, int id) { Matcher match = Pattern.compile("<article\\b[^>]*\\bid=\"order-" + id + "\"").matcher(html); if (!match.find()) return ""; return html.substring(match.start(), html.indexOf("</article>", match.end())); }
    private boolean hasReview(String html, int id) { return card(html, id).contains("name=\"action\" value=\"review\""); }
    private int[] pair(CompletableFuture<HttpResponse<String>> one, CompletableFuture<HttpResponse<String>> two) { return new int[]{one.join().statusCode(), two.join().statusCode()}; }
    private boolean oneWinner(int[] codes) { return (codes[0] == 302 && codes[1] == 409) || (codes[0] == 409 && codes[1] == 302); }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); System.out.println("OK " + (++checks) + " " + label); }
}
