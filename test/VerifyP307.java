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
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.Account;
import model.Order;
import model.Product;
import model.ServiceReview;
import model.WorkshopService;

/** Nghiệm thu route thật: đăng ký, catalog, create, Admin, history, review. SQL chỉ đọc/dọn fixture. */
public class VerifyP307 extends OrderDAO {
    private static int checks;
    private final String marker = "P307_" + UUID.randomUUID().toString().replace("-", "");
    private final List<Integer> accounts = new ArrayList<>();
    private final List<Integer> parts = new ArrayList<>();
    private String base;
    private int service, main;
    private HttpClient a, b, admin;
    private Map<String, Object> initialStatistics;

    public VerifyP307() throws SQLException { super(); }

    public static void main(String[] args) throws Exception {
        try (VerifyP307 test = new VerifyP307()) {
            if (args.length > 0 && args[0].equals("--fingerprint")) { System.out.println(test.fingerprint()); return; }
            test.base = args.length == 0 ? "http://127.0.0.1:18080/KeebForce" : args[0];
            String original = test.fingerprint();
            test.initialStatistics = test.getStatistics();
            try {
                test.setup(); test.flow();
                if (args.length > 1 && !args[1].equals("-")) test.outage(Path.of(args[1]));
                if (List.of(args).contains("--preview")) {
                    System.out.println("PREVIEW_USERNAME=" + test.marker + "_a");
                    System.out.println("PREVIEW_SERVICE=" + test.service);
                    System.out.println("PREVIEW_PRODUCTS=" + test.parts);
                    System.out.println("PREVIEW_READY (Complete browser flow, then Enter to verify SQL and clean fixtures)");
                    System.in.read();
                    test.verifyBrowserFlow();
                }
            } finally { test.cleanup(); }
            check(original.equals(test.fingerprint()), "All row contents in all 8 tables restored exactly after fixture cleanup");
        }
        System.out.println("PASS " + checks + " end-to-end HTTP/SQL checks.");
    }

    private void setup() throws Exception {
        try (ServiceDAO services = new ServiceDAO(); ProductDAO products = new ProductDAO()) {
            WorkshopService chosen = services.getActiveServices().stream().filter(s -> s.getServiceName().equals("Lắp ráp bàn phím")).findFirst().orElseThrow();
            service = chosen.getServiceId();
            List<Product> available = products.getActiveProducts();
            for (String name : List.of("Kit bàn phím 75% demo", "Switch linear demo", "Bộ keycap PBT demo")) {
                parts.add(available.stream().filter(p -> p.getProductName().equals(name)).findFirst().orElseThrow().getProductId());
            }
            BigDecimal total = chosen.getBasePrice();
            for (int i = 0; i < parts.size(); i++) total = total.add(products.getProductById(parts.get(i)).getPrice().multiply(BigDecimal.valueOf(i == 1 ? 70 : 1)));
            check(total.compareTo(new BigDecimal("2050000")) == 0 && chosen.getBasePrice().compareTo(new BigDecimal("200000")) == 0, "Existing P2 seed resolves canonical 2050000 demo, no catalog writes");
        }
        a = browser(); b = browser(); admin = browser();
        check(get(a, "/home").statusCode() == 200 && get(a, "/parts?q=" + encode("bàn phím")).statusCode() == 200, "Home and Unicode catalog search work");
        check(get(a, "/services?id=" + service).body().contains("/request?serviceId=" + service), "Catalog links to actual request route with context");
        register(a, "a", "Khách nghiệm thu P3-07 A"); register(b, "b", "Khách nghiệm thu P3-07 B");
        login(a, marker + "_a", "Fixture@123"); login(b, marker + "_b", "Fixture@123"); login(admin, "admin", "Admin@123");
    }

    private void register(HttpClient client, String suffix, String name) throws Exception {
        String username = marker + "_" + suffix;
        String csrf = token(get(client, "/register").body(), "csrf");
        HttpResponse<String> response = post(client, "/register", pairs("csrf", csrf, "username", username, "email", username + "@example.invalid",
                "fullName", name, "phone", "0900000000", "password", "Fixture@123", "confirmPassword", "Fixture@123", "role", "ADMIN"));
        try (AccountDAO dao = new AccountDAO()) {
            Account account = dao.getByUsername(username);
            if (account != null) accounts.add(account.getAccountId());
            check(response.statusCode() == 302 && response.headers().firstValue("location").orElse("").contains("registered=1")
                    && account != null && account.getRole().equals("CUSTOMER") && account.isActive(), "Register fixture " + suffix + " through AuthServlet, forged role ignored");
        }
    }

    private void flow() throws Exception {
        String form = get(a, "/request?serviceId=" + service).body();
        String body = createBody(form, true);
        String invalid = body.replace("quantity_" + parts.get(1) + "=70", "quantity_" + parts.get(1) + "=201");
        check(post(a, "/request", invalid).statusCode() == 400 && getOrdersByAccount(accounts.get(0)).isEmpty(), "Invalid quantity cannot create an order");
        HttpResponse<String> response = post(a, "/request", body + "&" + pairs("accountId", accounts.get(1), "totalAmount", 1, "role", "ADMIN"));
        check(response.statusCode() == 302 && response.headers().firstValue("location").orElse("").endsWith("/orders"), "Customer submits canonical order through HTTP with PRG");
        Order order = getOrdersByAccount(accounts.get(0)).get(0); main = order.getOrderId();
        check(order.getDetails().size() == 3 && order.getTotalAmount().compareTo(new BigDecimal("2050000")) == 0
                && order.getServicePrice().compareTo(new BigDecimal("200000")) == 0 && order.getOrderStatus().equals("PENDING"), "SQL stores owner, PENDING, 3 snapshots and exact 2050000 total");
        check(order.getDetails().stream().anyMatch(d -> d.getProductId() == parts.get(1) && d.getQuantity() == 70 && d.getSubtotal().compareTo(new BigDecimal("350000")) == 0), "SQL switch quantity and subtotal exact");
        String history = get(a, "/orders").body(); get(a, "/orders");
        check(history.contains("order-" + main) && history.contains("2.050.000") && getOrdersByAccount(accounts.get(0)).size() == 1, "History reads persisted order; refresh inserts nothing");
        check(post(a, "/request", body).statusCode() == 409 && getOrdersByAccount(accounts.get(0)).size() == 1, "Replay of same create token rejected");
        String csrfA = token(history, "csrf"), csrfB = token(get(b, "/orders").body(), "csrf");
        check(!get(b, "/orders?accountId=" + accounts.get(0)).body().contains("id=\"order-" + main + "\""), "Customer B cannot view A history with forged owner");
        check(post(b, "/orders", pairs("action", "cancel", "orderId", main, "csrf", csrfB)).statusCode() == 409, "Customer B cannot cancel A");
        check(post(a, "/orders", pairs("action", "review", "orderId", main, "rating", 5, "csrf", csrfA)).statusCode() == 409, "Pending order cannot be reviewed");
        String csrfAdmin = token(get(admin, "/admin/orders").body(), "csrf");
        check(post(admin, "/admin/users", pairs("action", "toggle", "accountId", accounts.get(0), "active", false, "csrf", csrfAdmin)).statusCode() == 302, "Admin locks fixture account using P1 route");
        try {
            check(get(a, "/orders").statusCode() == 302 && post(a, "/orders", pairs("action", "cancel", "orderId", main, "csrf", csrfA)).statusCode() == 302
                    && read(main).getOrderStatus().equals("PENDING"), "Locked session cannot read or mutate order");
        } finally {
            if (post(admin, "/admin/users", pairs("action", "toggle", "accountId", accounts.get(0), "active", true, "csrf", csrfAdmin)).statusCode() != 302) throw new AssertionError("Could not unlock fixture");
        }
        login(a, marker + "_a", "Fixture@123"); csrfA = token(get(a, "/orders").body(), "csrf");
        check(get(admin, "/admin/orders?status=PENDING").body().contains("id=\"order-" + main + "\""), "Admin pending filter sees submitted order");
        check(adminStatus(main, "COMPLETED", csrfAdmin).statusCode() == 409, "Admin cannot skip PROCESSING");
        check(adminStatus(main, "PROCESSING", csrfAdmin).statusCode() == 302 && read(main).getCompletedAt() == null, "Admin accepts request");
        String note = "Đã nhận bàn phím, đang lắp ráp theo yêu cầu.";
        check(post(admin, "/admin/orders", pairs("action", "note", "orderId", main, "adminNote", note, "csrf", csrfAdmin)).statusCode() == 302
                && get(a, "/orders").body().contains(note), "Admin note appears in customer history");
        check(post(a, "/orders", pairs("action", "cancel", "orderId", main, "csrf", csrfA)).statusCode() == 409, "Customer cannot cancel processing order");
        check(adminStatus(main, "COMPLETED", csrfAdmin).statusCode() == 302 && read(main).getCompletedAt() != null, "Admin completes with DB CompletedAt");
        String comment = "Lắp ráp đúng yêu cầu, bàn phím hoạt động tốt. Cảm ơn workshop!";
        check(post(a, "/orders", pairs("action", "review", "orderId", main, "rating", 5, "comment", comment, "csrf", csrfA)).statusCode() == 302, "Customer submits five-star review after completion");
        order = read(main);
        check(order.getReview() != null && order.getReview().getRating() == 5 && order.getReview().getComment().equals(comment)
                && order.getTotalAmount().compareTo(new BigDecimal("2050000")) == 0, "SQL review persisted without changing total");
        check(post(a, "/orders", pairs("action", "review", "orderId", main, "rating", 1, "csrf", csrfA)).statusCode() == 409, "Review replay blocked");
        check(get(browser(), "/services?id=" + service).body().contains(comment), "Anonymous public service page reads actual submitted review");
        check(((BigDecimal) getStatistics().get("completedValue")).compareTo(((BigDecimal) initialStatistics.get("completedValue")).add(new BigDecimal("2050000"))) == 0
                && get(admin, "/admin/dashboard").body().contains("Tổng giá trị đơn hoàn tất"), "Dashboard reflects completed value only");
        int noParts = createNoParts();
        check(read(noParts).getDetails().isEmpty() && read(noParts).getTotalAmount().compareTo(new BigDecimal("200000")) == 0, "Service-only order stores no parts and service fee only");
        check(post(a, "/orders", pairs("action", "cancel", "orderId", noParts, "csrf", csrfA)).statusCode() == 302
                && post(a, "/orders", pairs("action", "cancel", "orderId", noParts, "csrf", csrfA)).statusCode() == 409, "Customer cancel is conditional and repeat-safe");
        int rejected = createNoParts();
        check(adminStatus(rejected, "CANCELLED", csrfAdmin).statusCode() == 302 && read(rejected).getCompletedAt() == null, "Admin can reject a pending request");
        check(getOrdersByAccount(accounts.get(1)).isEmpty() && read(main).getOrderStatus().equals("COMPLETED"), "Final owner separation and main order state intact");
    }

    private void outage(Path config) throws Exception {
        Path path = config.toAbsolutePath().normalize();
        if (!path.getFileName().toString().equals("keebforce-db.properties") || !path.getParent().getParent().getFileName().toString().startsWith("keebforce-p307-")) throw new IllegalArgumentException("Private P307 config required");
        String csrf = token(get(a, "/orders").body(), "csrf"); byte[] saved = Files.readAllBytes(path);
        try {
            Files.writeString(path, "db.url=jdbc:sqlserver://127.0.0.1:1;databaseName=KeebForce_PRJ301;encrypt=false\ndb.user=test-only\ndb.password=unavailable\n");
            for (String route : List.of("/orders", "/admin/orders", "/admin/dashboard", "/services?id=" + service)) {
                HttpResponse<String> failed = get(route.startsWith("/admin") ? admin : a, route);
                check(failed.statusCode() == 503 && !failed.body().contains("jdbc:sqlserver") && !failed.body().contains("SQLException"), "DB outage returns friendly 503 " + route);
            }
            check(post(a, "/orders", pairs("action", "review", "orderId", main, "rating", 5, "csrf", csrf)).statusCode() == 503, "Protected POST blocked during outage");
        } finally { Files.write(path, saved); }
        check(get(a, "/orders").statusCode() == 200 && get(admin, "/admin/dashboard").statusCode() == 200, "Routes recover after private config restoration");
    }

    private void verifyBrowserFlow() throws Exception {
        List<Order> own = getOrdersByAccount(accounts.get(0));
        check(own.size() == 4 && own.stream().filter(o -> o.getOrderStatus().equals("COMPLETED") && o.getDetails().size() == 3
                && o.getTotalAmount().compareTo(new BigDecimal("2050000")) == 0 && o.getCompletedAt() != null).count() == 2,
                "Browser full flow verified in SQL: second 2050000 completed order with 3 details");
        check(own.stream().filter(o -> o.getOrderStatus().equals("COMPLETED") && o.getReview() != null && o.getReview().getRating() == 5).count() == 2,
                "Both HTTP and browser orders have exactly one five-star review");
        check(own.stream().filter(o -> o.getOrderStatus().equals("CANCELLED") && o.getDetails().isEmpty() && o.getCompletedAt() == null).count() == 2,
                "Cancelled service-only orders remain unchanged after browser flow");
        check(((BigDecimal) getStatistics().get("completedValue")).compareTo(((BigDecimal) initialStatistics.get("completedValue")).add(new BigDecimal("4100000"))) == 0,
                "SQL dashboard value increases by exactly 4100000 for two completed demo orders");
        try (ServiceDAO dao = new ServiceDAO()) {
            List<Integer> ids = own.stream().filter(o -> o.getOrderStatus().equals("COMPLETED")).map(Order::getOrderId).toList();
            check(dao.getReviews(service).stream().filter(r -> ids.contains(r.getOrderId()) && r.getRating() == 5).count() == 2, "P2 public DAO reads both real end-to-end reviews");
        }
    }

    private int createNoParts() throws Exception {
        String form = get(a, "/request?serviceId=" + service).body();
        if (post(a, "/request", createBody(form, false)).statusCode() != 302) throw new AssertionError("Service-only create failed");
        return getOrdersByAccount(accounts.get(0)).get(0).getOrderId();
    }
    private String createBody(String page, boolean withParts) {
        String body = pairs("csrf", token(page, "csrf"), "orderFormToken", token(page, "orderFormToken"), "action", "create", "serviceId", service,
                "contactName", "Khách nghiệm thu P3-07 A", "contactPhone", "0900000000", "deviceDescription", "Lắp ráp bàn phím 75%, kit và linh kiện chọn tại workshop.");
        if (withParts) for (int i = 0; i < parts.size(); i++) body += "&" + pairs("productIds", parts.get(i), "quantity_" + parts.get(i), i == 1 ? 70 : 1);
        return body;
    }
    private HttpResponse<String> adminStatus(int id, String status, String csrf) throws Exception { return post(admin, "/admin/orders", pairs("action", "status", "orderId", id, "newStatus", status, "csrf", csrf)); }
    private Order read(int id) throws SQLException { return getOrdersByAccount(accounts.get(0)).stream().filter(o -> o.getOrderId() == id).findFirst().orElseThrow(); }

    private String fingerprint() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String table : List.of("UserAccount", "Brand", "Category", "Product", "WorkshopService", "ServiceOrder", "OrderDetail", "ServiceReview")) {
            digest.update(table.getBytes(StandardCharsets.UTF_8));
            try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM dbo." + table + " ORDER BY 1")) {
                while (result.next()) for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) {
                    Object value = result.getObject(i); String text = value == null ? "NULL:" : value.getClass().getName() + ":" + value;
                    digest.update((text.length() + ":" + text + "\n").getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private void cleanup() throws SQLException {
        for (int account : accounts) {
            update("DELETE r FROM dbo.ServiceReview r JOIN dbo.ServiceOrder o ON o.OrderID=r.OrderID WHERE o.AccountID=?", account);
            update("DELETE d FROM dbo.OrderDetail d JOIN dbo.ServiceOrder o ON o.OrderID=d.OrderID WHERE o.AccountID=?", account);
            update("DELETE FROM dbo.ServiceOrder WHERE AccountID=?", account);
            update("DELETE FROM dbo.UserAccount WHERE AccountID=? AND Username LIKE ?", account, marker + "_%");
        }
    }
    private void update(String sql, Object... values) throws SQLException { try (PreparedStatement statement = connection.prepareStatement(sql)) { for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]); statement.executeUpdate(); } }
    private HttpClient browser() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(10)).build(); }
    private HttpResponse<String> get(HttpClient client, String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> post(HttpClient client, String path, String body) throws Exception { return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()); }
    private void login(HttpClient client, String username, String password) throws Exception {
        String csrf = token(get(client, "/login").body(), "csrf");
        if (post(client, "/login", pairs("username", username, "password", password, "csrf", csrf)).statusCode() != 302) throw new AssertionError("Login failed");
    }
    private String token(String html, String name) { Matcher match = Pattern.compile("name=\"" + name + "\"\\s+value=\"([^\"]+)\"").matcher(html); if (!match.find()) throw new AssertionError("Missing token " + name); return match.group(1); }
    private String pairs(Object... values) { List<String> items = new ArrayList<>(); for (int i = 0; i < values.length; i += 2) items.add(encode(values[i].toString()) + "=" + encode(values[i + 1].toString())); return String.join("&", items); }
    private String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); System.out.println("OK " + (++checks) + " " + message); }
}
