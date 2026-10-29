<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<c:set var="page" value="admin-services" scope="request" />
<c:set var="isEditMode" value="${formMode eq 'edit'}" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý dịch vụ — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container admin-main">
    <div class="admin-heading">
        <div>
            <p class="eyebrow">QUẢN TRỊ KEEBFORCE</p>
            <h1>Quản lý dịch vụ workshop</h1>
            <p class="panel-description">
                Thiết lập các gói công cố định của workshop. Việc đổi tiền công hoặc ngừng nhận mới không ảnh hưởng đến giá công đã lưu trên các đơn cũ.
            </p>
        </div>
        <div class="admin-heading-actions">
            <c:if test="${not empty services}">
                <span class="account-badge"><c:out value="${services.size()}" /> gói dịch vụ</span>
            </c:if>
            <c:if test="${isEditMode}">
                <a class="btn btn-workshop" href="<c:url value='/admin/services'/>#service-form">+ Chuyển sang thêm mới</a>
            </c:if>
        </div>
    </div>

    <jsp:include page="/WEB-INF/views/common/messages.jsp" />

    <section id="service-form" class="auth-panel admin-inline-panel" aria-labelledby="service-form-title">
        <div class="profile-heading">
            <h2 id="service-form-title">
                <c:choose>
                    <c:when test="${isEditMode}">
                        Cập nhật gói dịch vụ <c:if test="${formService.serviceId > 0}">#<c:out value="${formService.serviceId}" /></c:if>
                    </c:when>
                    <c:otherwise>Thêm gói dịch vụ mới</c:otherwise>
                </c:choose>
            </h2>
            <span class="account-badge"><c:out value="${isEditMode ? 'Chế độ chỉnh sửa' : 'Tạo mới'}" /></span>
        </div>

        <form action="<c:url value='/admin/services'/>" method="post">
            <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
            <input type="hidden" name="action" value="save">
            <input type="hidden" name="formMode" value="<c:out value='${formMode}'/>">
            <c:if test="${isEditMode}">
                <input type="hidden" name="id" value="<c:out value='${serviceIdValue}'/>">
            </c:if>

            <div class="form-field">
                <label for="serviceName">Tên gói dịch vụ</label>
                <input class="form-control" type="text" id="serviceName" name="serviceName"
                       maxlength="120" required aria-describedby="serviceName-help"
                       placeholder="Ví dụ: Lắp ráp bàn phím, Vệ sinh cơ bản..."
                       value="<c:out value='${formService.serviceName}'/>">
                <small id="serviceName-help">Bắt buộc, tối đa 120 ký tự, không chèn thẻ HTML.</small>
            </div>

            <div class="form-grid">
                <div class="form-field">
                    <label for="basePrice">Tiền công cố định (VND)</label>
                    <input class="form-control" type="number" id="basePrice" name="basePrice"
                           min="0" max="999999999999999999" step="1" required aria-describedby="basePrice-help"
                           placeholder="Ví dụ: 200000"
                           value="<c:out value='${basePriceInput}'/>">
                    <small id="basePrice-help">Mức công cố định theo gói (số nguyên đồng không âm).</small>
                </div>

                <div class="form-field">
                    <label for="active">Trạng thái nhận đơn</label>
                    <select class="form-control" id="active" name="active" aria-describedby="active-help">
                        <option value="true" ${formService.active ? 'selected' : ''}>Đang nhận yêu cầu</option>
                        <option value="false" ${not formService.active ? 'selected' : ''}>Ngừng nhận yêu cầu mới</option>
                    </select>
                    <small id="active-help">Ngừng nhận chỉ ẩn khỏi danh sách đặt mới, không xóa lịch sử đơn.</small>
                </div>
            </div>

            <div class="form-field">
                <label for="description">Mô tả công việc &amp; điều kiện nhận thiết bị</label>
                <textarea class="form-control admin-textarea" id="description" name="description"
                          rows="3" maxlength="1000" required aria-describedby="description-help"
                          placeholder="Nêu rõ những hạng mục đã gồm trong gói công và lưu ý khi khách gửi bàn phím..."><c:out value="${formService.description}" /></textarea>
                <small id="description-help">Bắt buộc, tối đa 1000 ký tự, chỉ dùng văn bản thuần (không chèn HTML/script).</small>
            </div>

            <div class="admin-form-actions">
                <button type="submit" class="btn btn-workshop">
                    <c:out value="${isEditMode ? 'Lưu cập nhật' : 'Thêm gói dịch vụ'}" />
                </button>
                <c:if test="${isEditMode}">
                    <a class="btn catalog-reset-link" href="<c:url value='/admin/services'/>">Hủy chỉnh sửa</a>
                </c:if>
            </div>
        </form>
    </section>

    <c:if test="${not empty services}">
        <div class="table-responsive data-table-wrap" tabindex="0" role="region" aria-label="Danh sách gói dịch vụ có thể cuộn ngang">
            <table class="table data-table">
                <caption class="visually-hidden">Danh sách gói dịch vụ workshop kèm thao tác sửa và bật hoặc tắt nhận yêu cầu mới.</caption>
                <thead>
                    <tr>
                        <th scope="col">Mã</th>
                        <th scope="col">Gói dịch vụ &amp; phạm vi công việc</th>
                        <th scope="col">Tiền công cố định</th>
                        <th scope="col">Trạng thái</th>
                        <th scope="col">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="s" items="${services}">
                        <tr class="${isEditMode and s.serviceId eq formService.serviceId ? 'admin-row-editing' : ''}">
                            <td><strong>#<c:out value="${s.serviceId}" /></strong></td>
                            <td>
                                <strong><c:out value="${s.serviceName}" /></strong>
                                <span class="user-detail"><c:out value="${s.description}" /></span>
                            </td>
                            <td>
                                <span class="money"><kf:money value="${s.basePrice}" /></span>
                            </td>
                            <td>
                                <span class="status-badge ${s.active ? 'status-active' : 'status-locked'}">
                                    <c:out value="${s.active ? 'Đang nhận yêu cầu' : 'Ngừng nhận mới'}" />
                                </span>
                            </td>
                            <td>
                                <div class="admin-row-actions">
                                    <c:url var="editServiceUrl" value="/admin/services">
                                        <c:param name="id" value="${s.serviceId}" />
                                    </c:url>
                                    <a class="btn admin-action-link" href="<c:out value='${editServiceUrl}'/>#service-form">Sửa</a>
                                    <form action="<c:url value='/admin/services'/>" method="post">
                                        <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
                                        <input type="hidden" name="action" value="toggle">
                                        <input type="hidden" name="id" value="<c:out value='${s.serviceId}'/>">
                                        <input type="hidden" name="active" value="${not s.active}">
                                        <button type="submit" class="btn user-status-button ${s.active ? 'button-lock' : 'button-unlock'}">
                                            <c:out value="${s.active ? 'Ngừng nhận' : 'Mở nhận lại'}" />
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

    <c:if test="${empty services and empty requestScope.errorMessage}">
        <p class="notice" role="status">Chưa có gói dịch vụ nào trong hệ thống.</p>
    </c:if>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
