<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<c:set var="page" value="services" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dịch vụ Workshop — KeebForge</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container admin-main">
    <div class="admin-heading">
        <div>
            <p class="eyebrow"><span aria-hidden="true">●</span> DỊCH VỤ WORKSHOP</p>
            <h1>Các gói dịch vụ bàn phím cơ</h1>
            <p class="panel-description">Mỗi gói dịch vụ có mức phí công cố định được công bố rõ ràng. Bạn có thể chọn thêm linh kiện của workshop khi gửi yêu cầu.</p>
        </div>
        <c:if test="${not empty services}">
            <span class="account-badge"><c:out value="${services.size()}" /> gói dịch vụ</span>
        </c:if>
    </div>

    <jsp:include page="/WEB-INF/views/common/messages.jsp" />

    <c:if test="${not empty services}">
        <div class="row g-4 catalog-grid">
            <c:forEach var="service" items="${services}" varStatus="status">
                <c:url var="requestUrl" value="/request">
                    <c:param name="serviceId" value="${service.serviceId}" />
                </c:url>
                <c:url var="reviewUrl" value="/services">
                    <c:param name="id" value="${service.serviceId}" />
                </c:url>
                <div class="col-md-6">
                    <article class="workshop-card ${not empty selectedService and selectedService.serviceId eq service.serviceId ? 'service-card-selected' : ''}">
                        <span class="card-number">0<c:out value="${status.count}" /> / GÓI DỊCH VỤ #<c:out value="${service.serviceId}" /></span>
                        <h2><c:out value="${service.serviceName}" /></h2>
                        <p><c:out value="${service.description}" /></p>
                        <div class="card-detail">
                            <span>Phí công cố định</span>
                            <span class="money"><kf:money value="${service.basePrice}" /></span>
                        </div>
                        <div class="service-card-actions">
                            <a class="btn btn-workshop" href="<c:out value='${requestUrl}'/>">Gửi yêu cầu dịch vụ</a>
                            <a class="text-link" href="<c:out value='${reviewUrl}'/>#reviews">Xem đánh giá</a>
                        </div>
                    </article>
                </div>
            </c:forEach>
        </div>
    </c:if>

    <c:if test="${empty services and empty requestScope.errorMessage}">
        <p class="notice">Hiện chưa có gói dịch vụ nào đang mở nhận yêu cầu.</p>
    </c:if>

    <c:if test="${not empty selectedService}">
        <section id="reviews" class="service-reviews-panel" aria-labelledby="reviews-title">
            <div class="profile-heading">
                <div>
                    <p class="eyebrow">ĐÁNH GIÁ KHÁCH HÀNG</p>
                    <h2 id="reviews-title"><c:out value="${selectedService.serviceName}" /></h2>
                </div>
                <span class="account-badge">
                    <c:out value="${empty reviews ? 0 : reviews.size()}" /> đánh giá
                </span>
            </div>

            <c:if test="${not empty reviews}">
                <div class="review-list">
                    <c:forEach var="review" items="${reviews}">
                        <article class="review-item">
                            <div class="review-header">
                                <strong><c:out value="${review.fullName}" /></strong>
                                <span class="review-rating" aria-label="Điểm đánh giá ${review.rating} trên 5 sao">
                                    <c:forEach begin="1" end="5" var="star">
                                        <span aria-hidden="true">${star <= review.rating ? '★' : '☆'}</span>
                                    </c:forEach>
                                    (<c:out value="${review.rating}" />/5)
                                </span>
                                <span class="user-detail">Đơn #<c:out value="${review.orderId}" /> · <c:out value="${review.createdAt.toLocalDate()}" /></span>
                            </div>
                            <p class="review-comment">
                                <c:out value="${empty review.comment ? 'Khách hàng không để lại bình luận.' : review.comment}" />
                            </p>
                        </article>
                    </c:forEach>
                </div>
            </c:if>

            <c:if test="${empty reviews}">
                <p class="notice">Chưa có đánh giá nào cho dịch vụ này. Đánh giá sẽ hiển thị sau khi khách hàng hoàn tất đơn dịch vụ.</p>
            </c:if>
        </section>
    </c:if>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
