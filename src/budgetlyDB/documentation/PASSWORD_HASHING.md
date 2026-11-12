# Password Hashing & Security (`PasswordHasher.java`)

This document explains how Budgetly protects user and administrator passwords using cryptographic hashing, why hashing is fundamentally different from encryption, and how `PasswordHasher.java` implements **PBKDF2WithHmacSHA256**.

---

## 1. What Is Password Hashing?

**Password hashing** is a **one-way** mathematical transformation that converts a password of any length into a fixed-length sequence of bytes (called a *hash* or *digest*).

- You can go forward (`password -> hash`),
- You **cannot** reverse the process (`hash -> original password`).

---

## 2. Why Passwords Must Never Be Stored as Plain Text

If passwords were saved directly as plain text in the `USERS` table:
- Anyone inspecting the database file, backup, or NetBeans table viewer could immediately read every user's password.
- Because people often reuse passwords across school, email, and banking apps, a plain-text leak exposes users far beyond Budgetly.

---

## 3. Hashing vs. Encryption (Do Not Confuse Them!)

| Concept | Encryption | Password Hashing |
| :--- | :--- | :--- |
| Direction | **Two-way** (Encrypt with key -> Decrypt back to original text) | **One-way** (Hash cannot be decrypted back to original password) |
| Goal | Confidentiality of data that needs to be read later | Verifying a secret without ever storing or retrieving the secret |
| Key needed to read original? | Yes (decryption key) | Impossible — there is no decryption key |

**Important:** Budgetly **hashes** passwords; it does **not** encrypt them. The application never needs to recover your original password. During login, it simply hashes what you just typed and checks whether the resulting hash matches the hash stored during registration.

---

## 4. What Is a Salt and Why Do We Need One?

A **salt** is a unique sequence of random bytes generated specifically for one password event (16 bytes / 128 bits in Budgetly).

Without a salt:
1. Two users who happen to pick the same password (e.g., `"budget2026"`) would produce the exact same hash in the `USERS` table, revealing that they share the same password.
2. Attackers could use precomputed lookup tables (*rainbow tables*) to crack common passwords instantaneously.

With a unique random salt per user:
- Even if Alice and Bob enter the exact same password, their `password_salt` values are different, so their `password_hash` values are completely different.
- Precomputed rainbow tables become useless because an attacker would have to recompute hashes separately for every single salt.

---

## 5. How `PBKDF2WithHmacSHA256` Works

Budgetly uses **PBKDF2WithHmacSHA256** (*Password-Based Key Derivation Function 2* using *HMAC-SHA-256*).

Why PBKDF2WithHmacSHA256 is ideal for this Java academic project:
1. **Built into the standard JDK** (`javax.crypto.SecretKeyFactory` and `javax.crypto.spec.PBEKeySpec`) — zero external dependencies required.
2. **Intentionally slow (work factor / iteration count)**: Instead of running SHA-256 once (which modern GPUs can guess billions of times per second), PBKDF2 repeats the HMAC-SHA-256 mixing **120,000 times** (`DEFAULT_ITERATIONS = 120_000`) to produce a **256-bit** (`KEY_LENGTH_BITS = 256`) derived key.
   - To a legitimate user logging in, 120,000 iterations takes only ~40–80 milliseconds.
   - To an attacker trying millions of password guesses, it increases the computational cost by 120,000x.

---

## 6. Registration Process (How Passwords Are Hashed)

```text
User enters password
        |
        v
Generate 16-byte random salt (SecureRandom)
        |
        v
Hash password + salt with PBKDF2WithHmacSHA256 (120,000 iterations, 256-bit key)
        |
        v
Format hash ("120000:<Base64Key>") and Base64 salt
        |
        v
Store password_hash & password_salt in Derby USERS table
```

### Actual Code from `PasswordHasher.java`

```java
public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
public static final int DEFAULT_ITERATIONS = 120_000;
public static final int SALT_LENGTH_BYTES = 16;
public static final int KEY_LENGTH_BITS = 256;
private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
```

---

## 7. Login Process (How Passwords Are Verified)

```text
User enters username and password
        |
        v
Retrieve stored password_salt and password_hash from USERS table
        |
        v
Hash entered password using the SAME stored salt & iteration count
        |
        v
Compare computed bytes vs stored bytes using MessageDigest.isEqual(...)
        |
        v
If equal -> allow login (provided account_status is ACTIVE)
Otherwise -> reject login
```

### Actual Verification Code from `PasswordHasher.java`

```java
public static boolean verifyPassword(char[] candidatePassword, String storedHash, String storedSalt) {
    ...
    saltBytes = Base64.getDecoder().decode(storedSalt.getBytes(StandardCharsets.UTF_8));
    expectedBytes = Base64.getDecoder().decode(hashBase64.getBytes(StandardCharsets.UTF_8));
    actualBytes = deriveKey(candidatePassword, saltBytes, iterations, expectedBytes.length * 8);

    // Constant-time comparison prevents attackers from inferring matching prefix bytes via timing.
    return MessageDigest.isEqual(expectedBytes, actualBytes);
}
```

---

## 8. Key Security Details Explained

### Why Original Passwords Cannot Be Retrieved from the Hash
Hash functions compress arbitrary input into 256 bits using thousands of non-invertible bitwise and modular mixing rounds (HMAC-SHA256). Information is discarded during compression, so there is no mathematical inverse function to reconstruct the original input.

### Why Constant-Time Comparison (`MessageDigest.isEqual`) Matters
Standard equality checks like `Arrays.equals()` or `String.equals()` stop immediately on the first mismatched byte. In theory, tiny timing differences can leak how many leading bytes matched (*timing side-channel attack*). `MessageDigest.isEqual(expectedBytes, actualBytes)` inspects **all** bytes in constant time before returning `true` or `false`.

### Clearing Sensitive Memory (`char[]`)
Passwords are read from `JPasswordField.getPassword()` as `char[]` rather than immutable `String` objects, and both `PasswordHasher` (`Arrays.fill(password, '\0')`, `spec.clearPassword()`) and `AuthenticationService` zero out sensitive character and byte arrays in `finally` blocks immediately after hashing or verification.

### How Password Changes Are Handled
When a user or administrator changes their password in `ProfileSettingsFrame` or `AdminDashboard`:
1. They must enter their **current password**, which is verified using `PasswordHasher.verifyPassword(...)`.
2. Their new password is hashed with a **brand-new random 16-byte salt** (never reusing the old salt).
3. Both `password_hash` and `password_salt` in `USERS` are updated via `PreparedStatement`.
4. Neither `PasswordHasher` nor any log ever prints or stores plain-text passwords.
