/* Chỉ hỗ trợ nhập liệu và tổng tham khảo; server sẽ tính lại giá khi lưu đơn. */
(() => {
    const form = document.getElementById('request-form');
    if (!form) return;

    const estimate = document.getElementById('request-estimate');
    const totalOutput = document.getElementById('estimatedTotal');
    const rows = Array.from(form.querySelectorAll('[data-part-price]'));
    const money = new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        maximumFractionDigits: 0
    });

    function updateEstimate() {
        let total = BigInt(estimate.dataset.servicePrice);
        let valid = true;
        rows.forEach(row => {
            const checkbox = row.querySelector('[name="productIds"]');
            const quantity = row.querySelector('input[type="number"]');
            quantity.setCustomValidity('');
            quantity.disabled = !checkbox.checked;
            if (!checkbox.checked) return;
            if (!quantity.validity.valid || !/^[0-9]+$/.test(quantity.value)) {
                quantity.setCustomValidity('Vui l\u00f2ng nh\u1eadp s\u1ed1 nguy\u00ean t\u1eeb 1 \u0111\u1ebfn 200.');
                valid = false;
                return;
            }
            total += BigInt(row.dataset.partPrice) * BigInt(quantity.value);
        });
        totalOutput.textContent = valid ? money.format(total) : 'Vui l\u00f2ng ki\u1ec3m tra s\u1ed1 l\u01b0\u1ee3ng';
    }

    ['contactName', 'deviceDescription'].forEach(id => {
        const input = document.getElementById(id);
        function validateText() {
            input.setCustomValidity(input.value.trim() ? '' : 'Vui l\u00f2ng nh\u1eadp n\u1ed9i dung, kh\u00f4ng ch\u1ec9 kho\u1ea3ng tr\u1eafng.');
        }
        input.addEventListener('input', validateText);
        validateText();
    });
    form.addEventListener('input', updateEstimate);
    form.addEventListener('change', updateEstimate);
    window.addEventListener('pageshow', updateEstimate);
    estimate.hidden = false;
    updateEstimate();
})();
