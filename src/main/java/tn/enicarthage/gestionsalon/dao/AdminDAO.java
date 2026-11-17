package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminDAO {

    public record UtilisateurVue(int id, String nom, String prenom, String email,
                                 String role, boolean bloque) {}

    // Liste de tous les comptes (jamais le hash du mot de passe)
    public List<UtilisateurVue> utilisateurs() {
        String sql = "SELECT id, nom, prenom, email, role, est_bloque FROM utilisateur ORDER BY id";
        List<UtilisateurVue> liste = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            while (r.next()) {
                liste.add(new UtilisateurVue(r.getInt("id"), r.getString("nom"),
                        r.getString("prenom"), r.getString("email"),
                        r.getString("role"), r.getBoolean("est_bloque")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return liste;
    }

    // Chiffres du tableau de bord (une seule requete)
    public Map<String, Integer> stats() {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM evenement), "
                + "(SELECT COUNT(*) FROM utilisateur), "
                + "(SELECT COUNT(*) FROM billet WHERE statut <> 'ANNULE'), "
                + "(SELECT COUNT(*) FROM billet WHERE statut = 'UTILISE'), "
                + "(SELECT COUNT(*) FROM stand WHERE statut = 'RESERVE'), "
                + "(SELECT COUNT(*) FROM demande_stand WHERE statut = 'EN_ATTENTE')";
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            if (r.next()) {
                stats.put("evenements", r.getInt(1));
                stats.put("utilisateurs", r.getInt(2));
                stats.put("billets", r.getInt(3));
                stats.put("presents", r.getInt(4));
                stats.put("standsReserves", r.getInt(5));
                stats.put("demandesEnAttente", r.getInt(6));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return stats;
    }

    public boolean compteActif(int id) {
        return roleActif(id) != null;
    }

    // Role si le compte existe et n'est pas bloque, sinon null
    public String roleActif(int id) {
        String sql = "SELECT role FROM utilisateur WHERE id = ? AND est_bloque = FALSE";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, id);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getString("role") : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }
}
