<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<c:set var="page" value="admin-products" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý linh kiện — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container admin-main">
    <div class="admin-heading">
        <div>
            <p class="eyebrow">QUẢN TRỊ KEEBFORCE</p>
            <h1>Quản lý linh kiện</h1>
            <p class="panel-description">Thêm mới, cập nhật thông tin, giá tham khảo và trạng thái hiển thị của linh kiện trong catalog.</p>
        </div>
        <div class="admin-heading-actions">
            <c:if test="${not empty products}">
                <span class="account-badge"><c:out value="${products.size()}" /> linh kiện</span>
            </c:if>
            <c:url var="newProductUrl" value="/admin/products">
                <c:param name="action" value="new" />
            </c:url>
            <a class="btn btn-workshop" href="<c:out value='${newProductUrl}'/>">+ Thêm linh kiện</a>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/common/messages.jsp" />

    <section class="catalog-filter-panel" aria-label="Bộ lọc quản trị linh kiện">
        <form action="<c:url value='/admin/products'/>" method="get" class="catalog-filter-form">
            <input type="hidden" name="action" value="list">
            <div class="form-field">
                <label for="admin-filter-q">Tên linh kiện</label>
                <input id="admin-filter-q" name="q" type="search" class="form-control" maxlength="120"
                       placeholder="Tìm theo tên linh kiện..."
                       value="<c:out value='${q}'/>">
            </div>
            <div class="form-field">
                <label for="admin-filter-category">Danh mục</label>
                <select id="admin-filter-category" name="categoryId" class="form-control">
                    <option value="">Tất cả danh mục</option>
                    <c:forEach var="cat" items="${categories}">
                        <option value="<c:out value='${cat.categoryId}'/>" ${cat.categoryId eq categoryId ? 'selected' : ''}>
                            <c:out value="${cat.categoryName}" />
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-field">
                <label for="admin-filter-brand">Thương hiệu</label>
                <select id="admin-filter-brand" name="brandId" class="form-control">
                    <option value="">Tất cả thương hiệu</option>
                    <c:forEach var="b" items="${brands}">
                        <option value="<c:out value='${b.brandId}'/>" ${b.brandId eq brandId ? 'selected' : ''}>
                            <c:out value="${b.brandName}" />
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="catalog-filter-actions">
                <button type="submit" class="btn btn-workshop">Lọc</button>
                <c:if test="${not empty q or not empty categoryId or not empty brandId}">
                    <a class="btn catalog-reset-link" href="<c:url value='/admin/products'/>">Đặt lại</a>
                </c:if>
            </div>
        </form>
    </section>

    <c:url var="placeholderUrl" value="/images/placeholder.png" />

    <c:if test="${not empty products}">
        <div class="table-responsive data-table-wrap" tabindex="0" role="region" aria-label="Danh sách linh kiện có thể cuộn ngang">
            <table class="table data-table">
                <caption class="visually-hidden">Danh sách linh kiện quản trị kèm thao tác sửa và ẩn hoặc hiện trên catalog.</caption>
                <thead>
                    <tr>
                        <th scope="col">Mã / Ảnh</th>
                        <th scope="col">Linh kiện</th>
                        <th scope="col">Phân loại</th>
                        <th scope="col">Giá tham khảo</th>
                        <th scope="col">Trạng thái</th>
                        <th scope="col">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="product" items="${products}">
                        <c:choose>
                            <c:when test="${empty product.imageUrl}">
                                <c:set var="imgSrc" value="${placeholderUrl}" />
                            </c:when>
                            <c:when test="${fn:startsWith(product.imageUrl, 'http://') or fn:startsWith(product.imageUrl, 'https://')}">
                                <c:set var="imgSrc" value="${product.imageUrl}" />
                            </c:when>
                            <c:otherwise>
                                <c:url var="imgSrc" value="${fn:startsWith(product.imageUrl, '/') ? product.imageUrl : '/'.concat(product.imageUrl)}" />
                            </c:otherwise>
                        </c:choose>
                        <tr>
                            <td>
                                <div class="admin-product-cell">
                                    <img class="admin-thumb" src="<c:out value='${imgSrc}'/>"
                                         alt="<c:out value='${product.productName}'/>" loading="lazy">
                                    <span class="user-detail">#<c:out value="${product.productId}" /></span>
                                </div>
                            </td>
                            <td>
                                <strong><c:out value="${product.productName}" /></strong>
                                <span class="user-detail">
                                    <c:out value="${empty product.description ? 'Chưa có mô tả chi tiết.' : product.description}" />
                                </span>
                            </td>
                            <td>
                                <strong><c:out value="${product.categoryName}" /></strong>
                                <span class="user-detail"><c:out value="${product.brandName}" /></span>
                            </td>
                            <td>
                                <span class="money"><kf:money value="${product.price}" /></span>
                            </td>
                            <td>
                                <span class="status-badge ${product.active ? 'status-active' : 'status-locked'}">
                                    <c:out value="${product.active ? 'Đang hiển thị' : 'Ngừng hiển thị'}" />
                                </span>
                            </td>
                            <td>
                                <div class="admin-row-actions">
                                    <c:url var="editUrl" value="/admin/products">
                                        <c:param name="action" value="edit" />
                                        <c:param name="id" value="${product.productId}" />
                                    </c:url>
                                    <a class="btn admin-action-link" href="<c:out value='${editUrl}'/>">Sửa</a>
                                    <form action="<c:url value='/admin/products'/>" method="post">
                                        <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                        <input type="hidden" name="action" value="toggle">
                                        <input type="hidden" name="id" value="<c:out value='${product.productId}'/>">
                                        <input type="hidden" name="active" value="${not product.active}">
                                        <button type="submit" class="btn user-status-button ${product.active ? 'button-lock' : 'button-unlock'}">
                                            <c:out value="${product.active ? 'Ẩn catalog' : 'Hiện lại'}" />
                                        </button>
                                    </form>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

    <c:if test="${empty products and empty requestScope.errorMessage}">
        <p class="notice" role="status">Không có linh kiện nào khớp điều kiện hiện tại.</p>
    </c:if>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
