<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="kf" tagdir="/WEB-INF/tags" %>
<c:set var="page" value="home" scope="request" />
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="KeebForge — workshop lắp ráp, vệ sinh và chăm sóc bàn phím cơ theo nhu cầu của bạn.">
    <title>KeebForge — Chăm chút từng phím</title>
    <jsp:include page="/WEB-INF/views/common/styles.jsp" />
</head>
<body class="page-body">
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<main id="main-content">
    <section class="container hero" aria-labelledby="hero-title">
        <div class="hero-copy">
            <p class="eyebrow"><span aria-hidden="true">●</span> GÓC NHỎ CHO NGƯỜI YÊU BÀN PHÍM</p>
            <h1 id="hero-title">Bàn phím của bạn.<br><span>Chất riêng của bạn.</span></h1>
            <p class="hero-description">Từ những cú gõ đầu tiên đến một bộ phím thật vừa ý. KeebForge giúp bạn lắp ráp, chăm sóc và làm mới bàn phím theo nhu cầu.</p>
            <a class="btn btn-workshop" href="#quy-trinh">Khám phá workshop <span aria-hidden="true">↗</span></a>
            <p class="hero-note">Lắp ráp, chăm sóc và làm mới bàn phím cơ.</p>
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
    <section id="workshop" class="container workshop-section" aria-labelledby="workshop-title">
        <div class="section-heading"><div><p class="eyebrow">MỘT CHÚT CHĂM CHÚT, NHIỀU CẢM HỨNG</p><h2 id="workshop-title">Góc chăm sóc bàn phím của bạn</h2></div><p>Giữ lại điều bạn thích.<br>Hoàn thiện điều bạn cần.</p></div>
        <div class="row g-3">
            <div class="col-md-4">
                <article class="workshop-card">
                    <span class="card-number">01 / BUILD</span>
                    <h3>Lắp ráp theo ý bạn</h3>
                    <p>Từ kit, switch đến keycap — cùng tạo nên bộ phím mang dấu ấn của bạn.</p>
                    <div class="card-detail"><span>Phí công tham khảo</span><span class="money"><kf:money value="${200000}" /></span></div>
                </article>
            </div>
            <div class="col-md-4">
                <article class="workshop-card">
                    <span class="card-number">02 / CARE</span>
                    <h3>Làm mới cảm giác gõ</h3>
                    <p>Vệ sinh cơ bản để bộ phím quen thuộc luôn sẵn sàng cho một ngày mới.</p>
                    <div class="card-detail"><span>Phí công tham khảo</span><span class="money"><kf:money value="${150000}" /></span></div>
                </article>
            </div>
            <div class="col-md-4">
                <article class="workshop-card">
                    <span class="card-number">03 / REPAIR</span>
                    <h3>Khắc phục lỗi cơ bản</h3>
                    <p>Thay switch cơ bản cho bàn phím hot-swap. Linh kiện thay thế tính riêng nếu cần.</p>
                    <div class="card-detail"><span>Phí công tham khảo</span><span class="money"><kf:money value="${100000}" /></span></div>
                </article>
            </div>
        </div>
    </section>
    <section id="quy-trinh" class="container process-section" aria-labelledby="process-title">
        <div class="process-intro"><p class="eyebrow">TỪ Ý TƯỞNG ĐẾN CẢM GIÁC GÕ</p><h2 id="process-title">Một quy trình thật dễ hiểu.</h2><p>Chia sẻ nhu cầu. Workshop tiếp nhận và chăm chút từng chi tiết.</p></div>
        <ol class="process-list"><li><span>01</span><div><h3>Chọn dịch vụ</h3><p>Xem nội dung và mức công được công bố.</p></div></li><li><span>02</span><div><h3>Kể về bàn phím của bạn</h3><p>Mô tả thiết bị, nhu cầu và chọn thêm linh kiện nếu cần.</p></div></li><li><span>03</span><div><h3>Gửi yêu cầu, theo dõi đơn</h3><p>Workshop tiếp nhận và cập nhật tiến độ xử lý.</p></div></li></ol>
    </section>
</main>
<jsp:include page="/WEB-INF/views/common/footer.jsp" />
</body>
</html>
