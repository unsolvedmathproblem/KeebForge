<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<a class="skip-link" href="#main-content">Bỏ qua menu, đến nội dung</a>
<header class="site-header">
    <nav class="container nav-shell" aria-label="Điều hướng chính">
        <a class="brand" href="<c:url value='/home'/>"><span class="brand-mark" aria-hidden="true">K.</span> KeebForge<span class="brand-label">WORKSHOP</span></a>
        <div class="nav-links">
            <a href="<c:url value='/home'/>" aria-current="${page eq 'home' ? 'page' : 'false'}">Trang chủ</a>
            <a href="<c:url value='/services'/>" aria-current="${page eq 'services' ? 'page' : 'false'}">Dịch vụ</a>
            <a href="<c:url value='/parts'/>" aria-current="${page eq 'parts' ? 'page' : 'false'}">Linh kiện</a>
            <a href="<c:url value='/home'/>#quy-trinh">Quy trình</a>
            <c:choose>
                <c:when test="${not empty sessionScope.user}">
                    <c:if test="${sessionScope.user.role eq 'ADMIN'}">
                        <span class="account-badge">Quản trị viên</span>
                        <a href="<c:url value='/admin/products'/>" aria-current="${page eq 'admin-products' ? 'page' : 'false'}">QL Linh kiện</a>
                        <a href="<c:url value='/admin/services'/>" aria-current="${page eq 'admin-services' ? 'page' : 'false'}">QL Dịch vụ</a>
                        <a href="<c:url value='/admin/users'/>" aria-current="${page eq 'users' ? 'page' : 'false'}">Tài khoản</a>
                    </c:if>
                    <a href="<c:url value='/profile'/>" aria-current="${page eq 'profile' ? 'page' : 'false'}">Hồ sơ</a>
                    <form action="<c:url value='/logout'/>" method="post" class="nav-form">
                        <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                        <button type="submit" class="nav-cta nav-button">Đăng xuất</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <a href="<c:url value='/login'/>" aria-current="${page eq 'login' ? 'page' : 'false'}">Đăng nhập</a>
                    <a class="nav-cta" href="<c:url value='/register'/>" aria-current="${page eq 'register' ? 'page' : 'false'}">Tạo tài khoản <span aria-hidden="true">↗</span></a>
                </c:otherwise>
            </c:choose>
        </div>
    </nav>
</header>
