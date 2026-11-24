package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;

import java.math.BigDecimal;
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
                                 String role, boolean bloque, boolean emailVerifie) {}

    // Liste de tous les comptes (jamais le hash du mot de passe)
    public List<UtilisateurVue> utilisateurs() {
        String sql = "SELECT id, nom, prenom, email, role, est_bloque, email_verifie FROM utilisateur ORDER BY id";
        List<UtilisateurVue> liste = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            while (r.next()) {
                liste.add(new UtilisateurVue(r.getInt("id"), r.getString("nom"),
                        r.getString("prenom"), r.getString("email"),
                        r.getString("role"), r.getBoolean("est_bloque"), r.getBoolean("email_verifie")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return liste;
    }

    public record RemplissageSession(int id, String titre, String evenement,
                                     int inscrits, int capacite, double taux) {}

    // Tableau de bord (BF-34) : billets vendus, chiffre d'affaires, stands reserves,
    // demandes en attente, taux de remplissage des sessions, utilisateurs par role
    public Map<String, Object> stats() {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM evenement), "
                + "(SELECT COUNT(*) FROM utilisateur), "
                + "(SELECT COUNT(*) FROM billet WHERE statut <> 'ANNULE'), "
                + "(SELECT COUNT(*) FROM billet WHERE statut = 'UTILISE'), "
                + "(SELECT COALESCE(SUM(prix_paye), 0) FROM billet WHERE statut <> 'ANNULE'), "
                + "(SELECT COUNT(*) FROM stand WHERE statut = 'RESERVE'), "
                + "(SELECT COALESCE(SUM(prix), 0) FROM stand WHERE statut = 'RESERVE'), "
                + "(SELECT COUNT(*) FROM demande_stand WHERE statut = 'EN_ATTENTE')";
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.getConnection()) {
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(sql)) {
                if (r.next()) {
                    BigDecimal caBillets = r.getBigDecimal(5);
                    BigDecimal caStands = r.getBigDecimal(7);
                    stats.put("evenements", r.getInt(1));
                    stats.put("utilisateurs", r.getInt(2));
                    stats.put("billets", r.getInt(3));
                    stats.put("presents", r.getInt(4));
                    stats.put("chiffreAffairesBillets", caBillets);
                    stats.put("standsReserves", r.getInt(6));
                    stats.put("chiffreAffairesStands", caStands);
                    stats.put("chiffreAffaires", caBillets.add(caStands));
                    stats.put("demandesEnAttente", r.getInt(8));
                }
            }
            stats.put("utilisateursParRole", utilisateursParRole(c));
            stats.put("remplissageSessions", remplissageSessions(c));
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return stats;
    }

    private Map<String, Integer> utilisateursParRole(Connection c) throws SQLException {
        Map<String, Integer> parRole = new LinkedHashMap<>();
        for (String role : new String[]{"ADMINISTRATEUR", "EXPOSANT", "VISITEUR", "AGENT_ACCUEIL"}) {
            parRole.put(role, 0);
        }
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT role, COUNT(*) FROM utilisateur GROUP BY role")) {
            while (r.next()) {
                parRole.put(r.getString(1), r.getInt(2));
            }
        }
        return parRole;
    }

    private List<RemplissageSession> remplissageSessions(Connection c) throws SQLException {
        String sql = "SELECT se.id, se.titre, e.titre, COUNT(i.id), se.capacite_max "
                + "FROM session_evenement se "
                + "JOIN evenement e ON e.id = se.evenement_id "
                + "LEFT JOIN inscription_session i ON i.session_id = se.id "
                + "GROUP BY se.id, se.titre, e.titre, se.capacite_max, se.heure_debut "
                + "ORDER BY se.heure_debut";
        List<RemplissageSession> liste = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                int inscrits = r.getInt(4);
                int capacite = r.getInt(5);
                double taux = capacite > 0 ? Math.round(1000.0 * inscrits / capacite) / 10.0 : 0;
                liste.add(new RemplissageSession(r.getInt(1), r.getString(2), r.getString(3),
                        inscrits, capacite, taux));
            }
        }
        return liste;
    }

    // ---------- Suppression d'un utilisateur (BF-07) ----------

    // Billets du visiteur (annules compris) : FK en RESTRICT, on bloque le compte a la place
    public int compterBillets(int utilisateurId) {
        return compter("SELECT COUNT(*) FROM billet WHERE visiteur_id = ?", utilisateurId);
    }

    // Un stand reserve a toujours un exposant : on ne supprime pas son proprietaire
    public int compterStandsReserves(int exposantId) {
        return compter("SELECT COUNT(*) FROM stand WHERE exposant_id = ? AND statut = 'RESERVE'", exposantId);
    }

    private int compter(String sql, int param) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, param);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
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
