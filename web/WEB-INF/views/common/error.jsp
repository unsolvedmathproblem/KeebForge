<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="page" value="error" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Không thể tiếp tục — KeebForge</title>
    <link rel="stylesheet" href="<c:url value='/css/bootstrap.min.css'/>">
    <link rel="stylesheet" href="<c:url value='/css/style.css'/>">
</head>
<body class="account-page">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container profile-main">
    <p class="eyebrow">KEEBFORGE WORKSHOP</p>
    <h1>Không thể tiếp tục</h1>
    <div class="notice notice-error" role="alert"><c:out value="${errorMessage}" /></div>
    <a class="text-link" href="<c:url value='/home'/>">← Về trang chủ</a>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
