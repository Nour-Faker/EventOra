package tn.enicarthage.gestionsalon.util;

import java.security.SecureRandom;

public final class CodeBillet {
    // Sans 0/O/1/I : lisible a l'accueil
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    private CodeBillet() {}

    public static String generer() {
        StringBuilder sb = new StringBuilder("SAL-");
        for (int i = 0; i < 10; i++) {
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }
        return sb.toString(); // 32^10 = 2^50 combinaisons, imprevisible
    }
}