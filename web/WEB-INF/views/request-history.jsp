<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xác nhận yêu cầu — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container profile-main">
    <p class="eyebrow">KEEBFORCE WORKSHOP</p>
    <h1>Yêu cầu dịch vụ của bạn</h1>
    <jsp:include page="/WEB-INF/views/common/messages.jsp" />
    <c:choose>
        <c:when test="${not empty createdOrder}">
            <section class="auth-panel profile-panel" aria-labelledby="receipt-title">
                <h2 id="receipt-title">Đã gửi yêu cầu #<c:out value="${createdOrder.orderId}" /></h2>
                <p>Workshop sẽ liên hệ với bạn để trao đổi và tiếp nhận bàn phím.</p>
                <dl class="profile-grid">
                    <div class="profile-field"><dt>Dịch vụ</dt><dd><c:out value="${createdOrder.serviceNameSnapshot}" /></dd></div>
                    <div class="profile-field"><dt>Tổng yêu cầu đã lưu</dt><dd><kf:money value="${createdOrder.totalAmount}" /></dd></div>
                    <div class="profile-field"><dt>Trạng thái khi gửi</dt><dd>Chờ tiếp nhận (PENDING)</dd></div>
                    <div class="profile-field"><dt>Liên hệ</dt><dd><c:out value="${createdOrder.contactName}" /> · <c:out value="${createdOrder.contactPhone}" /></dd></div>
                </dl>
            </section>
        </c:when>
        <c:otherwise><p>Chọn dịch vụ, mô tả bàn phím và gửi yêu cầu để workshop liên hệ với bạn.</p></c:otherwise>
    </c:choose>
    <a class="text-link" href="<c:url value='/services'/>">← Chọn dịch vụ</a>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
