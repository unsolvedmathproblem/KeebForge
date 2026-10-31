<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tổng quan workshop — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
    <link rel="stylesheet" href="<c:url value='/css/orders.css'/>">
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container admin-main">
    <div class="admin-heading">
        <div><p class="eyebrow">QUẢN TRỊ KEEBFORCE</p><h1>Tổng quan workshop</h1>
            <p class="panel-description">Theo dõi số yêu cầu và tổng giá trị các đơn đã hoàn tất.</p></div>
        <a class="btn btn-workshop" href="<c:url value='/admin/orders'/>">Quản lý đơn</a>
    </div>
    <jsp:include page="/WEB-INF/views/common/messages.jsp" />
    <c:if test="${not empty statistics}">
        <div class="order-stats-grid">
            <section class="auth-panel order-stat" aria-labelledby="stat-total"><h2 id="stat-total">Tổng đơn</h2><p class="stat-value"><c:out value="${statistics.totalOrders}" /></p><a class="text-link" href="<c:url value='/admin/orders'/>">Xem tất cả đơn</a></section>
            <section class="auth-panel order-stat" aria-labelledby="stat-value"><h2 id="stat-value">Tổng giá trị đơn hoàn tất</h2><p class="stat-value"><kf:money value="${statistics.completedValue}" /></p><p>Tính theo tổng giá trị đã lưu của các đơn COMPLETED.</p></section>
            <section class="auth-panel order-stat" aria-labelledby="stat-pending"><h2 id="stat-pending">Chờ tiếp nhận</h2><p class="stat-value"><c:out value="${statistics.PENDING}" /></p><a class="text-link" href="<c:url value='/admin/orders'><c:param name='status' value='PENDING'/></c:url>">Xem đơn chờ</a></section>
            <section class="auth-panel order-stat" aria-labelledby="stat-processing"><h2 id="stat-processing">Đang xử lý</h2><p class="stat-value"><c:out value="${statistics.PROCESSING}" /></p><a class="text-link" href="<c:url value='/admin/orders'><c:param name='status' value='PROCESSING'/></c:url>">Xem đơn đang làm</a></section>
            <section class="auth-panel order-stat" aria-labelledby="stat-completed"><h2 id="stat-completed">Đã hoàn tất</h2><p class="stat-value"><c:out value="${statistics.COMPLETED}" /></p><a class="text-link" href="<c:url value='/admin/orders'><c:param name='status' value='COMPLETED'/></c:url>">Xem đơn hoàn tất</a></section>
            <section class="auth-panel order-stat" aria-labelledby="stat-cancelled"><h2 id="stat-cancelled">Đã hủy</h2><p class="stat-value"><c:out value="${statistics.CANCELLED}" /></p><a class="text-link" href="<c:url value='/admin/orders'><c:param name='status' value='CANCELLED'/></c:url>">Xem đơn đã hủy</a></section>
        </div>
    </c:if>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
