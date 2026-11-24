package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BilletDAO {

    public record TypeVerrouille(int id, int evenementId, BigDecimal prix, int quota, LocalDate dateFin) {}
    public record TypeBilletVue(int id, String nom, BigDecimal prix, int quota, int restants) {}
    public record BilletVue(int id, String codeQr, String statut, String typeBillet,
                            BigDecimal prixPaye, String titre, LocalDate dateDebut, String lieu) {}
    public record ScanVue(int id, String statut, String visiteur, String titre,
                          String typeBillet, LocalDateTime dateScan) {}

    private static final String SELECT_VUE =
            "SELECT b.id, b.code_qr, b.statut, t.nom AS type_billet, b.prix_paye, "
                    + "e.titre, e.date_debut, e.lieu "
                    + "FROM billet b "
                    + "JOIN type_billet t ON t.id = b.type_billet_id "
                    + "JOIN evenement e ON e.id = t.evenement_id ";

    // ---------- Dans la transaction d'achat (connexion fournie par le service) ----------

    // Verrouille la ligne type_billet jusqu'au commit
    public TypeVerrouille verrouillerType(Connection c, int typeId) {
        String sql = "SELECT t.id, t.evenement_id, t.prix, t.quota, e.date_fin "
                + "FROM type_billet t JOIN evenement e ON e.id = t.evenement_id "
                + "WHERE t.id = ? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, typeId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                return new TypeVerrouille(r.getInt("id"), r.getInt("evenement_id"),
                        r.getBigDecimal("prix"), r.getInt("quota"),
                        r.getObject("date_fin", LocalDate.class));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    public int compterActifs(Connection c, int typeId) {
        return compter(c,
                "SELECT COUNT(*) FROM billet WHERE type_billet_id = ? AND statut <> 'ANNULE'",
                typeId);
    }

    public boolean existeActifPourEvenement(Connection c, int visiteurId, int evenementId) {
        return compter(c,
                "SELECT COUNT(*) FROM billet b JOIN type_billet t ON t.id = b.type_billet_id "
                        + "WHERE b.visiteur_id = ? AND t.evenement_id = ? AND b.statut <> 'ANNULE'",
                visiteurId, evenementId) > 0;
    }

    public int inserer(Connection c, int visiteurId, int typeId, String code, BigDecimal prix) {
        String sql = "INSERT INTO billet (visiteur_id, type_billet_id, code_qr, prix_paye) VALUES (?, ?, ?, ?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, visiteurId);
            s.setInt(2, typeId);
            s.setString(3, code);
            s.setBigDecimal(4, prix);
            s.executeUpdate();
            try (ResultSet k = s.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // ---------- Lectures ----------

    public List<TypeBilletVue> listerTypes(int evenementId) {
        String sql = "SELECT t.id, t.nom, t.prix, t.quota, t.quota - COUNT(b.id) AS restants "
                + "FROM type_billet t LEFT JOIN billet b ON b.type_billet_id = t.id AND b.statut <> 'ANNULE' "
                + "WHERE t.evenement_id = ? GROUP BY t.id, t.nom, t.prix, t.quota ORDER BY t.prix";
        List<TypeBilletVue> liste = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, evenementId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    liste.add(new TypeBilletVue(r.getInt("id"), r.getString("nom"),
                            r.getBigDecimal("prix"), r.getInt("quota"), r.getInt("restants")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return liste;
    }

    public List<BilletVue> trouverParVisiteur(int visiteurId) {
        return vues("WHERE b.visiteur_id = ? ORDER BY b.id DESC", visiteurId);
    }

    public BilletVue trouverVue(int billetId) {
        List<BilletVue> l = vues("WHERE b.id = ?", billetId);
        return l.isEmpty() ? null : l.get(0);
    }

    public ScanVue infoScan(String code) {
        String sql = "SELECT b.id, b.statut, u.prenom, u.nom, e.titre, t.nom AS type_billet, b.date_scan "
                + "FROM billet b "
                + "JOIN utilisateur u ON u.id = b.visiteur_id "
                + "JOIN type_billet t ON t.id = b.type_billet_id "
                + "JOIN evenement e ON e.id = t.evenement_id WHERE b.code_qr = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, code);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                return new ScanVue(r.getInt("id"), r.getString("statut"),
                        r.getString("prenom") + " " + r.getString("nom"), r.getString("titre"),
                        r.getString("type_billet"), r.getObject("date_scan", LocalDateTime.class));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // ---------- Ecritures atomiques : la condition est DANS le WHERE ----------

    // Proprietaire + etat verifies en une seule instruction
    public boolean annuler(int visiteurId, int billetId) {
        return maj("UPDATE billet SET statut = 'ANNULE' WHERE id = ? AND visiteur_id = ? AND statut = 'VALIDE'",
                billetId, visiteurId);
    }

    // Un seul scan peut gagner, meme avec deux agents en parallele
    public boolean marquerUtilise(int agentId, String code) {
        String sql = "UPDATE billet SET statut = 'UTILISE', agent_id = ?, date_scan = NOW() "
                + "WHERE code_qr = ? AND statut = 'VALIDE'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, agentId);
            s.setString(2, code);
            return s.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // ---------- Gestion des types de billets (administrateur) ----------

    public int creerType(int evenementId, String nom, BigDecimal prix, int quota) {
        String sql = "INSERT INTO type_billet (evenement_id, nom, prix, quota) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, evenementId);
            s.setString(2, nom);
            s.setBigDecimal(3, prix);
            s.setInt(4, quota);
            s.executeUpdate();
            try (ResultSet k = s.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    public boolean modifierType(int typeId, String nom, BigDecimal prix, int quota) {
        String sql = "UPDATE type_billet SET nom = ?, prix = ?, quota = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, nom);
            s.setBigDecimal(2, prix);
            s.setInt(3, quota);
            s.setInt(4, typeId);
            return s.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    public boolean supprimerType(int typeId) {
        return maj("DELETE FROM type_billet WHERE id = ?", typeId);
    }

    // Evenement du type, ou null si le type n'existe pas
    public Integer evenementDuType(int typeId) {
        String sql = "SELECT evenement_id FROM type_billet WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, typeId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // (evenement_id, nom) est UNIQUE : verifie avant pour renvoyer 409 et non 500
    public boolean existeNomType(int evenementId, String nom, int exceptId) {
        return compterSimple("SELECT COUNT(*) FROM type_billet WHERE evenement_id = ? AND nom = ? AND id <> ?",
                evenementId, nom, exceptId) > 0;
    }

    // Billets non annules (places deja prises)
    public int compterVendus(int typeId) {
        return compterSimple("SELECT COUNT(*) FROM billet WHERE type_billet_id = ? AND statut <> 'ANNULE'",
                typeId);
    }

    // Tous les billets, annules compris (la FK billet -> type_billet est RESTRICT)
    public int compterTous(int typeId) {
        return compterSimple("SELECT COUNT(*) FROM billet WHERE type_billet_id = ?", typeId);
    }

    public TypeBilletVue trouverTypeVue(int typeId) {
        String sql = "SELECT t.id, t.nom, t.prix, t.quota, t.quota - COUNT(b.id) AS restants "
                + "FROM type_billet t LEFT JOIN billet b ON b.type_billet_id = t.id AND b.statut <> 'ANNULE' "
                + "WHERE t.id = ? GROUP BY t.id, t.nom, t.prix, t.quota";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, typeId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                return new TypeBilletVue(r.getInt("id"), r.getString("nom"),
                        r.getBigDecimal("prix"), r.getInt("quota"), r.getInt("restants"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // ---------- Regles inter-tables ----------

    // Billet non annule du visiteur pour cet evenement (inscription a une session)
    public boolean possedeBilletActif(int visiteurId, int evenementId) {
        return compterSimple("SELECT COUNT(*) FROM billet b JOIN type_billet t ON t.id = b.type_billet_id "
                + "WHERE b.visiteur_id = ? AND t.evenement_id = ? AND b.statut <> 'ANNULE'",
                visiteurId, evenementId) > 0;
    }

    // Tous les billets d'un evenement, annules compris (suppression de l'evenement)
    public int compterParEvenement(int evenementId) {
        return compterSimple("SELECT COUNT(*) FROM billet b JOIN type_billet t ON t.id = b.type_billet_id "
                + "WHERE t.evenement_id = ?", evenementId);
    }

    // Billet du visiteur uniquement (PDF) : null si le billet n'existe pas ou appartient a un autre
    public BilletVue trouverVueDuVisiteur(int visiteurId, int billetId) {
        List<BilletVue> liste = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(SELECT_VUE + "WHERE b.id = ? AND b.visiteur_id = ?")) {
            s.setInt(1, billetId);
            s.setInt(2, visiteurId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    liste.add(new BilletVue(r.getInt("id"), r.getString("code_qr"), r.getString("statut"),
                            r.getString("type_billet"), r.getBigDecimal("prix_paye"), r.getString("titre"),
                            r.getObject("date_debut", LocalDate.class), r.getString("lieu")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return liste.isEmpty() ? null : liste.get(0);
    }

    // ---------- Utilitaires ----------

    // Comme compter(), mais sur sa propre connexion et avec des parametres de types varies
    private int compterSimple(String sql, Object... params) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) s.setObject(i + 1, params[i]);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    private List<BilletVue> vues(String suffixe, int param) {
        List<BilletVue> liste = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(SELECT_VUE + suffixe)) {
            s.setInt(1, param);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    liste.add(new BilletVue(r.getInt("id"), r.getString("code_qr"), r.getString("statut"),
                            r.getString("type_billet"), r.getBigDecimal("prix_paye"), r.getString("titre"),
                            r.getObject("date_debut", LocalDate.class), r.getString("lieu")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return liste;
    }

    private int compter(Connection c, String sql, int... params) {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) s.setInt(i + 1, params[i]);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    private boolean maj(String sql, int... params) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) s.setInt(i + 1, params[i]);
            return s.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }
}
