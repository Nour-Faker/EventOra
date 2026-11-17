package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StandVueDAO {

    public record StandVue(int id, String numero, String emplacement, BigDecimal superficie,
                           BigDecimal prix, String statut, String categorie) {}

    public record DemandeVue(int id, String stand, String evenement, String exposant,
                             String statut, String motifRefus, LocalDateTime dateDemande,
                             BigDecimal prix) {}

    private static final String SELECT_DEMANDE =
            "SELECT d.id, s.numero, e.titre, ex.nom_entreprise, d.statut, d.motif_refus, "
                    + "d.date_demande, s.prix "
                    + "FROM demande_stand d "
                    + "JOIN stand s ON s.id = d.stand_id "
                    + "JOIN evenement e ON e.id = s.evenement_id "
                    + "JOIN exposant ex ON ex.id = d.exposant_id ";

    public List<StandVue> listerStands(int evenementId) {
        String sql = "SELECT s.id, s.numero, s.emplacement, s.superficie, s.prix, s.statut, c.nom "
                + "FROM stand s JOIN categorie c ON c.id = s.categorie_id "
                + "WHERE s.evenement_id = ? ORDER BY s.numero";
        List<StandVue> liste = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, evenementId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    liste.add(new StandVue(r.getInt(1), r.getString(2), r.getString(3),
                            r.getBigDecimal(4), r.getBigDecimal(5), r.getString(6), r.getString(7)));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return liste;
    }

    public StandVue trouverStand(int standId) {
        String sql = "SELECT s.id, s.numero, s.emplacement, s.superficie, s.prix, s.statut, c.nom "
                + "FROM stand s JOIN categorie c ON c.id = s.categorie_id WHERE s.id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, standId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                return new StandVue(r.getInt(1), r.getString(2), r.getString(3),
                        r.getBigDecimal(4), r.getBigDecimal(5), r.getString(6), r.getString(7));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    public List<DemandeVue> demandesDeLExposant(int exposantId) {
        return demandes("WHERE d.exposant_id = ? ORDER BY d.id DESC", exposantId);
    }

    public List<DemandeVue> toutesLesDemandes() {
        return demandes("ORDER BY d.statut = 'EN_ATTENTE' DESC, d.date_demande DESC", null);
    }

    public DemandeVue trouverDemande(int id) {
        List<DemandeVue> l = demandes("WHERE d.id = ?", id);
        return l.isEmpty() ? null : l.get(0);
    }

    // Dans la transaction de l'appelant : le stand devient RESERVE avec son proprietaire
    public void reserver(Connection c, int standId, int exposantId) {
        String sql = "UPDATE stand SET statut = 'RESERVE', exposant_id = ? WHERE id = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, exposantId);
            s.setInt(2, standId);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    private List<DemandeVue> demandes(String suffixe, Integer param) {
        List<DemandeVue> liste = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(SELECT_DEMANDE + suffixe)) {
            if (param != null) s.setInt(1, param);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    liste.add(new DemandeVue(r.getInt(1), r.getString(2), r.getString(3),
                            r.getString(4), r.getString(5), r.getString(6),
                            r.getObject(7, LocalDateTime.class), r.getBigDecimal(8)));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return liste;
    }
}
