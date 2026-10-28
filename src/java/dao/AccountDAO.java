package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import model.Account;

/** Servlet tạo DAO trong từng request và đóng bằng try-with-resources. */
public class AccountDAO extends DBContext {

    private static final String COLUMNS = "AccountID, Username, Email, PasswordHash, "
            + "FullName, Phone, Role, IsActive, CreatedAt";

    public AccountDAO() throws SQLException {
        super();
    }

    public Account getByUsername(String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM dbo.UserAccount WHERE Username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapAccount(result) : null;
            }
        }
    }

    public Account getById(int accountId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM dbo.UserAccount WHERE AccountID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapAccount(result) : null;
            }
        }
    }

    public boolean existsUsernameOrEmail(String username, String email) throws SQLException {
        String sql = "SELECT 1 FROM dbo.UserAccount WHERE Username = ? OR Email = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setNString(2, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /** Chỉ nhận hash; role và active được ép trong SQL, không lấy từ form. */
    public boolean register(Account account) throws SQLException {
        String sql = "INSERT INTO dbo.UserAccount "
                + "(Username, Email, PasswordHash, FullName, Phone, Role, IsActive) "
                + "VALUES (?, ?, ?, ?, ?, 'CUSTOMER', 1)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, account.getUsername());
            statement.setNString(2, account.getEmail());
            statement.setNString(3, account.getPasswordHash());
            statement.setNString(4, account.getFullName());
            statement.setString(5, account.getPhone());
            if (statement.executeUpdate() != 1) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    account.setAccountId(keys.getInt(1));
                }
            }
            account.setRole("CUSTOMER");
            account.setActive(true);
            return true;
        }
    }

    public boolean updateProfile(int accountId, String fullName, String phone) throws SQLException {
        String sql = "UPDATE dbo.UserAccount SET FullName = ?, Phone = ? WHERE AccountID = ? AND IsActive = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, fullName);
            statement.setString(2, phone);
            statement.setInt(3, accountId);
            return statement.executeUpdate() == 1;
        }
    }

    private Account mapAccount(ResultSet result) throws SQLException {
        Account account = new Account();
        account.setAccountId(result.getInt("AccountID"));
        account.setUsername(result.getString("Username"));
        account.setEmail(result.getNString("Email"));
        account.setPasswordHash(result.getNString("PasswordHash"));
        account.setFullName(result.getNString("FullName"));
        account.setPhone(result.getString("Phone"));
        account.setRole(result.getString("Role"));
        account.setActive(result.getBoolean("IsActive"));
        account.setCreatedAt(result.getTimestamp("CreatedAt").toLocalDateTime());
        return account;
    }
}
