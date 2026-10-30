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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

/** JDBC + HTTP thật; tạo fixture riêng và chỉ dọn dữ liệu thuộc các ID fixture đó. */
public class VerifyP303 extends OrderDAO {

    private static int checks;
    private final String marker = "P303_" + UUID.randomUUID().toString().replace("-", "");
    private int owner;
    private int service;
    private int inactiveService;
    private final List<Integer> products = new ArrayList<>();
    private String baseUrl;
    private HttpClient client;

    public VerifyP303() throws SQLException {
        super();
    }

    public static void main(String[] args) throws Exception {
        try (VerifyP303 test = new VerifyP303()) {
            test.baseUrl = args.length == 0 ? "http://127.0.0.1:18080/KeebForce" : args[0];
            String before = test.counts();
            try {
                test.setup();
                test.daoChecks();
                test.httpChecks();
            } finally {
                test.cleanup();
            }
            check(before.equals(test.counts()), "Fixture cleanup restores row counts in all 8 tables");
        }
        System.out.println("PASS " + checks + " JDBC/HTTP checks; fixture-only cleanup complete.");
    }

    private void setup() throws Exception {
        Account account = new Account();
        account.setUsername(marker);
        account.setEmail(marker + "@example.invalid");
        account.setFullName(marker);
        account.setPhone("0901234567");
        account.setPasswordHash(PasswordUtil.hashPassword("Fixture@123"));
        try (AccountDAO accounts = new AccountDAO()) {
            if (!accounts.register(account)) {
                throw new AssertionError("Cannot create fixture account");
            }
        }
        owner = account.getAccountId();
        service = insert("INSERT INTO dbo.WorkshopService (ServiceName, Description, BasePrice) VALUES (?, ?, ?)",
                marker + " service", "Công lắp ráp thử", new BigDecimal("200000"));
        inactiveService = insert("INSERT INTO dbo.WorkshopService (ServiceName, Description, BasePrice, IsActive) "
                + "VALUES (?, ?, ?, 0)", marker + " inactive", "Không nhận", new BigDecimal("200000"));
        Product seed;
        try (ProductDAO parts = new ProductDAO()) {
            seed = parts.getActiveProducts().get(0);
        }
        for (String price : List.of("1200000", "5000", "300000", "10000")) {
            products.add(insert("INSERT INTO dbo.Product (ProductName, CategoryID, BrandID, Price, IsActive) "
                    + "VALUES (?, ?, ?, ?, ?)", marker + " part " + products.size(), seed.getCategoryId(),
                    seed.getBrandId(), new BigDecimal(price), products.size() == 3 ? 0 : 1));
        }
    }

    private void daoChecks() throws Exception {
        Order empty = order();
        empty.setAccountId(1);
        empty.setServiceNameSnapshot("FAKE");
        empty.setServicePrice(BigDecimal.ONE);
        empty.setTotalAmount(BigDecimal.ONE);
        empty.setOrderStatus("COMPLETED");
        int first = createOrder(empty, List.of(), owner);
        Order persisted = read(first);
        check(first > 0 && persisted.getDetails().isEmpty() && money(persisted.getTotalAmount(), "200000")
                && persisted.getAccountId() == owner && "PENDING".equals(persisted.getOrderStatus())
                && persisted.getCompletedAt() == null && persisted.getAdminNote() == null,
                "Zero parts commits PENDING under server owner and service price");
        check(connection.getAutoCommit(), "AutoCommit restored after successful create");
        Order demo = order();
        OrderDetail fake = part(0, 1);
        fake.setUnitPrice(BigDecimal.ONE);
        fake.setProductNameSnapshot("FAKE");
        int demoId = createOrder(demo, List.of(fake, part(1, 70), part(2, 1)), owner);
        persisted = read(demoId);
        check(money(persisted.getTotalAmount(), "2050000") && money(persisted.getServicePrice(), "200000")
                && persisted.getServiceNameSnapshot().equals(marker + " service") && persisted.getDetails().size() == 3
                && persisted.getDetails().stream().noneMatch(d -> "FAKE".equals(d.getProductNameSnapshot()))
                && persisted.getDetails().get(0).getUnitPrice().compareTo(new BigDecimal("1200000")) == 0,
                "DB snapshots and exact total 2050000 ignore caller prices/names");
        reject(order(), List.of(part(0, 0)), owner, "DAO quantity zero");
        reject(order(), List.of(part(0, 201)), owner, "DAO quantity over 200");
        reject(order(), List.of(part(0, 1), part(0, 1)), owner, "DAO duplicate product");
        reject(order(), List.of(part(3, 1)), owner, "DAO inactive product");
        OrderDetail missingPart = new OrderDetail();
        missingPart.setProductId(Integer.MAX_VALUE);
        missingPart.setQuantity(1);
        reject(order(), List.of(missingPart), owner, "DAO nonexistent product");
        Order invalid = order();
        invalid.setServiceId(inactiveService);
        reject(invalid, List.of(), owner, "DAO inactive service");
        invalid = order();
        invalid.setServiceId(Integer.MAX_VALUE);
        reject(invalid, List.of(), owner, "DAO missing service");
        invalid = order();
        invalid.setContactName(" ");
        reject(invalid, List.of(), owner, "DAO blank contact");
        invalid = order();
        invalid.setContactPhone("abcdefgh");
        reject(invalid, List.of(), owner, "DAO invalid phone");
        invalid = order();
        invalid.setDeviceDescription("x".repeat(1001));
        reject(invalid, List.of(), owner, "DAO oversized device description");
        invalid = order();
        invalid.setPreferredDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1));
        reject(invalid, List.of(), owner, "DAO past date");
        reject(order(), List.of(), Integer.MAX_VALUE, "DAO missing account");
        try (AccountDAO accounts = new AccountDAO()) {
            reject(order(), List.of(), accounts.getByUsername("admin").getAccountId(), "DAO refuses ADMIN owner");
        }
        update("UPDATE dbo.UserAccount SET IsActive=0 WHERE AccountID=?", owner);
        try {
            reject(order(), List.of(), owner, "DAO refuses locked customer");
        } finally {
            update("UPDATE dbo.UserAccount SET IsActive=1 WHERE AccountID=?", owner);
        }
        update("UPDATE dbo.Product SET Price=? WHERE ProductID=?", new BigDecimal("999999999999999999"), products.get(0));
        try {
            reject(order(), List.of(part(0, 200)), owner, "DAO prevents DECIMAL(18,0) total overflow");
        } finally {
            update("UPDATE dbo.Product SET Price=1200000 WHERE ProductID=?", products.get(0));
        }
        String beforeFailure = counts();
        // A second connection briefly locks only OrderDetail; parent INSERT still runs.
        // The detail INSERT times out in SQL Server. No trigger/schema/test hook is added.
        try (VerifyP303 blocker = new VerifyP303()) {
            blocker.connection.setAutoCommit(false);
            try (Statement lock = blocker.connection.createStatement();
                    ResultSet rows = lock.executeQuery("SELECT COUNT(*) FROM dbo.OrderDetail WITH (TABLOCKX, HOLDLOCK)")) {
                rows.next();
                // SET through Statement applies to the session, not a prepared RPC scope.
                try (Statement setting = connection.createStatement()) {
                    setting.execute("SET LOCK_TIMEOUT 1000");
                    try (ResultSet timeout = setting.executeQuery("SELECT @@LOCK_TIMEOUT")) {
                        timeout.next();
                        check(timeout.getInt(1) == 1000, "Test connection has bounded lock wait");
                    }
                }
                try {
                    createOrder(order(), List.of(part(0, 1)), owner);
                    throw new AssertionError("Expected OrderDetail INSERT error");
                } catch (SQLException expected) {
                    check(expected.getErrorCode() == 1222, "Real SQL Server detail INSERT timeout (1222)");
                } finally {
                    try (Statement setting = connection.createStatement()) {
                        setting.execute("SET LOCK_TIMEOUT -1");
                    }
                    blocker.connection.rollback();
                }
            }
        }
        check(beforeFailure.equals(counts()) && connection.getAutoCommit(),
                "Detail failure rolls back parent and details, restores AutoCommit");
        int recovered = createOrder(order(), List.of(part(0, 1)), owner);
        check(read(recovered).getDetails().size() == 1, "Connection works again after rollback");
        // Snapshot must remain unchanged after future catalog edits, only fixture catalog is changed.
        update("UPDATE dbo.WorkshopService SET BasePrice=250000 WHERE ServiceID=?", service);
        update("UPDATE dbo.Product SET Price=6000 WHERE ProductID=?", products.get(1));
        try {
            check(money(read(demoId).getTotalAmount(), "2050000") && money(read(demoId).getServicePrice(), "200000"),
                    "Catalog edits do not change committed order snapshots");
        } finally {
            update("UPDATE dbo.WorkshopService SET BasePrice=200000 WHERE ServiceID=?", service);
            update("UPDATE dbo.Product SET Price=5000 WHERE ProductID=?", products.get(1));
        }
    }

    private void httpChecks() throws Exception {
        client = browser();
        login(client, marker, "Fixture@123");
        String page = freshForm();
        check(!token(page, "csrf").equals(token(page, "orderFormToken")), "Submit token separate from P1 CSRF");
        Map<String, String> base = fields(page);
        int before = ownerCount();
        for (String[] bad : new String[][]{{"contactName", " "}, {"contactName", "x".repeat(101)},
                {"contactPhone", "abcdefghi"}, {"deviceDescription", " "},
                {"deviceDescription", "x".repeat(1001)}, {"customerNote", "x".repeat(1001)},
                {"preferredDate", "2020-01-01"}, {"preferredDate", "2026-02-31"}, {"action", "delete"}}) {
            Map<String, String> fields = new LinkedHashMap<>(base);
            fields.put(bad[0], bad[1]);
            check(post(client, body(fields)).statusCode() == 400, "HTTP invalid " + bad[0]);
        }
        for (String quantity : List.of("0", "201", "1.5", "1e2", "-1", "")) {
            String extra = pairs("productIds", products.get(0), "quantity_" + products.get(0), quantity);
            check(post(client, body(base) + "&" + extra).statusCode() == 400, "HTTP invalid quantity " + quantity);
        }
        String dup = pairs("productIds", products.get(0), "productIds", products.get(0), "quantity_" + products.get(0), 1);
        check(post(client, body(base) + "&" + dup).statusCode() == 400, "HTTP duplicate product IDs");
        dup = pairs("productIds", products.get(0), "quantity_" + products.get(0), 1, "quantity_" + products.get(0), 2);
        check(post(client, body(base) + "&" + dup).statusCode() == 400, "HTTP duplicate quantity parameter");
        check(post(client, body(base) + "&" + pairs("serviceId", service)).statusCode() == 400, "HTTP duplicate service ID");
        check(post(client, body(base) + "&" + pairs("productIds", products.get(0))).statusCode() == 400, "HTTP missing quantity");
        check(post(client, body(base) + "&" + pairs("productIds", products.get(3), "quantity_" + products.get(3), 1))
                .statusCode() == 400, "HTTP inactive product");
        Map<String, String> escaped = new LinkedHashMap<>(base);
        escaped.put("contactName", "<script>alert(1)</script>");
        HttpResponse<String> invalid = post(client, body(escaped) + "&"
                + pairs("productIds", products.get(0), "quantity_" + products.get(0), 201));
        check(invalid.statusCode() == 400 && invalid.body().contains("&lt;script&gt;")
                && !invalid.body().contains("<script>alert(1)") && invalid.body().contains("value=\"201\"")
                && token(page, "orderFormToken").equals(token(invalid.body(), "orderFormToken")),
                "Invalid form preserves escaped inputs, quantity and unconsumed token");
        Map<String, String> blankContact = new LinkedHashMap<>(base);
        blankContact.put("contactName", "");
        String twoParts = "&" + pairs("productIds", products.get(0), "quantity_" + products.get(0), 2,
                "productIds", products.get(1), "quantity_" + products.get(1), 70);
        invalid = post(client, body(blankContact) + twoParts);
        check(invalid.statusCode() == 400 && checked(invalid.body(), products.get(0))
                && checked(invalid.body(), products.get(1)) && invalid.body().contains("value=\"70\""),
                "Blank contact preserves all selected parts and quantities");
        Map<String, String> badDate = new LinkedHashMap<>(base);
        badDate.put("preferredDate", "2026-02-31");
        invalid = post(client, body(badDate) + twoParts);
        check(invalid.statusCode() == 400 && checked(invalid.body(), products.get(0)) && checked(invalid.body(), products.get(1)),
                "Invalid date preserves all selected parts");
        check(ownerCount() == before, "All invalid POSTs leave orders unchanged");
        HttpResponse<String> success = post(client, body(base));
        check(success.statusCode() == 302 && success.headers().firstValue("location").orElse("").endsWith("/orders"),
                "No-parts valid retry uses same token and PRG to /orders");
        check(ownerCount() == before + 1, "Successful retry creates exactly one order");
        HttpResponse<String> history = get(client, "/orders");
        check(history.statusCode() == 200 && history.body().contains("200.000") && history.body().contains(marker),
                "History is rendered after redirect");
        get(client, "/orders");
        check(ownerCount() == before + 1, "Refresh after redirect does not insert");
        check(post(client, body(base)).statusCode() == 409 && ownerCount() == before + 1, "Consumed token replay rejected");
        String nextPage = freshForm();
        Map<String, String> demo = fields(nextPage);
        String extras = pairs("productIds", products.get(0), "quantity_" + products.get(0), 1,
                "productIds", products.get(1), "quantity_" + products.get(1), 70,
                "productIds", products.get(2), "quantity_" + products.get(2), 1,
                "accountId", 1, "role", "ADMIN", "totalAmount", 1, "servicePrice", 1, "unitPrice", 1,
                "orderStatus", "COMPLETED", "adminNote", "FAKE");
        before = ownerCount();
        check(post(client, body(demo) + "&" + extras).statusCode() == 302, "Multi-part POST accepts valid selections");
        Order newest = getOrdersByAccount(owner).get(0);
        check(ownerCount() == before + 1 && money(newest.getTotalAmount(), "2050000")
                && newest.getDetails().size() == 3 && newest.getAccountId() == owner
                && "PENDING".equals(newest.getOrderStatus()) && newest.getAdminNote() == null,
                "HTTP ignores forged owner/role/status/note/prices, persists exact DB total");
        page = freshForm();
        String concurrentBody = body(fields(page)) + "&" + extras;
        before = ownerCount();
        CompletableFuture<HttpResponse<String>> one = client.sendAsync(postRequest(concurrentBody), HttpResponse.BodyHandlers.ofString());
        CompletableFuture<HttpResponse<String>> two = client.sendAsync(postRequest(concurrentBody), HttpResponse.BodyHandlers.ofString());
        int a = one.join().statusCode();
        int b = two.join().statusCode();
        check(((a == 302 && b == 409) || (a == 409 && b == 302)) && ownerCount() == before + 1,
                "Simultaneous double click: one success, one conflict, one DB order");
        page = freshForm();
        Map<String, String> withoutCsrf = fields(page);
        withoutCsrf.remove("csrf");
        before = ownerCount();
        check(post(client, body(withoutCsrf)).statusCode() == 403 && ownerCount() == before, "P1 CSRF still enforced");
        Map<String, String> badToken = fields(page);
        badToken.put("orderFormToken", "fake");
        check(post(client, body(badToken)).statusCode() == 409 && ownerCount() == before, "Forged form token rejected");
        update("UPDATE dbo.WorkshopService SET IsActive=0 WHERE ServiceID=?", service);
        try {
            check(post(client, body(fields(page))).statusCode() == 400 && ownerCount() == before,
                    "Service stopped after GET is rejected on POST");
        } finally {
            update("UPDATE dbo.WorkshopService SET IsActive=1 WHERE ServiceID=?", service);
        }
        check(post(client, body(fields(page))).statusCode() == 302 && ownerCount() == before + 1,
                "Service validation failure did not consume form token");
        HttpClient admin = browser();
        login(admin, "admin", "Admin@123");
        check(post(admin, body(fields(freshForm()))).statusCode() == 403, "ADMIN POST to request denied");
        HttpClient anonymous = browser();
        check(post(anonymous, body(base)).statusCode() == 302, "Anonymous POST redirected to login");
        page = freshForm();
        update("UPDATE dbo.UserAccount SET IsActive=0 WHERE AccountID=?", owner);
        before = ownerCount();
        try {
            HttpResponse<String> locked = post(client, body(fields(page)));
            check(locked.statusCode() == 302 && locked.headers().firstValue("location").orElse("").contains("inactive=1")
                    && ownerCount() == before, "Locked account invalidates session before POST");
        } finally {
            update("UPDATE dbo.UserAccount SET IsActive=1 WHERE AccountID=?", owner);
        }
    }

    private Order order() {
        Order order = new Order();
        order.setServiceId(service);
        order.setContactName(marker);
        order.setContactPhone("0901234567");
        order.setDeviceDescription("Bàn phím 75% <3");
        return order;
    }

    private OrderDetail part(int index, int quantity) {
        OrderDetail detail = new OrderDetail();
        detail.setProductId(products.get(index));
        detail.setQuantity(quantity);
        return detail;
    }

    private Order read(int id) throws SQLException {
        try (OrderDAO reader = new OrderDAO()) {
            return reader.getOrdersByAccount(owner).stream().filter(o -> o.getOrderId() == id).findFirst().orElseThrow();
        }
    }

    private void reject(Order order, List<OrderDetail> details, int accountId, String label) throws SQLException {
        int before = ownerCount();
        try {
            createOrder(order, details, accountId);
            throw new AssertionError(label + " accepted");
        } catch (IllegalArgumentException expected) {
            check(ownerCount() == before && connection.getAutoCommit(), label + " leaves no order");
        }
    }

    private int ownerCount() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM dbo.ServiceOrder WHERE AccountID=?")) {
            statement.setInt(1, owner);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private String counts() throws SQLException {
        StringBuilder result = new StringBuilder();
        for (String table : List.of("UserAccount", "Brand", "Category", "Product", "WorkshopService", "ServiceOrder", "OrderDetail", "ServiceReview")) {
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM dbo." + table)) {
                rows.next();
                result.append(rows.getInt(1)).append('/');
            }
        }
        return result.toString();
    }

    private int insert(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, values);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Missing fixture key");
                return keys.getInt(1);
            }
        }
    }

    private void update(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof String) statement.setNString(i + 1, (String) values[i]);
            else statement.setObject(i + 1, values[i]);
        }
    }

    private void cleanup() throws SQLException {
        // owner là ID account mới, duy nhất do test tạo, không xóa đơn/account seed hay người dùng.
        if (owner > 0) {
            update("DELETE d FROM dbo.OrderDetail d JOIN dbo.ServiceOrder o ON o.OrderID=d.OrderID WHERE o.AccountID=?", owner);
            update("DELETE FROM dbo.ServiceOrder WHERE AccountID=?", owner);
            update("DELETE FROM dbo.UserAccount WHERE AccountID=? AND Username=?", owner, marker);
        }
        for (int id : products) update("DELETE FROM dbo.Product WHERE ProductID=? AND ProductName LIKE ?", id, marker + "%");
        if (service > 0) update("DELETE FROM dbo.WorkshopService WHERE ServiceID=? AND ServiceName=?", service, marker + " service");
        if (inactiveService > 0) update("DELETE FROM dbo.WorkshopService WHERE ServiceID=? AND ServiceName=?", inactiveService, marker + " inactive");
    }

    private HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private HttpResponse<String> get(HttpClient browser, String path) throws Exception {
        return browser.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(20)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest postRequest(String body) {
        return HttpRequest.newBuilder(URI.create(baseUrl + "/request")).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private HttpResponse<String> post(HttpClient browser, String body) throws Exception {
        return browser.send(postRequest(body), HttpResponse.BodyHandlers.ofString());
    }

    private void login(HttpClient browser, String username, String password) throws Exception {
        String page = get(browser, "/login").body();
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(pairs("csrf", token(page, "csrf"), "username", username, "password", password))).build();
        if (browser.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() != 302) throw new AssertionError("Login failed");
    }

    private String freshForm() throws Exception {
        HttpResponse<String> page = get(client, "/request?serviceId=" + service);
        if (page.statusCode() != 200) throw new AssertionError("GET form failed: " + page.statusCode());
        return page.body();
    }

    private Map<String, String> fields(String page) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("csrf", token(page, "csrf"));
        fields.put("orderFormToken", token(page, "orderFormToken"));
        fields.put("action", "create");
        fields.put("serviceId", String.valueOf(service));
        fields.put("contactName", marker);
        fields.put("contactPhone", "0901234567");
        fields.put("deviceDescription", "Bàn phím 75% <3");
        fields.put("preferredDate", "");
        fields.put("customerNote", "");
        return fields;
    }

    private String token(String html, String name) {
        Matcher matcher = Pattern.compile("name=\"" + Pattern.quote(name) + "\"\\s+value=\"([^\"]+)\"").matcher(html);
        if (!matcher.find()) throw new AssertionError("Missing " + name);
        return matcher.group(1);
    }

    private boolean checked(String html, int productId) {
        return Pattern.compile("id=\"part-" + productId + "\"[^>]*\\bchecked\\b").matcher(html).find();
    }

    private String body(Map<String, String> fields) {
        List<String> entries = new ArrayList<>();
        fields.forEach((key, value) -> entries.add(encode(key) + "=" + encode(value)));
        return String.join("&", entries);
    }

    private String pairs(Object... pairs) {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) entries.add(encode(pairs[i].toString()) + "=" + encode(pairs[i + 1].toString()));
        return String.join("&", entries);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean money(BigDecimal value, String expected) {
        return value.compareTo(new BigDecimal(expected)) == 0;
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        System.out.println("OK " + (++checks) + " " + label);
    }
}
