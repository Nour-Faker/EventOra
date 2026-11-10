package tn.enicarthage.gestionSalon.dao;

import tn.enicarthage.gestionSalon.config.DatabaseConnection;
import tn.enicarthage.gestionSalon.model.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SessionDAO {

    // CREATE
    public void create(Session session) {
        String sql = "INSERT INTO session_evenement "
                + "(evenement_id, titre, description, heure_debut, heure_fin, capacite_max) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setInt(1, session.getEvenementId());
            statement.setString(2, session.getTitre());
            statement.setString(3, session.getDescription());
            statement.setObject(4, session.getHeureDebut());
            statement.setObject(5, session.getHeureFin());
            statement.setInt(6, session.getCapaciteMax());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    session.setId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ : toutes les sessions
    public List<Session> findAll() {
        return findList("SELECT * FROM session_evenement ORDER BY heure_debut", null);
    }

    // READ : une session par son id
    public Session findById(int id) {
        String sql = "SELECT * FROM session_evenement WHERE id = ?";

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

    // READ : les sessions d'un evenement (le programme)
    public List<Session> findByEvenementId(int evenementId) {
        return findList("SELECT * FROM session_evenement WHERE evenement_id = ? "
                + "ORDER BY heure_debut", evenementId);
    }

    // READ : les sessions auxquelles un visiteur est inscrit (JOIN sur la table N:N)
    public List<Session> findByVisiteurId(int visiteurId) {
        return findList("SELECT se.* FROM session_evenement se "
                + "JOIN inscription_session i ON i.session_id = se.id "
                + "WHERE i.visiteur_id = ? ORDER BY se.heure_debut", visiteurId);
    }

    // UPDATE
    public void update(Session session) {
        String sql = "UPDATE session_evenement SET evenement_id = ?, titre = ?, "
                + "description = ?, heure_debut = ?, heure_fin = ?, capacite_max = ? "
                + "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, session.getEvenementId());
            statement.setString(2, session.getTitre());
            statement.setString(3, session.getDescription());
            statement.setObject(4, session.getHeureDebut());
            statement.setObject(5, session.getHeureFin());
            statement.setInt(6, session.getCapaciteMax());
            statement.setInt(7, session.getId());
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DELETE (supprime aussi les inscriptions : ON DELETE CASCADE)
    public void delete(int id) {
        String sql = "DELETE FROM session_evenement WHERE id = ?";

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
    private List<Session> findList(String sql, Integer parameter) {
        List<Session> sessions = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            if (parameter != null) {
                statement.setInt(1, parameter);
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    sessions.add(mapRow(result));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sessions;
    }

    private Session mapRow(ResultSet result) throws SQLException {
        Session session = new Session();
        session.setId(result.getInt("id"));
        session.setEvenementId(result.getInt("evenement_id"));
        session.setTitre(result.getString("titre"));
        session.setDescription(result.getString("description"));
        session.setHeureDebut(result.getObject("heure_debut", LocalDateTime.class));
        session.setHeureFin(result.getObject("heure_fin", LocalDateTime.class));
        session.setCapaciteMax(result.getInt("capacite_max"));
        return session;
    }
}