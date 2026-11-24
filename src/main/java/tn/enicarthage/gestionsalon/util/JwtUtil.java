package tn.enicarthage.gestionsalon.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tn.enicarthage.gestionsalon.exception.AuthenticationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

public final class JwtUtil {

    public record Claims(int userId, String role) {}

    private static final int TAILLE_MIN_SECRET = 32; // octets : 256 bits pour HMAC-SHA256
    private static final Logger LOGGER = Logger.getLogger(JwtUtil.class.getName());
    private static final byte[] SECRET = chargerSecret();
    private static final Gson GSON = new Gson();
    private static final long DUREE_SECONDES = 3600;

    private JwtUtil() {}

    // Appelee au demarrage : charge le secret tout de suite (avertissements visibles des le lancement)
    public static void verifierConfiguration() {
        // le chargement de la classe initialise SECRET
    }

    // Le secret vient de JWT_SECRET. Aucun secret n'est ecrit dans le code : sans la variable,
    // un secret aleatoire est genere au demarrage (les jetons deviennent invalides au redemarrage).
    private static byte[] chargerSecret() {
        String env = System.getenv("JWT_SECRET");
        if (env != null && !env.isBlank()) {
            byte[] secret = env.getBytes(StandardCharsets.UTF_8);
            if (secret.length < TAILLE_MIN_SECRET) {
                LOGGER.warning("JWT_SECRET court (" + secret.length + " caracteres) : "
                        + TAILLE_MIN_SECRET + " caracteres minimum conseilles pour HMAC-SHA256.");
            }
            return secret;
        }
        LOGGER.warning("JWT_SECRET non defini : secret aleatoire genere. "
                + "Les jetons seront invalides apres un redemarrage du serveur.");
        byte[] secret = new byte[TAILLE_MIN_SECRET];
        new SecureRandom().nextBytes(secret);
        return secret;
    }

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