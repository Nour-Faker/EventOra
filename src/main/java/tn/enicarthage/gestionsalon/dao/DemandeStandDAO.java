package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import tn.enicarthage.gestionsalon.model.DemandeStand;
import tn.enicarthage.gestionsalon.model.StatutDemande;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DemandeStandDAO {

    public void create(DemandeStand demande) {
        String sql = "INSERT INTO demande_stand (exposant_id, stand_id, statut) VALUES (?, ?, ?)";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setInt(1, demande.getExposantId());
            statement.setInt(2, demande.getStandId());
            statement.setString(3, demande.getStatut().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    demande.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la creation de la demande", e);
        }
    }

    // CREATE dans la transaction de l'appelant (ne pas fermer la connexion)
    public void create(Connection connection, DemandeStand demande) {
        String sql = "INSERT INTO demande_stand (exposant_id, stand_id, statut) VALUES (?, ?, ?)";
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, demande.getExposantId());
            statement.setInt(2, demande.getStandId());
            statement.setString(3, demande.getStatut().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    demande.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la creation de la demande", e);
        }
    }

    public List<DemandeStand> findAll() {
        return findList("SELECT * FROM demande_stand ORDER BY id", null);
    }

    public DemandeStand findById(int id) {
        String sql = "SELECT * FROM demande_stand WHERE id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapRow(result);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la recherche de la demande " + id, e);
        }
        return null;
    }

    public List<DemandeStand> findByExposantId(int exposantId) {
        return findList("SELECT * FROM demande_stand WHERE exposant_id = ? ORDER BY id",
                exposantId);
    }

    public List<DemandeStand> findByStandId(int standId) {
        return findList("SELECT * FROM demande_stand WHERE stand_id = ? ORDER BY id",
                standId);
    }

    public void update(DemandeStand demande) {
        String sql = "UPDATE demande_stand SET statut = ?, admin_id = ?, motif_refus = ? "
                + "WHERE id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            bindUpdate(statement, demande);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Erreur lors de la modification de la demande " + demande.getId(), e);
        }
    }

    // UPDATE garde : ne reussit que si la demande est encore EN_ATTENTE
    public boolean updateSiEnAttente(Connection connection, DemandeStand demande) {
        String sql = "UPDATE demande_stand SET statut = ?, admin_id = ?, motif_refus = ?, "
                + "date_traitement = NOW() WHERE id = ? AND statut = 'EN_ATTENTE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindUpdate(statement, demande);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Erreur lors de la mise a jour de la demande " + demande.getId(), e);
        }
    }

    public int refuserAutresEnAttente(Connection connection, int standId,
                                      int demandeGardeeId, int adminId) {
        String sql = "UPDATE demande_stand SET statut = 'REFUSEE', admin_id = ?, motif_refus = ?, "
                + "date_traitement = NOW() WHERE stand_id = ? AND id <> ? AND statut = 'EN_ATTENTE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, adminId);
            statement.setString(2, "Stand attribue a un autre exposant");
            statement.setInt(3, standId);
            statement.setInt(4, demandeGardeeId);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Erreur lors du refus des autres demandes du stand " + standId, e);
        }
    }

    public int countEnAttenteByStand(Connection connection, int standId) {
        String sql = "SELECT COUNT(*) FROM demande_stand WHERE stand_id = ? AND statut = 'EN_ATTENTE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, standId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Erreur lors du comptage des demandes du stand " + standId, e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM demande_stand WHERE id = ?";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la suppression de la demande " + id, e);
        }
    }

    private void bindUpdate(PreparedStatement statement, DemandeStand demande)
            throws SQLException {
        statement.setString(1, demande.getStatut().name());
        if (demande.getAdminId() == null) {
            statement.setNull(2, Types.INTEGER);
        } else {
            statement.setInt(2, demande.getAdminId());
        }
        statement.setString(3, demande.getMotifRefus());
        statement.setInt(4, demande.getId());
    }

    private List<DemandeStand> findList(String sql, Integer parameter) {
        List<DemandeStand> demandes = new ArrayList<>();
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            if (parameter != null) {
                statement.setInt(1, parameter);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    demandes.add(mapRow(result));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la lecture des demandes", e);
        }
        return demandes;
    }
    public boolean existeEnAttente(Connection connection, int exposantId, int standId) {
        String sql = "SELECT 1 FROM demande_stand "
                + "WHERE exposant_id = ? AND stand_id = ? AND statut = 'EN_ATTENTE' LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, exposantId);
            statement.setInt(2, standId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Erreur lors de la verification des demandes du stand " + standId, e);
        }
    }

    private DemandeStand mapRow(ResultSet result) throws SQLException {
        DemandeStand demande = new DemandeStand();
        demande.setId(result.getInt("id"));
        demande.setExposantId(result.getInt("exposant_id"));
        demande.setStandId(result.getInt("stand_id"));
        int adminId = result.getInt("admin_id");
        demande.setAdminId(result.wasNull() ? null : adminId);
        demande.setDateDemande(result.getObject("date_demande", LocalDateTime.class));
        demande.setStatut(StatutDemande.valueOf(result.getString("statut")));
        demande.setMotifRefus(result.getString("motif_refus"));
        return demande;
    }
}