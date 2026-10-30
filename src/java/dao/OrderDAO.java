package dao;

import dal.DBContext;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.Order;
import model.OrderDetail;
import model.ServiceReview;

/** JDBC đơn dịch vụ; mỗi request tạo/đóng DAO cục bộ. */
public class OrderDAO extends DBContext {

    private static final String COLUMNS = "OrderID, AccountID, ServiceID, ServiceNameSnapshot, "
            + "ServicePrice, ContactName, ContactPhone, DeviceDescription, PreferredDate, "
            + "CustomerNote, OrderStatus, AdminNote, TotalAmount, CreatedAt, CompletedAt";

    public OrderDAO() throws SQLException {
        super();
    }

    /** Sở hữu một transaction; accountId lấy từ session, mọi giá/tên lấy lại từ DB. */
    public int createOrder(Order input, List<OrderDetail> selectedParts, int accountId) throws SQLException {
        if (input == null || accountId <= 0 || input.getServiceId() <= 0 || selectedParts == null) {
            throw new IllegalArgumentException("Thông tin yêu cầu không hợp lệ.");
        }
        String contactName = checkedText(input.getContactName(), 100, true, "Tên liên hệ");
        String phone = checkedText(input.getContactPhone(), 20, true, "Số điện thoại");
        if (!phone.matches("[0-9+(). \\-]{7,20}")) {
            throw new IllegalArgumentException("Số điện thoại phải có 7–20 ký tự số hoặc dấu điện thoại hợp lệ.");
        }
        String device = checkedText(input.getDeviceDescription(), 1000, true, "Mô tả thiết bị");
        String note = checkedText(input.getCustomerNote(), 1000, false, "Ghi chú");
        LocalDate preferred = input.getPreferredDate();
        if (preferred != null && (preferred.isBefore(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                || preferred.getYear() > 9999)) {
            throw new IllegalArgumentException("Ngày mong muốn phải từ hôm nay và trong phạm vi năm 9999.");
        }
        Set<Integer> ids = new HashSet<>();
        for (OrderDetail part : selectedParts) {
            if (part == null || part.getProductId() <= 0 || part.getQuantity() < 1 || part.getQuantity() > 200
                    || !ids.add(part.getProductId())) {
                throw new IllegalArgumentException("Linh kiện không hợp lệ, bị trùng hoặc số lượng ngoài 1–200.");
            }
        }
        if (!connection.getAutoCommit()) {
            throw new SQLException("createOrder cần connection không có transaction đang mở.");
        }
        connection.setAutoCommit(false);
        try {
            // Giữ shared lock tới commit để không bị ngừng nhận/đổi giá giữa kiểm tra và insert.
            try (PreparedStatement statement = connection.prepareStatement("SELECT AccountID FROM dbo.UserAccount "
                    + "WITH (HOLDLOCK) WHERE AccountID = ? AND Role = 'CUSTOMER' AND IsActive = 1")) {
                statement.setInt(1, accountId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("Tài khoản không còn quyền gửi yêu cầu dịch vụ.");
                    }
                }
            }
            String serviceName;
            BigDecimal servicePrice;
            try (PreparedStatement statement = connection.prepareStatement("SELECT ServiceName, BasePrice "
                    + "FROM dbo.WorkshopService WITH (HOLDLOCK) WHERE ServiceID = ? AND IsActive = 1")) {
                statement.setInt(1, input.getServiceId());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("Dịch vụ không tồn tại hoặc đã ngừng nhận yêu cầu.");
                    }
                    serviceName = result.getNString("ServiceName");
                    servicePrice = result.getBigDecimal("BasePrice");
                }
            }
            BigDecimal total = servicePrice;
            List<OrderDetail> details = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT ProductName, Price "
                    + "FROM dbo.Product WITH (HOLDLOCK) WHERE ProductID = ? AND IsActive = 1")) {
                for (OrderDetail selected : selectedParts) {
                    statement.setInt(1, selected.getProductId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("Một linh kiện đã ngừng nhận hoặc không tồn tại. Vui lòng chọn lại.");
                        }
                        OrderDetail detail = new OrderDetail();
                        detail.setProductId(selected.getProductId());
                        detail.setQuantity(selected.getQuantity());
                        detail.setProductNameSnapshot(result.getNString("ProductName"));
                        detail.setUnitPrice(result.getBigDecimal("Price"));
                        total = total.add(detail.getSubtotal());
                        details.add(detail);
                    }
                }
            }
            if (total.compareTo(new BigDecimal("999999999999999999")) > 0) {
                throw new IllegalArgumentException("Tổng yêu cầu vượt giới hạn lưu trữ. Vui lòng giảm số lượng linh kiện.");
            }
            int orderId;
            String sql = "INSERT INTO dbo.ServiceOrder (AccountID, ServiceID, ServiceNameSnapshot, ServicePrice, "
                    + "ContactName, ContactPhone, DeviceDescription, PreferredDate, CustomerNote, OrderStatus, TotalAmount) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, accountId);
                statement.setInt(2, input.getServiceId());
                statement.setNString(3, serviceName);
                statement.setBigDecimal(4, servicePrice);
                statement.setNString(5, contactName);
                statement.setString(6, phone);
                statement.setNString(7, device);
                statement.setDate(8, preferred == null ? null : Date.valueOf(preferred));
                statement.setNString(9, note);
                statement.setBigDecimal(10, total);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Không lưu được đơn dịch vụ.");
                }
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Không nhận được mã đơn dịch vụ.");
                    }
                    orderId = keys.getInt(1);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO dbo.OrderDetail "
                    + "(OrderID, ProductID, ProductNameSnapshot, Quantity, UnitPrice) VALUES (?, ?, ?, ?, ?)")) {
                for (OrderDetail detail : details) {
                    statement.setInt(1, orderId);
                    statement.setInt(2, detail.getProductId());
                    statement.setNString(3, detail.getProductNameSnapshot());
                    statement.setInt(4, detail.getQuantity());
                    statement.setBigDecimal(5, detail.getUnitPrice());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Không lưu được chi tiết linh kiện.");
                    }
                    detail.setOrderId(orderId);
                }
            }
            connection.commit();
            // Chỉ trả receipt sau commit. Không nhận account/status/snapshot/tổng từ input.
            input.setOrderId(orderId);
            input.setAccountId(accountId);
            input.setServiceNameSnapshot(serviceName);
            input.setServicePrice(servicePrice);
            input.setTotalAmount(total);
            input.setOrderStatus("PENDING");
            input.setContactName(contactName);
            input.setContactPhone(phone);
            input.setDeviceDescription(device);
            input.setCustomerNote(note);
            input.setAdminNote(null);
            input.setCompletedAt(null);
            input.setDetails(details);
            input.setReview(null);
            return orderId;
        } catch (SQLException | RuntimeException ex) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                ex.addSuppressed(rollbackError);
                // Không bật autoCommit khi rollback thất bại, tránh commit giao dịch còn sót.
                try {
                    connection.close();
                } catch (SQLException closeError) {
                    ex.addSuppressed(closeError);
                }
            }
            throw ex;
        } finally {
            if (!connection.isClosed()) {
                connection.setAutoCommit(true);
            }
        }
    }

    private String checkedText(String value, int max, boolean required, String label) {
        String text = value == null ? "" : value.strip();
        if ((required && text.isBlank()) || text.length() > max) {
            throw new IllegalArgumentException(label + " " + (required ? "bắt buộc và " : "")
                    + "không được quá " + max + " ký tự.");
        }
        return text.isEmpty() ? null : text;
    }

    /** Hủy đúng owner và trạng thái trong một UPDATE; không xóa đơn hay chi tiết. */
    public boolean cancelPending(int orderId, int accountId) throws SQLException {
        String sql = "UPDATE dbo.ServiceOrder SET OrderStatus = 'CANCELLED' "
                + "WHERE OrderID = ? AND AccountID = ? AND OrderStatus = 'PENDING' "
                + "AND EXISTS (SELECT 1 FROM dbo.UserAccount WHERE AccountID = ? "
                + "AND Role = 'CUSTOMER' AND IsActive = 1)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.setInt(2, accountId);
            statement.setInt(3, accountId);
            return statement.executeUpdate() == 1;
        }
    }

    /** accountId phải lấy từ session.user, không lấy từ tham số trình duyệt. */
    public List<Order> getOrdersByAccount(int accountId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM dbo.ServiceOrder WHERE AccountID = ? "
                + "ORDER BY CreatedAt DESC, OrderID DESC";
        List<Order> orders;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
            orders = readOrders(statement);
        }
        loadChildren(orders);
        return orders;
    }

    /** Chỉ dùng sau kiểm tra ADMIN ở Filter/Servlet. null/rỗng nghĩa là mọi trạng thái. */
    public List<Order> getAllOrders(String status) throws SQLException {
        boolean filtered = status != null && !status.isBlank();
        if (filtered && !List.of("PENDING", "PROCESSING", "COMPLETED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Trạng thái đơn không hợp lệ.");
        }
        String sql = "SELECT " + COLUMNS + " FROM dbo.ServiceOrder"
                + (filtered ? " WHERE OrderStatus = ?" : "") + " ORDER BY CreatedAt DESC, OrderID DESC";
        List<Order> orders;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (filtered) {
                statement.setString(1, status);
            }
            orders = readOrders(statement);
        }
        loadChildren(orders);
        return orders;
    }

    private List<Order> readOrders(PreparedStatement statement) throws SQLException {
        List<Order> orders = new ArrayList<>();
        try (ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Order order = new Order();
                order.setOrderId(result.getInt("OrderID"));
                order.setAccountId(result.getInt("AccountID"));
                order.setServiceId(result.getInt("ServiceID"));
                order.setServiceNameSnapshot(result.getNString("ServiceNameSnapshot"));
                order.setServicePrice(result.getBigDecimal("ServicePrice"));
                order.setContactName(result.getNString("ContactName"));
                order.setContactPhone(result.getString("ContactPhone"));
                order.setDeviceDescription(result.getNString("DeviceDescription"));
                Date preferredDate = result.getDate("PreferredDate");
                order.setPreferredDate(preferredDate == null ? null : preferredDate.toLocalDate());
                order.setCustomerNote(result.getNString("CustomerNote"));
                order.setOrderStatus(result.getString("OrderStatus"));
                order.setAdminNote(result.getNString("AdminNote"));
                order.setTotalAmount(result.getBigDecimal("TotalAmount"));
                order.setCreatedAt(result.getTimestamp("CreatedAt").toLocalDateTime());
                Timestamp completedAt = result.getTimestamp("CompletedAt");
                order.setCompletedAt(completedAt == null ? null : completedAt.toLocalDateTime());
                orders.add(order);
            }
        }
        return orders;
    }

    private void loadChildren(List<Order> orders) throws SQLException {
        // Đóng ResultSet cha trước khi đọc con; không cần bật MARS của SQL Server.
        for (Order order : orders) {
            order.setDetails(readDetails(order.getOrderId()));
            order.setReview(readReview(order.getOrderId()));
        }
    }

    private List<OrderDetail> readDetails(int orderId) throws SQLException {
        String sql = "SELECT OrderDetailID, OrderID, ProductID, ProductNameSnapshot, Quantity, UnitPrice "
                + "FROM dbo.OrderDetail WHERE OrderID = ? ORDER BY OrderDetailID";
        List<OrderDetail> details = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    OrderDetail detail = new OrderDetail();
                    detail.setOrderDetailId(result.getInt("OrderDetailID"));
                    detail.setOrderId(result.getInt("OrderID"));
                    detail.setProductId(result.getInt("ProductID"));
                    detail.setProductNameSnapshot(result.getNString("ProductNameSnapshot"));
                    detail.setQuantity(result.getInt("Quantity"));
                    detail.setUnitPrice(result.getBigDecimal("UnitPrice"));
                    details.add(detail);
                }
            }
        }
        return details;
    }

    private ServiceReview readReview(int orderId) throws SQLException {
        String sql = "SELECT r.ReviewID, r.OrderID, r.Rating, r.Comment, r.CreatedAt, u.FullName "
                + "FROM dbo.ServiceReview AS r JOIN dbo.ServiceOrder AS o ON o.OrderID = r.OrderID "
                + "JOIN dbo.UserAccount AS u ON u.AccountID = o.AccountID WHERE r.OrderID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                ServiceReview review = new ServiceReview();
                review.setReviewId(result.getInt("ReviewID"));
                review.setOrderId(result.getInt("OrderID"));
                review.setRating(result.getInt("Rating"));
                review.setComment(result.getNString("Comment"));
                review.setCreatedAt(result.getTimestamp("CreatedAt").toLocalDateTime());
                review.setFullName(result.getNString("FullName"));
                return review;
            }
        }
    }
}
