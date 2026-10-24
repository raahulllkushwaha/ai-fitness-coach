package com.rahul.aifitness.integration.intervalsicu;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class IntervalsTokenEncryptionService {

    private static final String PREFIX = "v1:";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final byte[] AAD =
            "ai-fitness-coach:intervals-icu:access-token"
                    .getBytes(StandardCharsets.UTF_8);

    private final String encodedKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public IntervalsTokenEncryptionService(
            @Value("${INTERVALS_TOKEN_ENCRYPTION_KEY:}")
            String encodedKey
    ) {
        this.encodedKey = encodedKey;
    }

    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            throw new IllegalArgumentException(
                    "Access token must not be blank"
            );
        }

        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getKey(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce)
            );
            cipher.updateAAD(AAD);

            byte[] ciphertext = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );

            byte[] payload = ByteBuffer
                    .allocate(nonce.length + ciphertext.length)
                    .put(nonce)
                    .put(ciphertext)
                    .array();

            return PREFIX + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(payload);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Could not encrypt the Intervals.icu token",
                    exception
            );
        }
    }

    public String decrypt(String encryptedValue) {
        if (!StringUtils.hasText(encryptedValue)
                || !encryptedValue.startsWith(PREFIX)) {
            throw new IllegalArgumentException(
                    "Invalid encrypted token format"
            );
        }

        try {
            byte[] payload = Base64.getUrlDecoder()
                    .decode(encryptedValue.substring(PREFIX.length()));

            if (payload.length <= NONCE_LENGTH + 16) {
                throw new IllegalArgumentException(
                        "Invalid encrypted token payload"
                );
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getKey(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce)
            );
            cipher.updateAAD(AAD);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Could not decrypt the Intervals.icu token; verify the encryption key",
                    exception
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        }
    }

    private SecretKeySpec getKey() {
        if (!StringUtils.hasText(encodedKey)) {
            throw new IllegalStateException(
                    "INTERVALS_TOKEN_ENCRYPTION_KEY is not configured"
            );
        }

        final byte[] keyBytes;
        try {
            keyBytes = Base64.getUrlDecoder().decode(encodedKey.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "INTERVALS_TOKEN_ENCRYPTION_KEY must be URL-safe Base64",
                    exception
            );
        }

        if (keyBytes.length != 32) {
            Arrays.fill(keyBytes, (byte) 0);
            throw new IllegalStateException(
                    "INTERVALS_TOKEN_ENCRYPTION_KEY must decode to exactly 32 bytes"
            );
        }

        return new SecretKeySpec(keyBytes, "AES");
    }
}