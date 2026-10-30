<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lịch sử yêu cầu — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
    <link rel="stylesheet" href="<c:url value='/css/orders.css'/>">
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container profile-main order-history-main">
    <p class="eyebrow">KEEBFORCE WORKSHOP</p>
    <h1>Lịch sử yêu cầu</h1>
    <p class="panel-description">Theo dõi yêu cầu đã gửi và ghi chú từ workshop. Bạn có thể hủy khi đơn còn chờ tiếp nhận.</p>
    <jsp:include page="/WEB-INF/views/common/messages.jsp" />
    <c:if test="${historyLoaded}">
        <c:if test="${empty orders}">
            <section class="auth-panel profile-panel" aria-labelledby="empty-orders-title">
                <h2 id="empty-orders-title">Bạn chưa có yêu cầu nào</h2>
                <p>Chọn dịch vụ, mô tả bàn phím và gửi yêu cầu để workshop liên hệ với bạn.</p>
            </section>
        </c:if>
        <c:forEach var="order" items="${orders}">
            <article class="auth-panel order-card" id="order-${order.orderId}" aria-labelledby="order-title-${order.orderId}">
                <div class="order-heading">
                    <div>
                        <h2 id="order-title-${order.orderId}">Yêu cầu #<c:out value="${order.orderId}" /></h2>
                        <p class="order-service-name"><c:out value="${order.serviceNameSnapshot}" /></p>
                    </div>
                    <span class="status-badge order-status-${order.orderStatus}">
                        <c:choose>
                            <c:when test="${order.orderStatus eq 'PENDING'}">Chờ tiếp nhận</c:when>
                            <c:when test="${order.orderStatus eq 'PROCESSING'}">Đang xử lý</c:when>
                            <c:when test="${order.orderStatus eq 'COMPLETED'}">Đã hoàn tất</c:when>
                            <c:otherwise>Đã hủy</c:otherwise>
                        </c:choose>
                    </span>
                </div>
                <dl class="profile-grid order-metadata">
                    <div class="profile-field"><dt>Ngày gửi</dt><dd><c:out value="${order.createdAt.format(dateTimeFormat)}" /></dd></div>
                    <div class="profile-field"><dt>Ngày mong muốn</dt><dd><c:out value="${empty order.preferredDate ? 'Chưa chọn' : order.preferredDate.format(dateFormat)}" /></dd></div>
                    <div class="profile-field"><dt>Liên hệ</dt><dd><c:out value="${order.contactName}" /> · <c:out value="${order.contactPhone}" /></dd></div>
                    <div class="profile-field"><dt>Tiền công đã lưu</dt><dd><kf:money value="${order.servicePrice}" /></dd></div>
                    <c:if test="${not empty order.completedAt}">
                        <div class="profile-field"><dt>Ngày hoàn tất</dt><dd><c:out value="${order.completedAt.format(dateTimeFormat)}" /></dd></div>
                    </c:if>
                </dl>
                <div class="order-text-block">
                    <h3>Thiết bị và nhu cầu</h3>
                    <p><c:out value="${order.deviceDescription}" /></p>
                </div>
                <c:if test="${not empty order.customerNote}">
                    <div class="order-text-block"><h3>Ghi chú của bạn</h3><p><c:out value="${order.customerNote}" /></p></div>
                </c:if>
                <div class="order-text-block">
                    <h3>Ghi chú từ workshop</h3>
                    <p><c:out value="${empty order.adminNote ? 'Workshop chưa có ghi chú.' : order.adminNote}" /></p>
                </div>
                <h3 id="order-parts-title-${order.orderId}">Linh kiện trong yêu cầu</h3>
                <c:choose>
                    <c:when test="${empty order.details}"><p>Không chọn thêm linh kiện của workshop.</p></c:when>
                    <c:otherwise>
                        <div class="table-responsive data-table-wrap" tabindex="0" role="region" aria-labelledby="order-parts-title-${order.orderId}">
                            <table class="table data-table order-parts">
                                <caption class="visually-hidden">Linh kiện và giá đã lưu của yêu cầu #<c:out value="${order.orderId}" /></caption>
                                <thead><tr><th scope="col">Linh kiện</th><th scope="col">Số lượng</th><th scope="col">Đơn giá</th><th scope="col">Thành tiền</th></tr></thead>
                                <tbody>
                                <c:forEach var="detail" items="${order.details}">
                                    <tr>
                                        <td><c:out value="${detail.productNameSnapshot}" /></td>
                                        <td><c:out value="${detail.quantity}" /></td>
                                        <td class="money"><kf:money value="${detail.unitPrice}" /></td>
                                        <td class="money"><kf:money value="${detail.subtotal}" /></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
                <div class="order-summary">
                    <p>Tổng yêu cầu đã lưu <strong class="money"><kf:money value="${order.totalAmount}" /></strong></p>
                    <c:if test="${order.orderStatus eq 'PENDING'}">
                        <form action="<c:url value='/orders'/>" method="post">
                            <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                            <input type="hidden" name="action" value="cancel">
                            <input type="hidden" name="orderId" value="<c:out value='${order.orderId}'/>">
                            <button class="btn btn-outline-secondary" type="submit">Hủy yêu cầu #<c:out value="${order.orderId}" /></button>
                        </form>
                    </c:if>
                </div>
                <c:if test="${order.orderStatus eq 'COMPLETED' or not empty order.review}">
                    <section class="order-review" aria-labelledby="review-title-${order.orderId}">
                        <h3 id="review-title-${order.orderId}">Đánh giá dịch vụ</h3>
                        <c:choose>
                            <c:when test="${not empty order.review}">
                                <p class="review-rating" aria-label="Đánh giá ${order.review.rating} trên 5 sao">
                                    <span aria-hidden="true"><c:forEach begin="1" end="5" var="star">${star le order.review.rating ? '★' : '☆'}</c:forEach></span>
                                    (<c:out value="${order.review.rating}" />/5)
                                </p>
                                <p class="review-comment"><c:out value="${empty order.review.comment ? 'Bạn không để lại bình luận.' : order.review.comment}" /></p>
                                <p class="user-detail">Đã gửi lúc <c:out value="${order.review.createdAt.format(dateTimeFormat)}" /></p>
                            </c:when>
                            <c:otherwise>
                                <p>Chia sẻ trải nghiệm sau khi hoàn tất dịch vụ. Mỗi yêu cầu được đánh giá một lần.</p>
                                <form action="<c:url value='/orders'/>" method="post">
                                    <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                    <input type="hidden" name="action" value="review">
                                    <input type="hidden" name="orderId" value="<c:out value='${order.orderId}'/>">
                                    <div class="form-field order-review-rating">
                                        <label for="rating-${order.orderId}">Điểm đánh giá — #<c:out value="${order.orderId}" /></label>
                                        <select class="form-control" id="rating-${order.orderId}" name="rating" required>
                                            <option value="">Chọn số sao</option>
                                            <c:forEach begin="1" end="5" var="score"><option value="${score}" ${reviewOrderId eq order.orderId and ratingInput eq score.toString() ? 'selected' : ''}>${score} sao</option></c:forEach>
                                        </select>
                                    </div>
                                    <div class="form-field">
                                        <label for="comment-${order.orderId}">Bình luận <span class="optional-label">(tùy chọn)</span></label>
                                        <textarea class="form-control" id="comment-${order.orderId}" name="comment" rows="3" maxlength="500" aria-describedby="comment-help-${order.orderId}"><c:out value="${reviewOrderId eq order.orderId ? commentInput : ''}" /></textarea>
                                        <small id="comment-help-${order.orderId}">Tối đa 500 ký tự. Nhận xét sẽ hiển thị cùng tên của bạn trên trang dịch vụ.</small>
                                    </div>
                                    <button class="btn btn-workshop" type="submit">Gửi đánh giá</button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                        <c:url var="serviceReviewsUrl" value="/services"><c:param name="id" value="${order.serviceId}" /></c:url>
                        <p class="order-review-link"><a class="text-link" href="<c:out value='${serviceReviewsUrl}'/>#reviews">Xem đánh giá trên trang dịch vụ</a></p>
                    </section>
                </c:if>
            </article>
        </c:forEach>
    </c:if>
    <a class="text-link" href="<c:url value='/services'/>">← Chọn dịch vụ</a>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
