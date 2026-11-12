package budgetlyDB;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Cryptographic password hashing utility using PBKDF2WithHmacSHA256.
 *
 * <p>Every password receives a unique 128-bit random salt generated via
 * {@link SecureRandom}. Hashes are verified using {@link MessageDigest#isEqual(byte[], byte[])}
 * to provide constant-time comparison and mitigate timing side-channel attacks.</p>
 */
public final class PasswordHasher {

    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int DEFAULT_ITERATIONS = 120_000;
    public static final int SALT_LENGTH_BYTES = 16;
    public static final int KEY_LENGTH_BITS = 256;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * Container holding the formatted hash string and Base64-encoded salt.
     */
    public static final class HashedPassword {
        private final String hash;
        private final String salt;

        public HashedPassword(String hash, String salt) {
            this.hash = hash;
            this.salt = salt;
        }

        public String getHash() {
            return hash;
        }

        public String getSalt() {
            return salt;
        }
    }

    /**
     * Generates a cryptographically strong random salt encoded as Base64.
     */
    public static String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * Hashes the given password characters using a newly generated unique salt
     * and the configured default PBKDF2 iteration count.
     *
     * <p>The supplied {@code password} array is cleared in memory after hashing.</p>
     */
    public static HashedPassword hashPassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        byte[] saltBytes = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(saltBytes);
        String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);
        try {
            byte[] derivedKey = deriveKey(password, saltBytes, DEFAULT_ITERATIONS, KEY_LENGTH_BITS);
            try {
                String hashFormatted = DEFAULT_ITERATIONS + ":" + Base64.getEncoder().encodeToString(derivedKey);
                return new HashedPassword(hashFormatted, saltBase64);
            } finally {
                Arrays.fill(derivedKey, (byte) 0);
            }
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(saltBytes, (byte) 0);
        }
    }

    /**
     * Verifies a candidate password against a stored PBKDF2 hash and Base64 salt.
     *
     * <p>The supplied {@code candidatePassword} array is cleared in memory after verification.</p>
     */
    public static boolean verifyPassword(char[] candidatePassword, String storedHash, String storedSalt) {
        if (candidatePassword == null || candidatePassword.length == 0
                || storedHash == null || storedHash.isEmpty()
                || storedSalt == null || storedSalt.isEmpty()) {
            if (candidatePassword != null) {
                Arrays.fill(candidatePassword, '\0');
            }
            return false;
        }

        byte[] saltBytes = null;
        byte[] expectedBytes = null;
        byte[] actualBytes = null;
        try {
            int iterations = DEFAULT_ITERATIONS;
            String hashBase64 = storedHash;
            int colonIdx = storedHash.indexOf(':');
            if (colonIdx > 0) {
                iterations = Integer.parseInt(storedHash.substring(0, colonIdx));
                hashBase64 = storedHash.substring(colonIdx + 1);
            }

            saltBytes = Base64.getDecoder().decode(storedSalt.getBytes(StandardCharsets.UTF_8));
            expectedBytes = Base64.getDecoder().decode(hashBase64.getBytes(StandardCharsets.UTF_8));
            actualBytes = deriveKey(candidatePassword, saltBytes, iterations, expectedBytes.length * 8);

            // Constant-time comparison prevents attackers from inferring matching prefix bytes via timing.
            return MessageDigest.isEqual(expectedBytes, actualBytes);
        } catch (IllegalArgumentException ex) {
            return false;
        } finally {
            Arrays.fill(candidatePassword, '\0');
            if (saltBytes != null) {
                Arrays.fill(saltBytes, (byte) 0);
            }
            if (expectedBytes != null) {
                Arrays.fill(expectedBytes, (byte) 0);
            }
            if (actualBytes != null) {
                Arrays.fill(actualBytes, (byte) 0);
            }
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Password hashing algorithm unavailable: " + ALGORITHM, ex);
        } finally {
            spec.clearPassword();
        }
    }
}
