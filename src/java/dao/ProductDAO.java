package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.Brand;
import model.Category;
import model.Product;

/** DAO truy vấn danh mục linh kiện, thương hiệu và phân loại bằng PreparedStatement. */
public class ProductDAO extends DBContext {

    private static final String SELECT_COLUMNS = "p.ProductID, p.ProductName, p.CategoryID, "
            + "c.CategoryName, p.BrandID, b.BrandName, p.Description, p.Price, "
            + "p.ImageURL, p.IsActive, p.CreatedAt";

    private static final String FROM_JOIN = " FROM dbo.Product AS p "
            + "JOIN dbo.Category AS c ON c.CategoryID = p.CategoryID "
            + "JOIN dbo.Brand AS b ON b.BrandID = p.BrandID ";

    public ProductDAO() throws SQLException {
        super();
    }

    public List<Category> getAllCategories() throws SQLException {
        String sql = "SELECT CategoryID, CategoryName FROM dbo.Category ORDER BY CategoryID";
        List<Category> categories = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Category category = new Category();
                category.setCategoryId(result.getInt("CategoryID"));
                category.setCategoryName(result.getNString("CategoryName"));
                categories.add(category);
            }
        }
        return categories;
    }

    public List<Brand> getAllBrands() throws SQLException {
        String sql = "SELECT BrandID, BrandName FROM dbo.Brand ORDER BY BrandID";
        List<Brand> brands = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Brand brand = new Brand();
                brand.setBrandId(result.getInt("BrandID"));
                brand.setBrandName(result.getNString("BrandName"));
                brands.add(brand);
            }
        }
        return brands;
    }

    public int countProducts(String keyword, Integer categoryId, Integer brandId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dbo.Product AS p WHERE p.IsActive = 1");
        appendFilters(sql, keyword, categoryId, brandId);
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindFilters(statement, keyword, categoryId, brandId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    public List<Product> getProducts(String keyword, Integer categoryId, Integer brandId, int page, int pageSize)
            throws SQLException {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int offset = (safePage - 1) * safePageSize;

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(SELECT_COLUMNS)
                .append(FROM_JOIN)
                .append("WHERE p.IsActive = 1");
        appendFilters(sql, keyword, categoryId, brandId);
        sql.append(" ORDER BY p.ProductID ASC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = bindFilters(statement, keyword, categoryId, brandId);
            statement.setInt(index++, offset);
            statement.setInt(index, safePageSize);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    products.add(mapProduct(result));
                }
            }
        }
        return products;
    }

    public Product getProductById(int productId) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + FROM_JOIN + "WHERE p.ProductID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapProduct(result) : null;
            }
        }
    }

    public List<Product> getFeaturedProducts(int limit) throws SQLException {
        int safeLimit = Math.max(1, limit);
        String sql = "SELECT " + SELECT_COLUMNS + FROM_JOIN
                + "WHERE p.IsActive = 1 "
                + "ORDER BY p.CreatedAt DESC, p.ProductID DESC "
                + "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, safeLimit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    products.add(mapProduct(result));
                }
            }
        }
        return products;
    }

    public List<Product> getActiveProducts() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + FROM_JOIN
                + "WHERE p.IsActive = 1 ORDER BY p.ProductID ASC";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                products.add(mapProduct(result));
            }
        }
        return products;
    }

    public List<Product> getAdminProducts() throws SQLException {
        return getAdminProducts(null, null, null);
    }

    public List<Product> getAdminProducts(String keyword, Integer categoryId, Integer brandId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(SELECT_COLUMNS)
                .append(FROM_JOIN)
                .append("WHERE 1 = 1");
        appendFilters(sql, keyword, categoryId, brandId);
        sql.append(" ORDER BY p.ProductID ASC");

        List<Product> products = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindFilters(statement, keyword, categoryId, brandId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    products.add(mapProduct(result));
                }
            }
        }
        return products;
    }

    public boolean categoryExists(int categoryId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.Category WHERE CategoryID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, categoryId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean brandExists(int brandId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.Brand WHERE BrandID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, brandId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO dbo.Product "
                + "(ProductName, CategoryID, BrandID, Description, Price, ImageURL, IsActive) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setNString(1, product.getProductName());
            statement.setInt(2, product.getCategoryId());
            statement.setInt(3, product.getBrandId());
            setNullableNString(statement, 4, product.getDescription());
            statement.setBigDecimal(5, product.getPrice());
            setNullableNString(statement, 6, product.getImageUrl());
            statement.setBoolean(7, product.isActive());
            if (statement.executeUpdate() != 1) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    product.setProductId(keys.getInt(1));
                }
            }
            return true;
        }
    }

    public boolean updateProduct(Product product) throws SQLException {
        String sql = "UPDATE dbo.Product "
                + "SET ProductName = ?, CategoryID = ?, BrandID = ?, Description = ?, "
                + "Price = ?, ImageURL = ?, IsActive = ? "
                + "WHERE ProductID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, product.getProductName());
            statement.setInt(2, product.getCategoryId());
            statement.setInt(3, product.getBrandId());
            setNullableNString(statement, 4, product.getDescription());
            statement.setBigDecimal(5, product.getPrice());
            setNullableNString(statement, 6, product.getImageUrl());
            statement.setBoolean(7, product.isActive());
            statement.setInt(8, product.getProductId());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean toggleActive(int productId) throws SQLException {
        String sql = "UPDATE dbo.Product "
                + "SET IsActive = CASE WHEN IsActive = 1 THEN 0 ELSE 1 END "
                + "WHERE ProductID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean toggleActive(int productId, boolean active) throws SQLException {
        String sql = "UPDATE dbo.Product SET IsActive = ? WHERE ProductID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setInt(2, productId);
            return statement.executeUpdate() == 1;
        }
    }

    private void setNullableNString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isEmpty()) {
            statement.setNull(index, Types.NVARCHAR);
        } else {
            statement.setNString(index, value);
        }
    }

    private void appendFilters(StringBuilder sql, String keyword, Integer categoryId, Integer brandId) {
        if (hasKeyword(keyword)) {
            sql.append(" AND p.ProductName LIKE ? ESCAPE '\\'");
        }
        if (categoryId != null) {
            sql.append(" AND p.CategoryID = ?");
        }
        if (brandId != null) {
            sql.append(" AND p.BrandID = ?");
        }
    }

    private int bindFilters(PreparedStatement statement, String keyword, Integer categoryId, Integer brandId)
            throws SQLException {
        int index = 1;
        if (hasKeyword(keyword)) {
            statement.setNString(index++, "%" + escapeLike(keyword.trim()) + "%");
        }
        if (categoryId != null) {
            statement.setInt(index++, categoryId);
        }
        if (brandId != null) {
            statement.setInt(index++, brandId);
        }
        return index;
    }

    private boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.trim().isEmpty();
    }

    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("[", "\\[");
    }

    private Product mapProduct(ResultSet result) throws SQLException {
        Product product = new Product();
        product.setProductId(result.getInt("ProductID"));
        product.setProductName(result.getNString("ProductName"));
        product.setCategoryId(result.getInt("CategoryID"));
        product.setCategoryName(result.getNString("CategoryName"));
        product.setBrandId(result.getInt("BrandID"));
        product.setBrandName(result.getNString("BrandName"));
        product.setDescription(result.getNString("Description"));
        product.setPrice(result.getBigDecimal("Price"));
        product.setImageUrl(result.getNString("ImageURL"));
        product.setActive(result.getBoolean("IsActive"));
        product.setCreatedAt(result.getTimestamp("CreatedAt").toLocalDateTime());
        return product;
    }
}
