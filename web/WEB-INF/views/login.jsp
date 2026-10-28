<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="page" value="login" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập — KeebForge</title>
    <link rel="stylesheet" href="<c:url value='/css/bootstrap.min.css'/>">
    <link rel="stylesheet" href="<c:url value='/css/style.css'/>">
</head>
<body class="account-page">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container auth-main">
    <section class="auth-intro" aria-labelledby="intro-title">
        <p class="eyebrow">MỖI BỘ PHÍM, MỘT CÂU CHUYỆN</p>
        <h1 id="intro-title">Chào bạn.<br><span>Rất vui khi gặp lại.</span></h1>
        <p>Đăng nhập để kết nối với góc chăm sóc bàn phím của bạn tại KeebForge.</p>
        <a class="text-link" href="<c:url value='/home'/>">← Khám phá workshop</a>
    </section>
    <section class="auth-panel" aria-labelledby="form-title">
        <p class="eyebrow">TÀI KHOẢN KEEBFORGE</p>
        <h2 id="form-title">Đăng nhập</h2>
        <p class="panel-description">Nhập thông tin tài khoản của bạn để tiếp tục.</p>
        <c:if test="${not empty message}">
            <div class="notice notice-success" role="status"><c:out value="${message}" /></div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="notice notice-error" role="alert"><c:out value="${errorMessage}" /></div>
        </c:if>
        <form action="<c:url value='/login'/>" method="post" class="account-form">
            <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
            <div class="form-field">
                <label for="username">Tên đăng nhập</label>
                <input class="form-control" id="username" name="username" type="text"
                       maxlength="50" autocomplete="username" required
                       value="<c:out value='${username}'/>">
            </div>
            <div class="form-field">
                <label for="password">Mật khẩu</label>
                <input class="form-control" id="password" name="password" type="password"
                       maxlength="128" autocomplete="current-password" required>
            </div>
            <button type="submit" class="btn btn-workshop form-submit">Đăng nhập <span aria-hidden="true">↗</span></button>
        </form>
        <p class="auth-switch">Bạn chưa có tài khoản? <a class="text-link" href="<c:url value='/register'/>">Đăng ký ngay</a></p>
    </section>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
