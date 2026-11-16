package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InscriptionSessionDAO {

    // Inscrit un visiteur a une session
    public void add(int visiteurId, int sessionId) {
        String sql = "INSERT INTO inscription_session (visiteur_id, session_id) VALUES (?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, visiteurId);
            statement.setInt(2, sessionId);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // Desinscrit un visiteur
    public void remove(int visiteurId, int sessionId) {
        String sql = "DELETE FROM inscription_session WHERE visiteur_id = ? AND session_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, visiteurId);
            statement.setInt(2, sessionId);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // Le visiteur est-il deja inscrit ?
    public boolean exists(int visiteurId, int sessionId) {
        String sql = "SELECT 1 FROM inscription_session WHERE visiteur_id = ? AND session_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, visiteurId);
            statement.setInt(2, sessionId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // Nombre d'inscrits a une session
    public int countBySession(int sessionId) {
        String sql = "SELECT COUNT(*) FROM inscription_session WHERE session_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, sessionId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }

        return 0;
    }
}