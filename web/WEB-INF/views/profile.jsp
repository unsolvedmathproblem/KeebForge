<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="page" value="profile" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hồ sơ của tôi — KeebForge</title>
    <link rel="stylesheet" href="<c:url value='/css/bootstrap.min.css'/>">
    <link rel="stylesheet" href="<c:url value='/css/style.css'/>">
</head>
<body class="account-page">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container profile-main">
    <p class="eyebrow">GÓC CỦA BẠN TẠI KEEBFORGE</p>
    <h1>Hồ sơ của tôi</h1>
    <p class="panel-description">Xem thông tin tài khoản và cập nhật liên hệ của bạn.</p>
    <c:if test="${not empty message}">
        <div class="notice notice-success" role="status"><c:out value="${message}" /></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="notice notice-error" role="alert"><c:out value="${errorMessage}" /></div>
    </c:if>
    <c:if test="${not empty profileAccount}">
        <section class="auth-panel profile-panel" aria-labelledby="profile-title">
            <div class="profile-heading">
                <h2 id="profile-title"><c:out value="${profileAccount.fullName}" /></h2>
                <span class="account-badge">
                    <c:choose>
                        <c:when test="${profileAccount.role eq 'ADMIN'}">Quản trị viên</c:when>
                        <c:otherwise>Khách hàng</c:otherwise>
                    </c:choose>
                </span>
            </div>
            <dl class="profile-grid">
                <div class="profile-field"><dt>Tên đăng nhập</dt><dd><c:out value="${profileAccount.username}" /></dd></div>
                <div class="profile-field"><dt>Email</dt><dd><c:out value="${profileAccount.email}" /></dd></div>
                <div class="profile-field"><dt>Ngày tham gia</dt><dd><c:out value="${joinedAt}" /></dd></div>
                <div class="profile-field"><dt>Trạng thái</dt><dd>Đang hoạt động</dd></div>
            </dl>
            <form action="<c:url value='/profile'/>" method="post" class="profile-form">
                <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                <input type="hidden" name="action" value="update">
                <h3>Thông tin liên hệ</h3>
                <div class="form-grid">
                    <div class="form-field">
                        <label for="fullName">Họ và tên</label>
                        <input class="form-control" type="text" id="fullName" name="fullName"
                               maxlength="100" autocomplete="name" required value="<c:out value='${fullName}'/>">
                    </div>
                    <div class="form-field">
                        <label for="phone">Số điện thoại <span class="optional-label">(tùy chọn)</span></label>
                        <input class="form-control" type="tel" id="phone" name="phone"
                               maxlength="20" autocomplete="tel" value="<c:out value='${phone}'/>">
                    </div>
                </div>
                <button type="submit" class="btn btn-workshop">Lưu thay đổi</button>
            </form>
        </section>
    </c:if>
    <a class="text-link" href="<c:url value='/home'/>">← Về trang chủ</a>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
