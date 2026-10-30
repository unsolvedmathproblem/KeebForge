<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<c:set var="page" value="home" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="KeebForce — workshop lắp ráp, vệ sinh và chăm sóc bàn phím cơ theo nhu cầu của bạn.">
    <title>KeebForce — Chăm chút từng phím</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content">
    <section class="container hero" aria-labelledby="hero-title">
        <div class="hero-copy">
            <p class="eyebrow"><span aria-hidden="true">●</span> GÓC NHỎ CHO NGƯỜI YÊU BÀN PHÍM</p>
            <h1 id="hero-title">Bàn phím của bạn.<br><span>Chất riêng của bạn.</span></h1>
            <p class="hero-description">Từ những cú gõ đầu tiên đến một bộ phím thật vừa ý. KeebForce giúp bạn lắp ráp, chăm sóc và làm mới bàn phím theo nhu cầu.</p>
            <div class="hero-actions">
                <a class="btn btn-workshop" href="<c:url value='/services'/>">Xem gói dịch vụ <span aria-hidden="true">↗</span></a>
                <a class="btn catalog-reset-link" href="<c:url value='/parts'/>">Tham khảo linh kiện</a>
            </div>
            <p class="hero-note">Lắp ráp, chăm sóc và làm mới bàn phím cơ với mức công cố định minh bạch.</p>
        </div>
        <div class="keyboard-scene" role="img" aria-label="Minh họa bàn phím cơ màu kem, có phím nhấn màu cam và xanh lá">
            <div class="scene-label">BUILT AROUND YOU <span>01 / KE E B</span></div>
            <div class="keyboard">
                <div class="key-row"><span class="key accent">esc</span><span class="key">1</span><span class="key">2</span><span class="key">3</span><span class="key">4</span><span class="key">5</span><span class="key">6</span><span class="key">7</span><span class="key">8</span><span class="key">9</span><span class="key wide">del</span></div>
                <div class="key-row"><span class="key wide">tab</span><span class="key">Q</span><span class="key">W</span><span class="key">E</span><span class="key">R</span><span class="key">T</span><span class="key">Y</span><span class="key">U</span><span class="key">I</span><span class="key">O</span><span class="key">P</span></div>
                <div class="key-row"><span class="key wide">caps</span><span class="key">A</span><span class="key">S</span><span class="key">D</span><span class="key">F</span><span class="key">G</span><span class="key">H</span><span class="key">J</span><span class="key">K</span><span class="key enter">enter</span></div>
                <div class="key-row"><span class="key wide">shift</span><span class="key">Z</span><span class="key">X</span><span class="key">C</span><span class="key">V</span><span class="key">B</span><span class="key">N</span><span class="key">M</span><span class="key">↑</span><span class="key wide">shift</span></div>
                <div class="key-row"><span class="key">ctrl</span><span class="key">fn</span><span class="key">alt</span><span class="key space"></span><span class="key">alt</span><span class="key">←</span><span class="key">↓</span><span class="key">→</span></div>
            </div>
            <div class="scene-caption"><span>YOUR KEYS. YOUR FEEL.</span><span>Thiết kế để gõ mỗi ngày.</span></div>
        </div>
    </section>

    <div class="container">
        <jsp:include page="/WEB-INF/views/common/messages.jsp" />
    </div>

    <section id="workshop" class="container workshop-section" aria-labelledby="workshop-title">
        <div class="section-heading">
            <div>
                <p class="eyebrow">DỊCH VỤ WORKSHOP</p>
                <h2 id="workshop-title">Gói chăm sóc &amp; lắp ráp bàn phím</h2>
            </div>
            <a class="text-link" href="<c:url value='/services'/>">Xem chi tiết &amp; đánh giá →</a>
        </div>
        <c:if test="${not empty services}">
            <div class="row g-3">
                <c:forEach var="service" items="${services}" varStatus="status">
                    <c:url var="requestUrl" value="/request">
                        <c:param name="serviceId" value="${service.serviceId}" />
                    </c:url>
                    <c:url var="reviewUrl" value="/services">
                        <c:param name="id" value="${service.serviceId}" />
                    </c:url>
                    <div class="col-md-6">
                        <article class="workshop-card">
                            <span class="card-number"><c:out value="${status.count < 10 ? '0' : ''}${status.count}" /> / SERVICE #<c:out value="${service.serviceId}" /></span>
                            <h3><c:out value="${service.serviceName}" /></h3>
                            <p><c:out value="${service.description}" /></p>
                            <div class="card-detail">
                                <span>Phí công cố định</span>
                                <span class="money"><kf:money value="${service.basePrice}" /></span>
                            </div>
                            <div class="service-card-actions">
                                <a class="btn btn-workshop" href="<c:out value='${requestUrl}'/>">Gửi yêu cầu</a>
                                <a class="text-link" href="<c:out value='${reviewUrl}'/>#reviews">Xem đánh giá</a>
                            </div>
                        </article>
                    </div>
                </c:forEach>
            </div>
        </c:if>
        <c:if test="${empty services and empty requestScope.errorMessage}">
            <p class="notice">Hiện chưa có gói dịch vụ nào đang mở nhận yêu cầu.</p>
        </c:if>
    </section>

    <section id="linh-kien" class="container workshop-section" aria-labelledby="featured-parts-title">
        <div class="section-heading">
            <div>
                <p class="eyebrow">LINH KIỆN THAM KHẢO</p>
                <h2 id="featured-parts-title">Kit, switch &amp; keycap tại workshop</h2>
            </div>
            <a class="text-link" href="<c:url value='/parts'/>">Xem toàn bộ linh kiện →</a>
        </div>
        <c:url var="placeholderUrl" value="/images/placeholder.png" />
        <c:if test="${not empty products}">
            <div class="row g-4">
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
            <p class="notice">Hiện chưa có linh kiện nào để hiển thị.</p>
        </c:if>
    </section>

    <section id="quy-trinh" class="container process-section" aria-labelledby="process-title">
        <div class="process-intro"><p class="eyebrow">TỪ Ý TƯỞNG ĐẾN CẢM GIÁC GÕ</p><h2 id="process-title">Một quy trình thật dễ hiểu.</h2><p>Chia sẻ nhu cầu. Workshop tiếp nhận và chăm chút từng chi tiết.</p></div>
        <ol class="process-list"><li><span>01</span><div><h3>Chọn dịch vụ</h3><p>Xem nội dung và mức công được công bố.</p></div></li><li><span>02</span><div><h3>Kể về bàn phím của bạn</h3><p>Mô tả thiết bị, nhu cầu và chọn thêm linh kiện nếu cần.</p></div></li><li><span>03</span><div><h3>Gửi yêu cầu, theo dõi đơn</h3><p>Workshop tiếp nhận và cập nhật tiến độ xử lý.</p></div></li></ol>
    </section>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
