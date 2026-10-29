package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.ServiceReview;
import model.WorkshopService;

/**
 * DAO truy vấn gói dịch vụ workshop và đánh giá của khách hàng.
 * Đây là dữ liệu dịch vụ của cửa hàng, không phải tầng service kiến trúc.
 */
public class ServiceDAO extends DBContext {

    private static final String COLUMNS = "ServiceID, ServiceName, Description, BasePrice, IsActive";

    public ServiceDAO() throws SQLException {
        super();
    }

    public List<WorkshopService> getActiveServices() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM dbo.WorkshopService WHERE IsActive = 1 ORDER BY ServiceID ASC";
        List<WorkshopService> services = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                services.add(mapService(result));
            }
        }
        return services;
    }

    public WorkshopService getById(int serviceId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM dbo.WorkshopService WHERE ServiceID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, serviceId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapService(result) : null;
            }
        }
    }

    public List<ServiceReview> getReviews(int serviceId) throws SQLException {
        String sql = "SELECT r.ReviewID, r.OrderID, r.Rating, r.Comment, r.CreatedAt, u.FullName "
                + "FROM dbo.ServiceReview AS r "
                + "JOIN dbo.ServiceOrder AS o ON o.OrderID = r.OrderID "
                + "JOIN dbo.UserAccount AS u ON u.AccountID = o.AccountID "
                + "WHERE o.ServiceID = ? "
                + "ORDER BY r.CreatedAt DESC, r.ReviewID DESC";
        List<ServiceReview> reviews = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, serviceId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ServiceReview review = new ServiceReview();
                    review.setReviewId(result.getInt("ReviewID"));
                    review.setOrderId(result.getInt("OrderID"));
                    review.setRating(result.getInt("Rating"));
                    review.setComment(result.getNString("Comment"));
                    review.setCreatedAt(result.getTimestamp("CreatedAt").toLocalDateTime());
                    review.setFullName(result.getNString("FullName"));
                    reviews.add(review);
                }
            }
        }
        return reviews;
    }

    private WorkshopService mapService(ResultSet result) throws SQLException {
        WorkshopService service = new WorkshopService();
        service.setServiceId(result.getInt("ServiceID"));
        service.setServiceName(result.getNString("ServiceName"));
        service.setDescription(result.getNString("Description"));
        service.setBasePrice(result.getBigDecimal("BasePrice"));
        service.setActive(result.getBoolean("IsActive"));
        return service;
    }
}
