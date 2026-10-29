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
            <p class="panel-description">Mỗi gói dịch vụ có mức phí công cố định được công bố rõ ràng. Bạn có thể tham khảo và chọn thêm linh kiện của workshop khi gửi yêu cầu.</p>
        </div>
        <div class="admin-heading-actions">
            <c:if test="${not empty services}">
                <span class="account-badge"><c:out value="${services.size()}" /> gói dịch vụ</span>
            </c:if>
            <a class="btn catalog-reset-link" href="<c:url value='/parts'/>">Xem linh kiện tham khảo</a>
        </div>
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
                        <span class="card-number"><c:out value="${status.count < 10 ? '0' : ''}${status.count}" /> / GÓI DỊCH VỤ #<c:out value="${service.serviceId}" /></span>
                        <h2><c:out value="${service.serviceName}" /></h2>
                        <p><c:out value="${service.description}" /></p>
                        <div class="card-detail">
                            <span>Phí công cố định</span>
                            <span class="money"><kf:money value="${service.basePrice}" /></span>
                        </div>
                        <div class="service-card-actions">
                            <a class="btn btn-workshop" href="<c:out value='${requestUrl}'/>">Gửi yêu cầu dịch vụ</a>
                            <a class="text-link" href="<c:out value='${reviewUrl}'/>#reviews">
                                <c:out value="${not empty selectedService and selectedService.serviceId eq service.serviceId ? 'Đang xem đánh giá ↓' : 'Xem đánh giá'}" />
                            </a>
                        </div>
                    </article>
                </div>
            </c:forEach>
        </div>

        <section id="reviews" class="service-reviews-panel" aria-labelledby="reviews-title">
            <div class="profile-heading">
                <div>
                    <p class="eyebrow">ĐÁNH GIÁ KHÁCH HÀNG</p>
                    <h2 id="reviews-title">
                        <c:choose>
                            <c:when test="${not empty selectedService}">
                                <c:out value="${selectedService.serviceName}" />
                            </c:when>
                            <c:otherwise>Nhận xét từ đơn dịch vụ đã hoàn tất</c:otherwise>
                        </c:choose>
                    </h2>
                </div>
                <c:if test="${not empty selectedService}">
                    <div class="review-summary-badges">
                        <c:if test="${not empty averageRating}">
                            <span class="account-badge review-average-badge">★ <c:out value="${averageRating}" /> / 5</span>
                        </c:if>
                        <span class="account-badge">
                            <c:out value="${empty reviews ? 0 : reviews.size()}" /> đánh giá
                        </span>
                    </div>
                </c:if>
            </div>

            <nav class="review-service-tabs" aria-label="Chọn gói dịch vụ để xem đánh giá">
                <c:forEach var="s" items="${services}">
                    <c:url var="tabUrl" value="/services">
                        <c:param name="id" value="${s.serviceId}" />
                    </c:url>
                    <a class="review-tab-pill ${not empty selectedService and selectedService.serviceId eq s.serviceId ? 'review-tab-active' : ''}"
                       href="<c:out value='${tabUrl}'/>#reviews"
                       aria-current="${not empty selectedService and selectedService.serviceId eq s.serviceId ? 'true' : 'false'}">
                        <c:out value="${s.serviceName}" />
                    </a>
                </c:forEach>
                <c:if test="${not empty selectedService}">
                    <a class="review-tab-pill review-tab-clear" href="<c:url value='/services'/>#reviews">Thu gọn</a>
                </c:if>
            </nav>

            <c:choose>
                <c:when test="${not empty selectedService}">
                    <c:if test="${not empty reviews}">
                        <div class="review-list">
                            <c:forEach var="review" items="${reviews}">
                                <article class="review-item">
                                    <div class="review-header">
                                        <strong><c:out value="${review.fullName}" /></strong>
                                        <span class="review-rating" aria-label="Điểm đánh giá <c:out value='${review.rating}'/> trên 5 sao">
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
                        <p class="notice" role="status">Chưa có đánh giá nào cho dịch vụ này. Đánh giá sẽ hiển thị sau khi khách hàng hoàn tất đơn dịch vụ.</p>
                    </c:if>
                </c:when>
                <c:otherwise>
                    <p class="notice" role="status">Chọn một gói dịch vụ bên trên để xem đánh giá thực tế từ khách hàng đã hoàn tất đơn tại workshop.</p>
                </c:otherwise>
            </c:choose>
        </section>
    </c:if>

    <c:if test="${empty services and empty requestScope.errorMessage}">
        <p class="notice" role="status">Hiện chưa có gói dịch vụ nào đang mở nhận yêu cầu.</p>
    </c:if>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
