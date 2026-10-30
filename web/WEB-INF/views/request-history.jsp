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
            </article>
        </c:forEach>
    </c:if>
    <a class="text-link" href="<c:url value='/services'/>">← Chọn dịch vụ</a>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
