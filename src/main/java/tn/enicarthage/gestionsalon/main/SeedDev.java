package tn.enicarthage.gestionsalon.main;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Usage : definir SEED_PASSWORD (8 caracteres minimum) puis lancer ce main.
public class SeedDev {

    public static void main(String[] args) throws Exception {
        String motDePasse = System.getenv("SEED_PASSWORD");
        if (motDePasse == null || motDePasse.length() < 8) {
            System.out.println("Definissez SEED_PASSWORD (8 caracteres minimum).");
            return;
        }
        String hash = PasswordUtil.hash(motDePasse);

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            creerUtilisateur(c, "Admin", "Salon", "admin@salon.tn", "ADMINISTRATEUR", hash);
            int agentId = creerUtilisateur(c, "Agent", "Accueil", "agent@salon.tn", "AGENT_ACCUEIL", hash);
            try (PreparedStatement s = c.prepareStatement(
                    "INSERT IGNORE INTO agent_accueil (id, matricule) VALUES (?, 'AG-001')")) {
                s.setInt(1, agentId);
                s.executeUpdate();
            }
            c.commit();
            System.out.println("Admin (admin@salon.tn) et agent (agent@salon.tn) prets.");
        }
    }

    private static int creerUtilisateur(Connection c, String nom, String prenom, String email,
                                        String role, String hash) throws Exception {
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO utilisateur (nom, prenom, email, mot_de_passe_hash, role) "
                        + "VALUES (?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE mot_de_passe_hash = VALUES(mot_de_passe_hash)")) {
            s.setString(1, nom);
            s.setString(2, prenom);
            s.setString(3, email);
            s.setString(4, hash);
            s.setString(5, role);
            s.executeUpdate();
        }
        try (PreparedStatement s = c.prepareStatement("SELECT id FROM utilisateur WHERE email = ?")) {
            s.setString(1, email);
            try (ResultSet r = s.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }
}