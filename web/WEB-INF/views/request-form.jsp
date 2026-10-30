<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Yêu cầu dịch vụ — KeebForce</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
    <link rel="stylesheet" href="<c:url value='/css/orders.css'/>">
    <script src="<c:url value='/js/orders.js'/>" defer></script>
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content" class="container profile-main request-main">
    <p class="eyebrow">CHĂM CHÚT CHO BÀN PHÍM CỦA BẠN</p>
    <h1>Yêu cầu dịch vụ</h1>
    <p class="panel-description">Mô tả bàn phím, để lại liên hệ và chọn thêm linh kiện nếu cần.</p>
    <jsp:include page="/WEB-INF/views/common/messages.jsp" />

    <c:if test="${not empty selectedService}">
        <section class="auth-panel request-service" aria-labelledby="selected-service-title">
            <div>
                <span class="account-badge">Dịch vụ đã chọn</span>
                <h2 id="selected-service-title"><c:out value="${selectedService.serviceName}" /></h2>
                <p class="request-description"><c:out value="${selectedService.description}" /></p>
            </div>
            <div class="request-service-price">
                <span>Tiền công cố định</span>
                <strong><kf:money value="${selectedService.basePrice}" /></strong>
            </div>
        </section>

        <form id="request-form" action="<c:url value='/request'/>" method="post">
            <input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
            <input type="hidden" name="action" value="create">
            <input type="hidden" name="orderFormToken" value="<c:out value='${orderFormToken}'/>">
            <input type="hidden" name="serviceId" value="<c:out value='${selectedService.serviceId}'/>">

            <section class="auth-panel request-section" aria-labelledby="contact-title">
                <h2 id="contact-title">Thông tin liên hệ</h2>
                <p class="panel-description">Workshop sẽ dùng thông tin này để trao đổi về yêu cầu của bạn.</p>
                <div class="form-grid">
                    <div class="form-field">
                        <label for="contactName">Họ và tên liên hệ</label>
                        <input class="form-control" type="text" id="contactName" name="contactName"
                               maxlength="100" required autocomplete="name" aria-describedby="contactName-help"
                               value="<c:out value='${contactName}'/>">
                        <small id="contactName-help">Bắt buộc, tối đa 100 ký tự.</small>
                    </div>
                    <div class="form-field">
                        <label for="contactPhone">Số điện thoại liên hệ</label>
                        <input class="form-control" type="tel" id="contactPhone" name="contactPhone"
                               minlength="7" maxlength="20" pattern="[0-9\+\(\)\. \-]{7,20}"
                               required autocomplete="tel" aria-describedby="contactPhone-help"
                               value="<c:out value='${contactPhone}'/>">
                        <small id="contactPhone-help">Bắt buộc, 7–20 ký tự: số, khoảng trắng, dấu +, -, . hoặc ngoặc đơn.</small>
                    </div>
                </div>
            </section>

            <section class="auth-panel request-section" aria-labelledby="device-title">
                <h2 id="device-title">Bàn phím và nhu cầu</h2>
                <div class="form-field">
                    <label for="deviceDescription">Mô tả thiết bị</label>
                    <textarea class="form-control" id="deviceDescription" name="deviceDescription" rows="4"
                              maxlength="1000" required aria-describedby="deviceDescription-help"
                              placeholder="Tên bàn phím hoặc kit, tình trạng hiện tại và công việc bạn mong muốn..."><c:out value="${deviceDescription}" /></textarea>
                    <small id="deviceDescription-help">Bắt buộc, tối đa 1000 ký tự. Đồ tự mang theo chỉ cần mô tả tại đây.</small>
                </div>
                <div class="form-field">
                    <label for="preferredDate">Ngày mong muốn <span class="optional-label">(tùy chọn)</span></label>
                    <input class="form-control" type="date" id="preferredDate" name="preferredDate"
                           min="<c:out value='${today}'/>" value="<c:out value='${preferredDateInput}'/>" aria-describedby="preferredDate-help">
                    <small id="preferredDate-help">Từ hôm nay theo giờ Việt Nam. Ngày tham khảo để workshop trao đổi, chưa phải lịch hẹn đã xác nhận.</small>
                </div>
                <div class="form-field">
                    <label for="customerNote">Ghi chú thêm <span class="optional-label">(tùy chọn)</span></label>
                    <textarea class="form-control" id="customerNote" name="customerNote" rows="3"
                              maxlength="1000" aria-describedby="customerNote-help"
                              placeholder="Linh kiện tự mang, thời gian tiện liên hệ hoặc lưu ý khác..."><c:out value="${customerNote}" /></textarea>
                    <small id="customerNote-help">Tối đa 1000 ký tự.</small>
                </div>
            </section>

            <section class="auth-panel request-section" aria-labelledby="parts-title">
                <h2 id="parts-title">Linh kiện chọn thêm</h2>
                <p id="parts-help" class="panel-description">Không bắt buộc chọn linh kiện. Nếu dùng đồ của workshop, chọn món và số lượng từ 1 đến 200 cho mỗi món.</p>
                <c:choose>
                    <c:when test="${empty products}">
                        <p>Hiện chưa có linh kiện nhận chọn thêm. Bạn vẫn có thể yêu cầu dịch vụ với đồ tự mang.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive data-table-wrap" tabindex="0" role="region" aria-labelledby="parts-title">
                            <table class="table data-table request-parts" aria-describedby="parts-help">
                                <caption class="visually-hidden">Linh kiện đang hoạt động và đơn giá tham khảo</caption>
                                <thead><tr><th scope="col">Chọn linh kiện</th><th scope="col">Đơn giá</th><th scope="col">Số lượng</th></tr></thead>
                                <tbody>
                                <c:forEach var="product" items="${products}">
                                    <tr data-part-price="<c:out value='${product.price}'/>">
                                        <td>
                                            <div class="request-part-choice">
                                                <input class="form-check-input" type="checkbox" id="part-${product.productId}"
                                                       name="productIds" value="<c:out value='${product.productId}'/>" ${selectedPartIds.contains(product.productId) ? 'checked' : ''}>
                                                <label for="part-${product.productId}"><c:out value="${product.productName}" /></label>
                                            </div>
                                            <small class="request-part-category"><c:out value="${product.categoryName}" /> · <c:out value="${product.brandName}" /></small>
                                        </td>
                                        <td class="money"><kf:money value="${product.price}" /></td>
                                        <td>
                                            <label class="visually-hidden" for="quantity-${product.productId}">Số lượng <c:out value="${product.productName}" /></label>
                                            <input class="form-control request-quantity" type="number" id="quantity-${product.productId}"
                                                   name="quantity_${product.productId}" min="1" max="200" step="1"
                                                   value="<c:out value="${quantities.containsKey(product.productId) ? quantities[product.productId] : '1'}"/>" required>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="auth-panel request-section" aria-labelledby="estimate-title">
                <h2 id="estimate-title">Chi phí tham khảo</h2>
                <div id="request-estimate" class="request-estimate" data-service-price="<c:out value='${selectedService.basePrice}'/>" hidden>
                    <span>Tổng ước tính</span>
                    <output id="estimatedTotal" class="money" aria-live="polite"></output>
                </div>
                <p id="estimate-help" class="panel-description">Tổng ước tính gồm tiền công và linh kiện đã chọn. Giá lưu khi gửi yêu cầu sẽ được tính lại theo giá hiện tại của workshop.</p>
                <noscript><p>Tiền công và đơn giá hiển thị ở trên. Bật JavaScript để tính tổng ước tính khi chọn linh kiện.</p></noscript>
                <button type="submit" class="btn btn-workshop" aria-describedby="estimate-help">Gửi yêu cầu</button>
            </section>
        </form>
    </c:if>
    <p><a class="text-link" href="<c:url value='/orders'/>">Xem lịch sử yêu cầu</a></p>
    <a class="text-link" href="<c:url value='/services'/>">← Quay lại chọn dịch vụ</a>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
