package tn.enicarthage.gestionSalon.dao;

import tn.enicarthage.gestionSalon.config.DatabaseConnection;
import tn.enicarthage.gestionSalon.model.Billet;
import tn.enicarthage.gestionSalon.model.StatutBillet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BilletDAO {

    // CREATE (date_achat est remplie par MySQL, agent_id reste NULL)
    public void create(Billet billet) {
        String sql = "INSERT INTO billet (visiteur_id, evenement_id, code_qr, statut) "
                + "VALUES (?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setInt(1, billet.getVisiteurId());
            statement.setInt(2, billet.getEvenementId());
            statement.setString(3, billet.getCodeQR());
            statement.setString(4, billet.getStatut().name());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    billet.setId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ : un billet par son id
    public Billet findById(int id) {
        return findOne("SELECT * FROM billet WHERE id = ?", String.valueOf(id));
    }

    // READ : un billet par son code QR
    public Billet findByCodeQr(String codeQr) {
        return findOne("SELECT * FROM billet WHERE code_qr = ?", codeQr);
    }

    // READ : les billets d'un visiteur
    public List<Billet> findByVisiteurId(int visiteurId) {
        List<Billet> billets = new ArrayList<>();
        String sql = "SELECT * FROM billet WHERE visiteur_id = ? ORDER BY id";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, visiteurId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    billets.add(mapRow(result));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return billets;
    }

    // UPDATE : seuls le statut et l'agent changent
    public void update(Billet billet) {
        String sql = "UPDATE billet SET statut = ?, agent_id = ? WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, billet.getStatut().name());

            if (billet.getAgentId() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, billet.getAgentId());
            }

            statement.setInt(3, billet.getId());
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DELETE
    public void delete(int id) {
        String sql = "DELETE FROM billet WHERE id = ?";

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

    // SELECT d'un seul billet avec un parametre texte
    // (setString fonctionne aussi pour une colonne INT : MySQL convertit)
    private Billet findOne(String sql, String parameter) {
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, parameter);

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

    private Billet mapRow(ResultSet result) throws SQLException {
        Billet billet = new Billet();
        billet.setId(result.getInt("id"));
        billet.setVisiteurId(result.getInt("visiteur_id"));
        billet.setEvenementId(result.getInt("evenement_id"));

        int agentId = result.getInt("agent_id");
        billet.setAgentId(result.wasNull() ? null : agentId);

        billet.setCodeQR(result.getString("code_qr"));
        billet.setDateAchat(result.getObject("date_achat", LocalDateTime.class));
        billet.setStatut(StatutBillet.valueOf(result.getString("statut")));
        return billet;
    }
}