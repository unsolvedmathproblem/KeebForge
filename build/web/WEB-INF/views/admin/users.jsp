<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="page" value="users" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý tài khoản — KeebForge</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container admin-main">
    <div class="admin-heading">
        <div>
            <p class="eyebrow">QUẢN TRỊ KEEBFORGE</p>
            <h1>Quản lý tài khoản</h1>
            <p class="panel-description">Xem thông tin và quản lý quyền hoạt động của tài khoản.</p>
        </div>
        <c:if test="${not empty accounts}">
            <span class="account-badge"><c:out value="${accounts.size()}" /> tài khoản</span>
        </c:if>
    </div>
    <jsp:include page="/WEB-INF/views/common/messages.jsp" />
    <c:if test="${not empty accounts}">
        <div class="table-responsive data-table-wrap" tabindex="0" role="region" aria-label="Danh sách tài khoản có thể cuộn ngang">
            <table class="table data-table">
                <caption class="visually-hidden">Danh sách tài khoản và thao tác khóa, mở tài khoản.</caption>
                <thead>
                    <tr>
                        <th scope="col">Tài khoản</th>
                        <th scope="col">Thông tin liên hệ</th>
                        <th scope="col">Vai trò</th>
                        <th scope="col">Trạng thái</th>
                        <th scope="col">Ngày tham gia</th>
                        <th scope="col">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="account" items="${accounts}">
                        <tr>
                            <td><strong><c:out value="${account.username}" /></strong><span class="user-detail">#<c:out value="${account.accountId}" /></span></td>
                            <td>
                                <strong><c:out value="${account.fullName}" /></strong>
                                <span class="user-detail"><c:out value="${account.email}" /></span>
                                <span class="user-detail"><c:out value="${empty account.phone ? 'Chưa bổ sung số điện thoại' : account.phone}" /></span>
                            </td>
                            <td><c:out value="${account.role eq 'ADMIN' ? 'Quản trị viên' : 'Khách hàng'}" /></td>
                            <td><span class="status-badge ${account.active ? 'status-active' : 'status-locked'}"><c:out value="${account.active ? 'Đang hoạt động' : 'Đã khóa'}" /></span></td>
                            <td><c:out value="${account.createdAt.toLocalDate()}" /></td>
                            <td>
                                <c:choose>
                                    <c:when test="${account.accountId eq sessionScope.user.accountId}">
                                        <span class="user-detail">Tài khoản của bạn</span>
                                    </c:when>
                                    <c:otherwise>
                                        <form action="<c:url value='/admin/users'/>" method="post">
                                            <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                            <input type="hidden" name="action" value="toggle">
                                            <input type="hidden" name="accountId" value="<c:out value='${account.accountId}'/>">
                                            <input type="hidden" name="active" value="${not account.active}">
                                            <button type="submit" class="btn user-status-button ${account.active ? 'button-lock' : 'button-unlock'}"><c:out value="${account.active ? 'Khóa tài khoản' : 'Mở tài khoản'}" /></button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>
    <c:if test="${empty accounts and empty errorMessage}">
        <p class="notice">Chưa có tài khoản nào để hiển thị.</p>
    </c:if>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
