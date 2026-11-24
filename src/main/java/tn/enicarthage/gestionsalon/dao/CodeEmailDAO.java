package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Codes a 6 chiffres envoyes par email (verification de l'adresse, mot de passe oublie).
 * Seule l'empreinte SHA-256 du code est stockee. Les calculs de date (expiration, delai
 * entre deux envois) se font avec l'horloge de MySQL (NOW()) pour eviter tout decalage
 * de fuseau horaire entre Java et la base.
 */
public class CodeEmailDAO {

    public static final String VERIFICATION = "VERIFICATION";
    public static final String REINITIALISATION = "REINITIALISATION";

    /** Dernier code non utilise : `expire` vaut true si sa date limite est passee. */
    public record CodeActif(int id, String hash, int tentatives, boolean expire) {}

    public void creer(int utilisateurId, String type, String hash, int minutesValidite) {
        String sql = "INSERT INTO code_email (utilisateur_id, type, code_hash, expire_le) "
                + "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE))";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, utilisateurId);
            s.setString(2, type);
            s.setString(3, hash);
            s.setInt(4, minutesValidite);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // Un nouveau code rend les precedents inutilisables
    public void invaliderTous(int utilisateurId, String type) {
        String sql = "UPDATE code_email SET utilise = TRUE "
                + "WHERE utilisateur_id = ? AND type = ? AND utilise = FALSE";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, utilisateurId);
            s.setString(2, type);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // Un code a-t-il ete envoye il y a moins de `secondes` ? (anti-spam)
    public boolean envoyeRecemment(int utilisateurId, String type, int secondes) {
        String sql = "SELECT 1 FROM code_email WHERE utilisateur_id = ? AND type = ? "
                + "AND created_at > DATE_SUB(NOW(), INTERVAL ? SECOND) LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, utilisateurId);
            s.setString(2, type);
            s.setInt(3, secondes);
            try (ResultSet r = s.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    public CodeActif dernierActif(int utilisateurId, String type) {
        String sql = "SELECT id, code_hash, tentatives, expire_le <= NOW() AS expire FROM code_email "
                + "WHERE utilisateur_id = ? AND type = ? AND utilise = FALSE ORDER BY id DESC LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, utilisateurId);
            s.setString(2, type);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                return new CodeActif(r.getInt("id"), r.getString("code_hash"),
                        r.getInt("tentatives"), r.getBoolean("expire"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    public void incrementerTentatives(int id) {
        maj("UPDATE code_email SET tentatives = tentatives + 1 WHERE id = ?", id);
    }

    // Condition dans le WHERE : un code ne peut servir qu'une fois, meme avec deux requetes simultanees
    public boolean marquerUtilise(int id) {
        return maj("UPDATE code_email SET utilise = TRUE WHERE id = ? AND utilise = FALSE", id);
    }

    private boolean maj(String sql, int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            return s.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }
}
