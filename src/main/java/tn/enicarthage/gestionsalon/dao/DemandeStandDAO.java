package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
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

    // CREATE (la date est remplie par MySQL : DEFAULT CURRENT_TIMESTAMP)
    public void create(DemandeStand demande) {
        String sql = "INSERT INTO demande_stand (exposant_id, stand_id, statut) "
                + "VALUES (?, ?, ?)";

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
            e.printStackTrace();
        }
    }

    // READ : toutes les demandes
    public List<DemandeStand> findAll() {
        return findList("SELECT * FROM demande_stand ORDER BY id", null);
    }

    // READ : une demande par son id
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
            e.printStackTrace();
        }

        return null;
    }

    // READ : les demandes d'un exposant
    public List<DemandeStand> findByExposantId(int exposantId) {
        return findList("SELECT * FROM demande_stand WHERE exposant_id = ? ORDER BY id",
                exposantId);
    }

    // READ : les demandes portant sur un stand
    public List<DemandeStand> findByStandId(int standId) {
        return findList("SELECT * FROM demande_stand WHERE stand_id = ? ORDER BY id",
                standId);
    }

    // UPDATE : seuls le statut, l'admin et le motif changent
    public void update(DemandeStand demande) {
        String sql = "UPDATE demande_stand SET statut = ?, admin_id = ?, motif_refus = ? "
                + "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, demande.getStatut().name());

            if (demande.getAdminId() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, demande.getAdminId());
            }

            statement.setString(3, demande.getMotifRefus());
            statement.setInt(4, demande.getId());
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DELETE
    public void delete(int id) {
        String sql = "DELETE FROM demande_stand WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Execute un SELECT renvoyant plusieurs lignes (avec ou sans parametre entier)
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
            e.printStackTrace();
        }

        return demandes;
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