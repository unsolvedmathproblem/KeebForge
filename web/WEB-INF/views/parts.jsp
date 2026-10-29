<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<c:set var="page" value="parts" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Linh kiện &amp; Bàn phím — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container admin-main">
    <div class="admin-heading">
        <div>
            <p class="eyebrow"><span aria-hidden="true">●</span> CATALOG WORKSHOP</p>
            <h1>Linh kiện &amp; bàn phím tham khảo</h1>
            <p class="panel-description">Tra cứu kit, switch, keycap và phụ kiện hiện có tại workshop để chọn kèm khi gửi yêu cầu dịch vụ.</p>
        </div>
        <div class="admin-heading-actions">
            <c:if test="${not empty totalProducts}">
                <span class="account-badge"><c:out value="${totalProducts}" /> linh kiện</span>
            </c:if>
            <a class="btn btn-workshop" href="<c:url value='/services'/>">Chọn gói dịch vụ →</a>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/common/messages.jsp" />

    <section class="catalog-filter-panel" aria-label="Bộ lọc tìm kiếm linh kiện">
        <form action="<c:url value='/parts'/>" method="get" class="catalog-filter-form">
            <div class="form-field">
                <label for="filter-q">Tên linh kiện</label>
                <input id="filter-q" name="q" type="search" class="form-control" maxlength="120"
                       placeholder="Ví dụ: Kit 75%, Switch linear, PBT..."
                       value="<c:out value='${q}'/>">
            </div>
            <div class="form-field">
                <label for="filter-category">Danh mục</label>
                <select id="filter-category" name="categoryId" class="form-control">
                    <option value="">Tất cả danh mục</option>
                    <c:forEach var="cat" items="${categories}">
                        <option value="<c:out value='${cat.categoryId}'/>" ${cat.categoryId eq categoryId ? 'selected' : ''}>
                            <c:out value="${cat.categoryName}" />
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="form-field">
                <label for="filter-brand">Thương hiệu</label>
                <select id="filter-brand" name="brandId" class="form-control">
                    <option value="">Tất cả thương hiệu</option>
                    <c:forEach var="b" items="${brands}">
                        <option value="<c:out value='${b.brandId}'/>" ${b.brandId eq brandId ? 'selected' : ''}>
                            <c:out value="${b.brandName}" />
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="catalog-filter-actions">
                <button type="submit" class="btn btn-workshop">Lọc linh kiện</button>
                <c:if test="${not empty q or not empty categoryId or not empty brandId}">
                    <a class="btn catalog-reset-link" href="<c:url value='/parts'/>">Đặt lại</a>
                </c:if>
            </div>
        </form>
    </section>

    <c:url var="placeholderUrl" value="/images/placeholder.png" />

    <c:if test="${not empty products}">
        <div class="row g-4 catalog-grid">
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
                <div class="col-md-6 col-lg-4">
                    <article class="workshop-card part-card">
                        <div class="part-image-wrap">
                            <img class="part-image" src="<c:out value='${imgSrc}'/>"
                                 alt="<c:out value='${product.productName}'/>" loading="lazy">
                        </div>
                        <div class="part-meta">
                            <span class="account-badge"><c:out value="${product.categoryName}" /></span>
                            <span class="part-brand"><c:out value="${product.brandName}" /></span>
                        </div>
                        <h3><c:out value="${product.productName}" /></h3>
                        <p><c:out value="${empty product.description ? 'Linh kiện tham khảo dùng kèm khi đặt dịch vụ tại workshop.' : product.description}" /></p>
                        <div class="card-detail">
                            <span>Giá tham khảo</span>
                            <span class="money"><kf:money value="${product.price}" /></span>
                        </div>
                    </article>
                </div>
            </c:forEach>
        </div>
    </c:if>

    <c:if test="${empty products and empty requestScope.errorMessage}">
        <div class="notice" role="status">
            Không tìm thấy linh kiện phù hợp với bộ lọc hiện tại.
            <a class="text-link" href="<c:url value='/parts'/>">Xem toàn bộ danh mục</a>
        </div>
    </c:if>

    <c:if test="${totalPages > 1}">
        <p class="pagination-summary">Trang <strong><c:out value="${currentPage}" /></strong> / <c:out value="${totalPages}" /></p>
        <nav class="catalog-pagination" aria-label="Phân trang danh mục linh kiện">
            <c:if test="${currentPage > 1}">
                <c:url var="prevUrl" value="/parts">
                    <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                    <c:if test="${not empty categoryId}"><c:param name="categoryId" value="${categoryId}" /></c:if>
                    <c:if test="${not empty brandId}"><c:param name="brandId" value="${brandId}" /></c:if>
                    <c:param name="page" value="${currentPage - 1}" />
                </c:url>
                <a class="page-link-item" href="<c:out value='${prevUrl}'/>">← Trang trước</a>
            </c:if>

            <c:forEach begin="1" end="${totalPages}" var="p">
                <c:url var="pageUrl" value="/parts">
                    <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                    <c:if test="${not empty categoryId}"><c:param name="categoryId" value="${categoryId}" /></c:if>
                    <c:if test="${not empty brandId}"><c:param name="brandId" value="${brandId}" /></c:if>
                    <c:param name="page" value="${p}" />
                </c:url>
                <a class="page-link-item ${p eq currentPage ? 'page-link-active' : ''}"
                   href="<c:out value='${pageUrl}'/>"
                   aria-current="${p eq currentPage ? 'page' : 'false'}">
                    <c:out value="${p}" />
                </a>
            </c:forEach>

            <c:if test="${currentPage < totalPages}">
                <c:url var="nextUrl" value="/parts">
                    <c:if test="${not empty q}"><c:param name="q" value="${q}" /></c:if>
                    <c:if test="${not empty categoryId}"><c:param name="categoryId" value="${categoryId}" /></c:if>
                    <c:if test="${not empty brandId}"><c:param name="brandId" value="${brandId}" /></c:if>
                    <c:param name="page" value="${currentPage + 1}" />
                </c:url>
                <a class="page-link-item" href="<c:out value='${nextUrl}'/>">Trang sau →</a>
            </c:if>
        </nav>
    </c:if>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
