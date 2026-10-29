package controller.admin;

import dao.ProductDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import model.Account;
import model.Brand;
import model.Category;
import model.Product;

/** Quản trị danh mục linh kiện: danh sách, thêm mới, chỉnh sửa và ẩn/hiện hiển thị. */
@WebServlet(name = "AdminProductServlet", urlPatterns = {"/admin/products"})
public class AdminProductServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String action = trimToEmpty(request.getParameter("action"));
        if (action.isEmpty() || "list".equals(action)) {
            String saved = request.getParameter("saved");
            if ("created".equals(saved)) {
                request.setAttribute("message", "Đã thêm linh kiện mới vào danh mục.");
            } else if ("updated".equals(saved)) {
                request.setAttribute("message", "Đã cập nhật thông tin linh kiện.");
            } else if ("1".equals(request.getParameter("toggled"))) {
                request.setAttribute("message", "Đã cập nhật trạng thái hiển thị của linh kiện.");
            }
            showList(request, response);
            return;
        }
        if ("new".equals(action)) {
            showNewForm(request, response);
            return;
        }
        if ("edit".equals(action)) {
            showEditForm(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("errorMessage", "Thao tác quản trị linh kiện không hợp lệ.");
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentAdmin(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String action = trimToEmpty(request.getParameter("action"));
        if ("save".equals(action)) {
            handleSave(request, response);
            return;
        }
        if ("toggle".equals(action)) {
            handleToggle(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("errorMessage", "Thao tác biểu mẫu không hợp lệ.");
        showList(request, response);
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = trimToEmpty(request.getParameter("q"));
        if (keyword.length() > 120) {
            keyword = keyword.substring(0, 120);
        }
        Integer requestedCategoryId = parsePositiveInt(request.getParameter("categoryId"));
        Integer requestedBrandId = parsePositiveInt(request.getParameter("brandId"));

        request.setAttribute("page", "admin-products");
        request.setAttribute("q", keyword);

        try (ProductDAO dao = new ProductDAO()) {
            List<Category> categories = dao.getAllCategories();
            List<Brand> brands = dao.getAllBrands();

            Integer categoryId = containsCategory(categories, requestedCategoryId) ? requestedCategoryId : null;
            Integer brandId = containsBrand(brands, requestedBrandId) ? requestedBrandId : null;
            List<Product> products = dao.getAdminProducts(keyword, categoryId, brandId);

            request.setAttribute("categories", categories);
            request.setAttribute("brands", brands);
            request.setAttribute("categoryId", categoryId);
            request.setAttribute("brandId", brandId);
            request.setAttribute("products", products);
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh sách quản trị linh kiện.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể tải danh sách linh kiện lúc này. Vui lòng thử lại sau.");
        }

        request.getRequestDispatcher("/WEB-INF/views/admin/products.jsp").forward(request, response);
    }

    private void showNewForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Product draft = new Product();
        draft.setActive(true);

        request.setAttribute("page", "admin-products");
        request.setAttribute("formMode", "new");
        request.setAttribute("product", draft);
        request.setAttribute("priceInput", "");
        request.setAttribute("productIdValue", "");

        try (ProductDAO dao = new ProductDAO()) {
            request.setAttribute("categories", dao.getAllCategories());
            request.setAttribute("brands", dao.getAllBrands());
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải dữ liệu biểu mẫu thêm linh kiện.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể tải danh mục và thương hiệu lúc này. Vui lòng thử lại sau.");
        }

        request.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer productId = parsePositiveInt(request.getParameter("id"));
        if (productId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", "Mã linh kiện cần chỉnh sửa không hợp lệ.");
            showList(request, response);
            return;
        }

        try (ProductDAO dao = new ProductDAO()) {
            Product product = dao.getProductById(productId);
            if (product == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("errorMessage", "Không tìm thấy linh kiện cần chỉnh sửa.");
                showList(request, response);
                return;
            }
            request.setAttribute("page", "admin-products");
            request.setAttribute("formMode", "edit");
            request.setAttribute("product", product);
            request.setAttribute("priceInput", product.getPrice() != null ? product.getPrice().toPlainString() : "");
            request.setAttribute("productIdValue", String.valueOf(product.getProductId()));
            request.setAttribute("categories", dao.getAllCategories());
            request.setAttribute("brands", dao.getAllBrands());
            request.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(request, response);
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải thông tin linh kiện để chỉnh sửa.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể tải thông tin linh kiện lúc này. Vui lòng thử lại sau.");
            showList(request, response);
        }
    }

    private void handleSave(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String formMode = trimToEmpty(request.getParameter("formMode"));
        String idRaw = request.getParameter("id");
        if (idRaw == null) {
            idRaw = request.getParameter("productId");
        }
        idRaw = trimToEmpty(idRaw);

        boolean isEdit = "edit".equals(formMode) || !idRaw.isEmpty();
        Integer parsedId = isEdit ? parsePositiveInt(idRaw) : null;

        String productName = trimToEmpty(request.getParameter("productName"));
        String categoryRaw = trimToEmpty(request.getParameter("categoryId"));
        String brandRaw = trimToEmpty(request.getParameter("brandId"));
        String priceRaw = trimToEmpty(request.getParameter("price"));
        String imageUrl = trimToEmpty(request.getParameter("imageUrl"));
        String description = trimToEmpty(request.getParameter("description"));
        Boolean parsedActive = parseActive(request.getParameter("active"));

        Integer categoryId = parsePositiveInt(categoryRaw);
        Integer brandId = parsePositiveInt(brandRaw);

        Product draft = new Product();
        if (parsedId != null) {
            draft.setProductId(parsedId);
        }
        draft.setProductName(productName);
        if (categoryId != null) {
            draft.setCategoryId(categoryId);
        }
        if (brandId != null) {
            draft.setBrandId(brandId);
        }
        draft.setImageUrl(imageUrl.isEmpty() ? null : imageUrl);
        draft.setDescription(description.isEmpty() ? null : description);
        draft.setActive(parsedActive != null ? parsedActive : true);

        String error = validateFields(isEdit, parsedId, productName, categoryId, brandId,
                priceRaw, imageUrl, description, parsedActive, draft);

        request.setAttribute("page", "admin-products");
        request.setAttribute("formMode", isEdit ? "edit" : "new");
        request.setAttribute("product", draft);
        request.setAttribute("priceInput", priceRaw);
        request.setAttribute("productIdValue", idRaw);

        try (ProductDAO dao = new ProductDAO()) {
            List<Category> categories = dao.getAllCategories();
            List<Brand> brands = dao.getAllBrands();
            request.setAttribute("categories", categories);
            request.setAttribute("brands", brands);

            if (error == null) {
                if (!containsCategory(categories, categoryId)) {
                    error = "Danh mục đã chọn không tồn tại trong hệ thống.";
                } else if (!containsBrand(brands, brandId)) {
                    error = "Thương hiệu đã chọn không tồn tại trong hệ thống.";
                }
            }

            if (error != null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                request.setAttribute("errorMessage", error);
                request.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(request, response);
                return;
            }

            if (isEdit) {
                if (!dao.updateProduct(draft)) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    request.setAttribute("errorMessage", "Không tìm thấy linh kiện cần cập nhật.");
                    request.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(request, response);
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/admin/products?saved=updated");
                return;
            }

            dao.addProduct(draft);
            response.sendRedirect(request.getContextPath() + "/admin/products?saved=created");
        } catch (SQLException ex) {
            getServletContext().log("Không thể lưu thông tin linh kiện.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể lưu thông tin linh kiện lúc này. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(request, response);
        }
    }

    private void handleToggle(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idRaw = request.getParameter("id");
        if (idRaw == null) {
            idRaw = request.getParameter("productId");
        }
        Integer productId = parsePositiveInt(idRaw);
        String activeParam = request.getParameter("active");

        if (productId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", "Mã linh kiện cần đổi trạng thái không hợp lệ.");
            showList(request, response);
            return;
        }
        if (activeParam != null && !activeParam.trim().isEmpty()
                && !("true".equals(activeParam.trim()) || "false".equals(activeParam.trim()))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errorMessage", "Trạng thái đích của linh kiện không hợp lệ.");
            showList(request, response);
            return;
        }

        try (ProductDAO dao = new ProductDAO()) {
            boolean updated;
            if (activeParam != null && !activeParam.trim().isEmpty()) {
                updated = dao.toggleActive(productId, "true".equals(activeParam.trim()));
            } else {
                updated = dao.toggleActive(productId);
            }
            if (!updated) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("errorMessage", "Không tìm thấy linh kiện cần đổi trạng thái.");
                showList(request, response);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/admin/products?toggled=1");
        } catch (SQLException ex) {
            getServletContext().log("Không thể đổi trạng thái linh kiện.", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Chưa thể cập nhật trạng thái linh kiện. Vui lòng thử lại sau.");
            showList(request, response);
        }
    }

    private String validateFields(boolean isEdit, Integer parsedId, String productName,
            Integer categoryId, Integer brandId, String priceRaw, String imageUrl,
            String description, Boolean parsedActive, Product draft) {
        if (isEdit && parsedId == null) {
            return "Mã linh kiện cần cập nhật không hợp lệ.";
        }
        if (productName.isEmpty()) {
            return "Vui lòng nhập tên linh kiện.";
        }
        if (productName.length() > 120) {
            return "Tên linh kiện không được vượt quá 120 ký tự.";
        }
        if (containsHtmlOrScript(productName)) {
            return "Tên linh kiện không được chứa thẻ HTML hoặc mã script.";
        }
        if (categoryId == null) {
            return "Vui lòng chọn danh mục hợp lệ.";
        }
        if (brandId == null) {
            return "Vui lòng chọn thương hiệu hợp lệ.";
        }
        if (priceRaw.isEmpty()) {
            return "Vui lòng nhập giá tham khảo.";
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceRaw).setScale(0, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException ex) {
            return "Giá tham khảo không đúng định dạng số.";
        } catch (ArithmeticException ex) {
            return "Giá tham khảo phải là số nguyên đồng (không có phần thập phân).";
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            return "Giá tham khảo không được là số âm.";
        }
        if (price.precision() > 18) {
            return "Giá tham khảo vượt quá giới hạn cho phép (tối đa 18 chữ số).";
        }
        draft.setPrice(price);

        if (!imageUrl.isEmpty()) {
            if (imageUrl.length() > 500) {
                return "Đường dẫn ảnh không được vượt quá 500 ký tự.";
            }
            if (!isSafeImageUrl(imageUrl)) {
                return "Đường dẫn ảnh chỉ chấp nhận đường dẫn nội bộ (ví dụ /images/...) hoặc URL http/https hợp lệ.";
            }
        }
        if (description.length() > 1000) {
            return "Mô tả linh kiện không được vượt quá 1000 ký tự.";
        }
        if (containsHtmlOrScript(description)) {
            return "Mô tả linh kiện không được chứa thẻ HTML hoặc mã script.";
        }
        if (parsedActive == null) {
            return "Trạng thái hiển thị không hợp lệ.";
        }
        return null;
    }

    private boolean containsHtmlOrScript(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (value.indexOf('<') >= 0 || value.indexOf('>') >= 0) {
            return true;
        }
        return value.toLowerCase(Locale.ROOT).contains("javascript:");
    }

    private boolean isSafeImageUrl(String url) {
        for (int i = 0; i < url.length(); i++) {
            char ch = url.charAt(i);
            if (ch <= ' ' || ch == '<' || ch == '>' || ch == '"' || ch == '\'') {
                return false;
            }
        }
        if (url.contains("..")) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return url.length() > 8;
        }
        if (url.startsWith("/")) {
            return !url.startsWith("//");
        }
        return lower.startsWith("images/");
    }

    private Boolean parseActive(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Boolean.FALSE;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "on".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized) || "0".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Integer parsePositiveInt(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean containsCategory(List<Category> categories, Integer categoryId) {
        if (categoryId == null) {
            return false;
        }
        for (Category category : categories) {
            if (category.getCategoryId() == categoryId) {
                return true;
            }
        }
        return false;
    }

    private boolean containsBrand(List<Brand> brands, Integer brandId) {
        if (brandId == null) {
            return false;
        }
        for (Brand brand : brands) {
            if (brand.getBrandId() == brandId) {
                return true;
            }
        }
        return false;
    }

    private String trimToEmpty(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private Account currentAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") instanceof Account) {
            Account user = (Account) session.getAttribute("user");
            if ("ADMIN".equals(user.getRole())) {
                return user;
            }
        }
        return null;
    }
}
