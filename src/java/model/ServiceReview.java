package model;

import java.time.LocalDateTime;

/**
 * JavaBean tương ứng bảng ServiceReview. P2 đọc qua ServiceDAO.getReviews,
 * P3 ghi qua OrderDAO.insertReview; dùng chung một Model này.
 * fullName là UserAccount.FullName lấy bằng JOIN qua ServiceOrder để hiển thị,
 * không phải cột của bảng ServiceReview.
 */
public class ServiceReview {

    private int reviewId;
    private int orderId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private String fullName;

    public ServiceReview() {
    }

    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
