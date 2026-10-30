<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý đơn — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
    <link rel="stylesheet" href="<c:url value='/css/orders.css'/>">
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container admin-main">
    <div class="admin-heading">
        <div><p class="eyebrow">QUẢN TRỊ KEEBFORCE</p><h1>Quản lý đơn</h1>
            <p class="panel-description">Tiếp nhận yêu cầu, cập nhật tiến độ và ghi chú cho khách.</p></div>
        <a class="btn btn-outline-secondary" href="<c:url value='/admin/dashboard'/>">Xem thống kê</a>
    </div>
    <jsp:include page="/WEB-INF/views/common/messages.jsp" />
    <form action="<c:url value='/admin/orders'/>" method="get" class="order-filter">
        <div class="form-field">
            <label for="status-filter">Trạng thái đơn</label>
            <select class="form-control" id="status-filter" name="status">
                <option value="">Tất cả trạng thái</option>
                <option value="PENDING" ${statusFilter eq 'PENDING' ? 'selected' : ''}>Chờ tiếp nhận</option>
                <option value="PROCESSING" ${statusFilter eq 'PROCESSING' ? 'selected' : ''}>Đang xử lý</option>
                <option value="COMPLETED" ${statusFilter eq 'COMPLETED' ? 'selected' : ''}>Đã hoàn tất</option>
                <option value="CANCELLED" ${statusFilter eq 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
            </select>
        </div>
        <button class="btn btn-workshop" type="submit">Lọc đơn</button>
    </form>
    <c:if test="${ordersLoaded}">
        <c:if test="${empty orders}"><p class="notice">Không có yêu cầu nào phù hợp với bộ lọc.</p></c:if>
        <c:forEach var="order" items="${orders}">
            <article class="auth-panel order-card" id="order-${order.orderId}" aria-labelledby="order-title-${order.orderId}">
                <div class="order-heading">
                    <div><h2 id="order-title-${order.orderId}">Yêu cầu #<c:out value="${order.orderId}" /></h2>
                        <p class="order-service-name"><c:out value="${order.serviceNameSnapshot}" /></p></div>
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
                    <div class="profile-field"><dt>Tài khoản khách</dt><dd>#<c:out value="${order.accountId}" /></dd></div>
                    <div class="profile-field"><dt>Liên hệ</dt><dd><c:out value="${order.contactName}" /> · <c:out value="${order.contactPhone}" /></dd></div>
                    <div class="profile-field"><dt>Ngày gửi</dt><dd><c:out value="${order.createdAt.format(dateTimeFormat)}" /></dd></div>
                    <div class="profile-field"><dt>Ngày mong muốn</dt><dd><c:out value="${empty order.preferredDate ? 'Chưa chọn' : order.preferredDate.format(dateFormat)}" /></dd></div>
                    <c:if test="${not empty order.completedAt}"><div class="profile-field"><dt>Ngày hoàn tất</dt><dd><c:out value="${order.completedAt.format(dateTimeFormat)}" /></dd></div></c:if>
                    <div class="profile-field"><dt>Tiền công đã lưu</dt><dd><kf:money value="${order.servicePrice}" /></dd></div>
                </dl>
                <div class="order-text-block"><h3>Thiết bị và nhu cầu</h3><p><c:out value="${order.deviceDescription}" /></p></div>
                <c:if test="${not empty order.customerNote}"><div class="order-text-block"><h3>Ghi chú của khách</h3><p><c:out value="${order.customerNote}" /></p></div></c:if>
                <h3 id="order-parts-${order.orderId}">Linh kiện trong yêu cầu</h3>
                <c:choose>
                    <c:when test="${empty order.details}"><p>Không chọn thêm linh kiện của workshop.</p></c:when>
                    <c:otherwise>
                        <div class="table-responsive data-table-wrap" tabindex="0" role="region" aria-labelledby="order-parts-${order.orderId}">
                            <table class="table data-table order-parts">
                                <caption class="visually-hidden">Chi tiết yêu cầu #<c:out value="${order.orderId}" /></caption>
                                <thead><tr><th scope="col">Linh kiện</th><th scope="col">Số lượng</th><th scope="col">Đơn giá</th><th scope="col">Thành tiền</th></tr></thead>
                                <tbody><c:forEach var="detail" items="${order.details}"><tr>
                                    <td><c:out value="${detail.productNameSnapshot}" /></td><td><c:out value="${detail.quantity}" /></td>
                                    <td class="money"><kf:money value="${detail.unitPrice}" /></td><td class="money"><kf:money value="${detail.subtotal}" /></td>
                                </tr></c:forEach></tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
                <div class="order-summary"><p>Tổng yêu cầu đã lưu <strong class="money"><kf:money value="${order.totalAmount}" /></strong></p></div>
                <div class="order-admin-actions">
                    <section aria-labelledby="status-title-${order.orderId}">
                        <h3 id="status-title-${order.orderId}">Trạng thái xử lý</h3>
                        <c:choose>
                            <c:when test="${order.orderStatus eq 'PENDING' or order.orderStatus eq 'PROCESSING'}">
                                <form action="<c:url value='/admin/orders'/>" method="post">
                                    <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                    <input type="hidden" name="action" value="status">
                                    <input type="hidden" name="orderId" value="<c:out value='${order.orderId}'/>">
                                    <input type="hidden" name="status" value="<c:out value='${statusFilter}'/>">
                                    <div class="form-field">
                                        <label for="new-status-${order.orderId}">Trạng thái tiếp theo của #<c:out value="${order.orderId}" /></label>
                                        <select class="form-control" id="new-status-${order.orderId}" name="newStatus" required>
                                            <c:choose>
                                                <c:when test="${order.orderStatus eq 'PENDING'}"><option value="PROCESSING">Tiếp nhận — Đang xử lý</option><option value="CANCELLED">Từ chối — Hủy yêu cầu</option></c:when>
                                                <c:otherwise><option value="COMPLETED">Đã hoàn tất</option></c:otherwise>
                                            </c:choose>
                                        </select>
                                    </div>
                                    <button class="btn btn-workshop" type="submit">Cập nhật trạng thái</button>
                                </form>
                            </c:when>
                            <c:otherwise><p>Đơn đã kết thúc, không thể chuyển trạng thái.</p></c:otherwise>
                        </c:choose>
                    </section>
                    <form action="<c:url value='/admin/orders'/>" method="post">
                        <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                        <input type="hidden" name="action" value="note">
                        <input type="hidden" name="orderId" value="<c:out value='${order.orderId}'/>">
                        <input type="hidden" name="status" value="<c:out value='${statusFilter}'/>">
                        <div class="form-field">
                            <label for="admin-note-${order.orderId}">Ghi chú cho khách — #<c:out value="${order.orderId}" /></label>
                            <textarea class="form-control admin-textarea" id="admin-note-${order.orderId}" name="adminNote" rows="3" maxlength="1000" aria-describedby="note-help-${order.orderId}"><c:out value="${noteOrderId eq order.orderId ? noteInput : order.adminNote}" /></textarea>
                            <small id="note-help-${order.orderId}">Tối đa 1000 ký tự. Nội dung hiện tại sẽ hiển thị trong lịch sử của khách.</small>
                        </div>
                        <button class="btn btn-outline-secondary" type="submit">Lưu ghi chú</button>
                    </form>
                </div>
            </article>
        </c:forEach>
    </c:if>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
