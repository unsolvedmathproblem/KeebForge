package util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash một chiều có salt riêng; không lưu mật khẩu rõ trong database. */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(hash);
    }

    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || password.isEmpty() || storedHash == null || storedHash.length() > 255) {
            return false;
        }
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        int iterations;
        byte[] salt;
        byte[] expected;
        try {
            iterations = Integer.parseInt(parts[1]);
            // Không cho hash hỏng yêu cầu số vòng lặp quá lớn, gây treo request.
            if (iterations < ITERATIONS || iterations > 1_200_000) {
                return false;
            }
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (salt.length != SALT_BYTES || expected.length != HASH_BYTES) {
            return false;
        }
        return MessageDigest.isEqual(expected, derive(password, salt, iterations));
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] characters = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(characters, salt, iterations, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Không thể tạo hash PBKDF2 trên Java hiện tại.", ex);
        } finally {
            spec.clearPassword();
            Arrays.fill(characters, '\0');
        }
    }
}
