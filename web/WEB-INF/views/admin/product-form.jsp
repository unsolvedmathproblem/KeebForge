<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="page" value="admin-products" scope="request" />
<c:set var="isEditMode" value="${formMode eq 'edit'}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${isEditMode ? 'Cập nhật linh kiện' : 'Thêm linh kiện mới'}" /> — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container profile-main">
    <p class="eyebrow">QUẢN TRỊ KEEBFORCE</p>
    <h1>
        <c:choose>
            <c:when test="${isEditMode}">
                Cập nhật linh kiện <c:if test="${product.productId > 0}"><span>#<c:out value="${product.productId}" /></span></c:if>
            </c:when>
            <c:otherwise>Thêm linh kiện mới</c:otherwise>
        </c:choose>
    </h1>
    <p class="panel-description">
        Thay đổi thông tin hoặc giá linh kiện chỉ áp dụng cho các yêu cầu dịch vụ mới; các đơn đã gửi giữ nguyên tên và đơn giá đã lưu.
    </p>

    <jsp:include page="/WEB-INF/views/common/messages.jsp" />

    <section class="auth-panel profile-panel" aria-labelledby="product-form-title">
        <div class="profile-heading">
            <h2 id="product-form-title"><c:out value="${isEditMode ? 'Thông tin linh kiện' : 'Khai báo linh kiện catalog'}" /></h2>
            <span class="account-badge"><c:out value="${isEditMode ? 'Chế độ chỉnh sửa' : 'Tạo mới'}" /></span>
        </div>

        <form action="<c:url value='/admin/products'/>" method="post">
            <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
            <input type="hidden" name="action" value="save">
            <input type="hidden" name="formMode" value="<c:out value='${formMode}'/>">
            <c:if test="${isEditMode}">
                <input type="hidden" name="id" value="<c:out value='${productIdValue}'/>">
            </c:if>

            <div class="form-field">
                <label for="productName">Tên linh kiện</label>
                <input class="form-control" type="text" id="productName" name="productName"
                       maxlength="120" required aria-describedby="productName-help"
                       placeholder="Ví dụ: Kit bàn phím nhôm 75%..."
                       value="<c:out value='${product.productName}'/>">
                <small id="productName-help">Bắt buộc, tối đa 120 ký tự, không chứa thẻ HTML.</small>
            </div>

            <div class="form-grid">
                <div class="form-field">
                    <label for="categoryId">Danh mục</label>
                    <select class="form-control" id="categoryId" name="categoryId" required>
                        <option value="">-- Chọn danh mục --</option>
                        <c:forEach var="cat" items="${categories}">
                            <option value="<c:out value='${cat.categoryId}'/>" ${cat.categoryId eq product.categoryId ? 'selected' : ''}>
                                <c:out value="${cat.categoryName}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-field">
                    <label for="brandId">Thương hiệu</label>
                    <select class="form-control" id="brandId" name="brandId" required>
                        <option value="">-- Chọn thương hiệu --</option>
                        <c:forEach var="b" items="${brands}">
                            <option value="<c:out value='${b.brandId}'/>" ${b.brandId eq product.brandId ? 'selected' : ''}>
                                <c:out value="${b.brandName}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>
            </div>

            <div class="form-grid">
                <div class="form-field">
                    <label for="price">Giá tham khảo (VND)</label>
                    <input class="form-control" type="number" id="price" name="price"
                           min="0" max="999999999999999999" step="1" required aria-describedby="price-help"
                           placeholder="Ví dụ: 350000"
                           value="<c:out value='${priceInput}'/>">
                    <small id="price-help">Số nguyên đồng không âm (ví dụ: 0, 5000, 1200000).</small>
                </div>

                <div class="form-field">
                    <label for="active">Trạng thái hiển thị</label>
                    <select class="form-control" id="active" name="active" aria-describedby="active-help">
                        <option value="true" ${product.active ? 'selected' : ''}>Đang hiển thị (cho phép chọn mới)</option>
                        <option value="false" ${not product.active ? 'selected' : ''}>Ngừng hiển thị (lưu trữ)</option>
                    </select>
                    <small id="active-help">Ngừng hiển thị chỉ ẩn khỏi catalog và biểu mẫu mới, không xóa lịch sử đơn.</small>
                </div>
            </div>

            <div class="form-field">
                <label for="imageUrl">Đường dẫn ảnh (ImageURL) <span class="optional-label">(tùy chọn)</span></label>
                <input class="form-control" type="text" id="imageUrl" name="imageUrl"
                       maxlength="500" aria-describedby="imageUrl-help"
                       placeholder="/images/placeholder.png hoặc https://..."
                       value="<c:out value='${product.imageUrl}'/>">
                <small id="imageUrl-help">Để trống để dùng ảnh mặc định local (/images/placeholder.png), hoặc nhập đường dẫn nội bộ / URL http(s).</small>
            </div>

            <div class="form-field">
                <label for="description">Mô tả linh kiện <span class="optional-label">(tùy chọn)</span></label>
                <textarea class="form-control admin-textarea" id="description" name="description"
                          rows="4" maxlength="1000" aria-describedby="description-help"
                          placeholder="Mô tả công dụng, quy cách tính giá hoặc lưu ý khi chọn kèm dịch vụ..."><c:out value="${product.description}" /></textarea>
                <small id="description-help">Tối đa 1000 ký tự, chỉ dùng văn bản thuần, không chèn thẻ HTML hoặc script.</small>
            </div>

            <div class="admin-form-actions">
                <button type="submit" class="btn btn-workshop">
                    <c:out value="${isEditMode ? 'Lưu cập nhật' : 'Thêm linh kiện'}" />
                </button>
                <a class="btn catalog-reset-link" href="<c:url value='/admin/products'/>">Hủy bỏ</a>
            </div>
        </form>
    </section>

    <a class="text-link" href="<c:url value='/admin/products'/>">← Quay lại danh sách linh kiện</a>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
