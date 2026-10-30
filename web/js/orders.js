/* Chỉ hỗ trợ nhập liệu và tổng tham khảo; server sẽ tính lại giá khi lưu đơn. */
(() => {
    const form = document.getElementById('request-form');
    if (!form) return;

    const estimate = document.getElementById('request-estimate');
    const totalOutput = document.getElementById('estimatedTotal');
    const rows = Array.from(form.querySelectorAll('[data-part-price]'));
    const money = new Intl.NumberFormat('vi-VN');

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
                quantity.setCustomValidity('Vui lòng nhập số nguyên từ 1 đến 200.');
                valid = false;
                return;
            }
            total += BigInt(row.dataset.partPrice) * BigInt(quantity.value);
        });
        totalOutput.textContent = valid ? money.format(total) + ' ₫' : 'Vui lòng kiểm tra số lượng';
    }

    ['contactName', 'deviceDescription'].forEach(id => {
        const input = document.getElementById(id);
        function validateText() {
            input.setCustomValidity(input.value.trim() ? '' : 'Vui lòng nhập nội dung, không chỉ khoảng trắng.');
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
