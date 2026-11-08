package tn.enicarthage.gestionsalon.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tn.enicarthage.gestionsalon.exception.AuthenticationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

public final class JwtUtil {

    public record Claims(int userId, String role) {}

    private static final byte[] SECRET = System
            .getenv().getOrDefault("JWT_SECRET", "dev-only-secret-change-me-please-32b")
            .getBytes(StandardCharsets.UTF_8);
    private static final Gson GSON = new Gson();
    private static final long DUREE_SECONDES = 3600;

    private JwtUtil() {}

    public static String creer(int userId, String role) {
        String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64(GSON.toJson(Map.of(
                "sub", userId,
                "role", role,
                "exp", Instant.now().getEpochSecond() + DUREE_SECONDES)));
        return header + "." + payload + "." + signer(header + "." + payload);
    }

    public static Claims verifier(String token) {
        try {
            String[] p = token.split("\\.");
            if (p.length != 3) throw new AuthenticationException("Jeton invalide.");

            byte[] attendu = signer(p[0] + "." + p[1]).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(attendu, p[2].getBytes(StandardCharsets.UTF_8))) {
                throw new AuthenticationException("Jeton invalide.");
            }

            JsonObject json = JsonParser.parseString(new String(
                    Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("exp").getAsLong() < Instant.now().getEpochSecond()) {
                throw new AuthenticationException("Jeton expire.");
            }
            return new Claims(json.get("sub").getAsInt(), json.get("role").getAsString());

        } catch (AuthenticationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AuthenticationException("Jeton invalide.");
        }
    }

    private static String signer(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Signature impossible", e);
        }
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}