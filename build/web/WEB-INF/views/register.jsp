<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="page" value="register" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng ký — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container auth-main auth-main-register">
    <section class="auth-intro" aria-labelledby="intro-title">
        <p class="eyebrow">BẮT ĐẦU TỪ MỘT KẾT NỐI</p>
        <h1 id="intro-title">Một tài khoản.<br><span>Một góc của riêng bạn.</span></h1>
        <p>Tạo tài khoản khách hàng để đồng hành cùng KeebForce trên hành trình chăm chút bộ phím yêu thích.</p>
        <a class="text-link" href="<c:url value='/home'/>">← Tìm hiểu về workshop</a>
    </section>
    <section class="auth-panel" aria-labelledby="form-title">
        <p class="eyebrow">CHÀO MỪNG ĐẾN VỚI KEEBFORCE</p>
        <h2 id="form-title">Tạo tài khoản</h2>
        <p class="panel-description">Điền thông tin bên dưới. Số điện thoại có thể bổ sung sau.</p>
        <jsp:include page="/WEB-INF/views/common/messages.jsp" />
        <form action="<c:url value='/register'/>" method="post" class="account-form">
            <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
            <div class="form-grid">
                <div class="form-field">
                    <label for="username">Tên đăng nhập</label>
                    <input class="form-control" id="username" name="username" type="text"
                           minlength="3" maxlength="50" pattern="[A-Za-z0-9_]{3,50}"
                           autocomplete="username" aria-describedby="username-help" required
                           value="<c:out value='${username}'/>">
                    <small id="username-help">3–50 ký tự: chữ không dấu, số, dấu _.</small>
                </div>
                <div class="form-field">
                    <label for="email">Email</label>
                    <input class="form-control" id="email" name="email" type="email"
                           maxlength="100" autocomplete="email" required
                           value="<c:out value='${email}'/>">
                </div>
                <div class="form-field">
                    <label for="fullName">Họ và tên</label>
                    <input class="form-control" id="fullName" name="fullName" type="text"
                           maxlength="100" autocomplete="name" required
                           value="<c:out value='${fullName}'/>">
                </div>
                <div class="form-field">
                    <label for="phone">Số điện thoại <span class="optional-label">(tùy chọn)</span></label>
                    <input class="form-control" id="phone" name="phone" type="tel"
                           minlength="7" maxlength="20" autocomplete="tel" aria-describedby="phone-help"
                           value="<c:out value='${phone}'/>">
                    <small id="phone-help">Có thể để trống; nếu nhập, dùng 7–20 ký tự.</small>
                </div>
                <div class="form-field">
                    <label for="password">Mật khẩu</label>
                    <input class="form-control" id="password" name="password" type="password"
                           minlength="8" maxlength="128" autocomplete="new-password"
                           aria-describedby="password-help" required>
                    <small id="password-help">Dùng từ 8 đến 128 ký tự.</small>
                </div>
                <div class="form-field">
                    <label for="confirmPassword">Nhập lại mật khẩu</label>
                    <input class="form-control" id="confirmPassword" name="confirmPassword" type="password"
                           minlength="8" maxlength="128" autocomplete="new-password" required>
                </div>
            </div>
            <button type="submit" class="btn btn-workshop form-submit">Tạo tài khoản <span aria-hidden="true">↗</span></button>
        </form>
        <p class="auth-switch">Đã có tài khoản? <a class="text-link" href="<c:url value='/login'/>">Đăng nhập</a></p>
    </section>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
