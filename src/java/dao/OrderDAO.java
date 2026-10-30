package dao;

import dal.DBContext;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Order;
import model.OrderDetail;
import model.ServiceReview;

/** P3-01: chỉ đọc đơn và snapshot; mỗi request tạo/đóng DAO cục bộ. */
public class OrderDAO extends DBContext {

    private static final String COLUMNS = "OrderID, AccountID, ServiceID, ServiceNameSnapshot, "
            + "ServicePrice, ContactName, ContactPhone, DeviceDescription, PreferredDate, "
            + "CustomerNote, OrderStatus, AdminNote, TotalAmount, CreatedAt, CompletedAt";

    public OrderDAO() throws SQLException {
        super();
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
