package tn.enicarthage.gestionsalon.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Codes a 6 chiffres pour les emails : generation aleatoire et empreinte SHA-256. */
public final class CodeUtil {

    private static final SecureRandom RNG = new SecureRandom();

    private CodeUtil() {}

    public static String genererCode() {
        return String.format("%06d", RNG.nextInt(1_000_000));
    }

    public static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    // Comparaison en temps constant (ne revele pas combien de caracteres sont corrects)
    public static boolean egal(String hashA, String hashB) {
        return MessageDigest.isEqual(hashA.getBytes(StandardCharsets.UTF_8), hashB.getBytes(StandardCharsets.UTF_8));
    }
}
