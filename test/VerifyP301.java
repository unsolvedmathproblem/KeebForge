import dao.AccountDAO;
import dao.OrderDAO;
import dao.ProductDAO;
import dao.ServiceDAO;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import model.Account;
import model.Order;
import model.OrderDetail;
import model.Product;
import model.WorkshopService;

/** Kiểm tra JDBC thật, chạy riêng ngoài WAR; mọi fixture luôn rollback. */
public class VerifyP301 extends OrderDAO {

    private static int checks;

    public VerifyP301() throws SQLException {
        super();
    }

    public static void main(String[] args) throws Exception {
        Account customerA;
        Account customerB;
        WorkshopService service;
        List<Product> products;
        try (AccountDAO accounts = new AccountDAO();
                ServiceDAO services = new ServiceDAO(); ProductDAO parts = new ProductDAO()) {
            customerA = accounts.getByUsername("customer1");
            customerB = accounts.getByUsername("customer2");
            service = services.getActiveServices().stream()
                    .filter(s -> "Lắp ráp bàn phím".equals(s.getServiceName())).findFirst().orElseThrow();
            products = parts.getActiveProducts();
        }
        check(customerA != null && customerB != null && customerA.isActive() && customerB.isActive()
                && "CUSTOMER".equals(customerA.getRole()) && "CUSTOMER".equals(customerB.getRole()),
                "P1 AccountDAO bàn giao hai customer active");
        Product kit = product(products, "Kit bàn phím 75% demo");
        Product switches = product(products, "Switch linear demo");
        Product keycaps = product(products, "Bộ keycap PBT demo");
        BigDecimal expected = service.getBasePrice().add(kit.getPrice())
                .add(switches.getPrice().multiply(BigDecimal.valueOf(70))).add(keycaps.getPrice());
        check(expected.compareTo(new BigDecimal("2050000")) == 0, "Giá seed qua DAO P2 = 2050000");

        try (VerifyP301 dao = new VerifyP301()) {
            String before = dao.counts();
            int existingOrders = dao.getAllOrders(null).size();
            dao.connection.setAutoCommit(false);
            try {
                int pending = dao.fixtureOrder(customerA, service, "PENDING", expected, true);
                dao.fixtureDetail(pending, kit, 1);
                dao.fixtureDetail(pending, switches, 70);
                dao.fixtureDetail(pending, keycaps, 1);
                int completed = dao.fixtureOrder(customerA, service, "COMPLETED", service.getBasePrice(), false);
                int processing = dao.fixtureOrder(customerB, service, "PROCESSING", service.getBasePrice(), false);
                int cancelled = dao.fixtureOrder(customerB, service, "CANCELLED", service.getBasePrice(), false);
                dao.fixtureReview(completed);

                List<Order> mine = dao.getOrdersByAccount(customerA.getAccountId());
                Order order = find(mine, pending);
                check(mine.stream().allMatch(o -> o.getAccountId() == customerA.getAccountId())
                        && mine.stream().noneMatch(o -> o.getOrderId() == processing || o.getOrderId() == cancelled),
                        "Query khách A chỉ đọc owner A");
                List<Order> theirs = dao.getOrdersByAccount(customerB.getAccountId());
                check(theirs.stream().allMatch(o -> o.getAccountId() == customerB.getAccountId())
                        && theirs.stream().noneMatch(o -> o.getOrderId() == pending || o.getOrderId() == completed),
                        "Query khách B không đọc đơn A");
                check(dao.getOrdersByAccount(Integer.MAX_VALUE).isEmpty(), "Owner không có đơn trả list rỗng");
                check(order.getServiceId() == service.getServiceId()
                        && "P3-01 · Công lắp ráp snapshot".equals(order.getServiceNameSnapshot())
                        && order.getServicePrice().compareTo(service.getBasePrice()) == 0,
                        "Tên/giá dịch vụ lấy snapshot trong đơn");
                check("Nguyễn Khách <&>".equals(order.getContactName())
                        && "0901234567".equals(order.getContactPhone())
                        && "Bàn phím 75% — đồ riêng".equals(order.getDeviceDescription())
                        && "Giữ lại keycap cũ".equals(order.getCustomerNote())
                        && "Chờ nhận bàn phím".equals(order.getAdminNote()), "Mapping Unicode/contact/note đầy đủ");
                check(LocalDate.of(2026, 11, 5).equals(order.getPreferredDate())
                        && LocalDateTime.of(2026, 10, 30, 9, 0).equals(order.getCreatedAt())
                        && order.getCompletedAt() == null && order.getReview() == null,
                        "DATE/DATETIME2 và NULL của đơn pending");
                List<OrderDetail> details = order.getDetails();
                check(details.size() == 3 && details.stream().allMatch(d -> d.getOrderId() == pending
                        && d.getOrderDetailId() > 0), "Đọc đủ 3 chi tiết, không nhân bản do JOIN");
                OrderDetail switchLine = details.stream().filter(d -> d.getProductId() == switches.getProductId())
                        .findFirst().orElseThrow();
                check(switchLine.getQuantity() == 70
                        && switchLine.getUnitPrice().compareTo(new BigDecimal("5000")) == 0
                        && switchLine.getSubtotal().compareTo(new BigDecimal("350000")) == 0
                        && switchLine.getProductNameSnapshot().equals("Snapshot · " + switches.getProductName()),
                        "Chi tiết dùng tên/giá snapshot và subtotal BigDecimal");
                BigDecimal calculated = order.getServicePrice().add(details.stream().map(OrderDetail::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                check(calculated.compareTo(expected) == 0 && order.getTotalAmount().compareTo(expected) == 0,
                        "Tổng đơn fixture chính xác 2050000");
                Order done = find(mine, completed);
                check(done.getDetails().isEmpty() && done.getPreferredDate() == null && done.getCustomerNote() == null
                        && done.getAdminNote() == null && done.getTotalAmount().compareTo(service.getBasePrice()) == 0,
                        "Đơn 0 linh kiện và các cột nullable");
                check(LocalDateTime.of(2026, 10, 30, 10, 0).equals(done.getCompletedAt())
                        && done.getReview() != null && done.getReview().getReviewId() > 0
                        && done.getReview().getOrderId() == completed && done.getReview().getRating() == 5
                        && "Rất tốt <3 & cảm ơn".equals(done.getReview().getComment())
                        && done.getReview().getCreatedAt() != null
                        && customerA.getFullName().equals(done.getReview().getFullName()),
                        "Dùng đúng ServiceReview P2, fullName JOIN và completedAt");
                List<Order> all = dao.getAllOrders(null);
                check(all.size() == existingOrders + 4 && dao.getAllOrders("").size() == all.size()
                        && dao.getAllOrders(" ").size() == all.size(), "Admin list mọi đơn với null/rỗng");
                List<Integer> fixtureIds = List.of(pending, completed, processing, cancelled);
                check(all.stream().filter(o -> fixtureIds.contains(o.getOrderId())).map(Order::getOrderId).toList()
                        .equals(List.of(cancelled, processing, completed, pending)), "Cùng CreatedAt sắp OrderID DESC");
                for (String status : List.of("PENDING", "PROCESSING", "COMPLETED", "CANCELLED")) {
                    List<Order> filtered = dao.getAllOrders(status);
                    check(filtered.stream().allMatch(o -> status.equals(o.getOrderStatus()))
                            && filtered.stream().anyMatch(o -> fixtureIds.contains(o.getOrderId())), "Lọc " + status);
                }
                try {
                    dao.getAllOrders("PENDING' OR 1=1 --");
                    throw new AssertionError("Trạng thái sai phải bị từ chối");
                } catch (IllegalArgumentException expectedError) {
                    check(true, "Chặn status ngoài whitelist, không ghép SQL đầu vào");
                }
            } finally {
                dao.connection.rollback();
                dao.connection.setAutoCommit(true);
            }
            check(before.equals(dao.counts()), "Rollback giữ nguyên số dòng 8 bảng, không DELETE/đổi catalog");
        }
        VerifyP301 closed = new VerifyP301();
        closed.close();
        try {
            closed.getAllOrders(null);
            throw new AssertionError("DAO đã đóng phải báo SQLException");
        } catch (SQLException expectedError) {
            check(true, "Đóng connection và truyền SQLException, không trả list giả");
        }
        System.out.println("PASS " + checks + " checks; fixtures rolled back.");
    }

    private int fixtureOrder(Account owner, WorkshopService service, String status, BigDecimal total, boolean filled)
            throws SQLException {
        String sql = "INSERT INTO dbo.ServiceOrder (AccountID, ServiceID, ServiceNameSnapshot, ServicePrice, "
                + "ContactName, ContactPhone, DeviceDescription, PreferredDate, CustomerNote, OrderStatus, "
                + "AdminNote, TotalAmount, CreatedAt, CompletedAt) VALUES (?, ?, N'P3-01 · Công lắp ráp snapshot', ?, "
                + "N'Nguyễn Khách <&>', '0901234567', N'Bàn phím 75% — đồ riêng', ?, ?, ?, ?, ?, "
                + "'2026-10-30T09:00:00', ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, owner.getAccountId());
            statement.setInt(2, service.getServiceId());
            statement.setBigDecimal(3, service.getBasePrice());
            statement.setDate(4, filled ? java.sql.Date.valueOf("2026-11-05") : null);
            statement.setNString(5, filled ? "Giữ lại keycap cũ" : null);
            statement.setString(6, status);
            statement.setNString(7, filled ? "Chờ nhận bàn phím" : null);
            statement.setBigDecimal(8, total);
            statement.setTimestamp(9, "COMPLETED".equals(status)
                    ? java.sql.Timestamp.valueOf("2026-10-30 10:00:00") : null);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Fixture thiếu generated key");
                }
                return keys.getInt(1);
            }
        }
    }

    private void fixtureDetail(int orderId, Product product, int quantity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO dbo.OrderDetail "
                + "(OrderID, ProductID, ProductNameSnapshot, Quantity, UnitPrice) VALUES (?, ?, ?, ?, ?)")) {
            statement.setInt(1, orderId);
            statement.setInt(2, product.getProductId());
            statement.setNString(3, "Snapshot · " + product.getProductName());
            statement.setInt(4, quantity);
            statement.setBigDecimal(5, product.getPrice());
            statement.executeUpdate();
        }
    }

    private void fixtureReview(int orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO dbo.ServiceReview "
                + "(OrderID, Rating, Comment) VALUES (?, 5, N'Rất tốt <3 & cảm ơn')")) {
            statement.setInt(1, orderId);
            statement.executeUpdate();
        }
    }

    private String counts() throws SQLException {
        StringBuilder counts = new StringBuilder();
        for (String table : List.of("UserAccount", "Brand", "Category", "Product", "WorkshopService",
                "ServiceOrder", "OrderDetail", "ServiceReview")) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM dbo." + table);
                    ResultSet result = statement.executeQuery()) {
                result.next();
                counts.append(table).append('=').append(result.getInt(1)).append(';');
            }
        }
        return counts.toString();
    }

    private static Product product(List<Product> products, String name) {
        return products.stream().filter(p -> name.equals(p.getProductName())).findFirst().orElseThrow();
    }

    private static Order find(List<Order> orders, int id) {
        return orders.stream().filter(o -> o.getOrderId() == id).findFirst().orElseThrow();
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
        checks++;
        System.out.println("OK " + checks + " " + label);
    }
}
