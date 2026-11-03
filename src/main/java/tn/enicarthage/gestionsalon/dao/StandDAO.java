package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.model.Stand;
import tn.enicarthage.gestionsalon.model.StatutStand;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StandDAO {

    // CREATE
    public void create(Stand stand) {
        String sql = "INSERT INTO stand "
                + "(evenement_id, categorie_id, numero, emplacement, superficie, prix, statut) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setInt(1, stand.getEvenementId());
            statement.setInt(2, stand.getCategorieId());
            statement.setString(3, stand.getNumero());
            statement.setString(4, stand.getEmplacement());
            statement.setDouble(5, stand.getSuperficie());
            statement.setBigDecimal(6, stand.getPrix());
            statement.setString(7, stand.getStatut().name());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    stand.setId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("TODO", e);
        }
    }

    // READ : tous les stands
    public List<Stand> findAll() {
        List<Stand> stands = new ArrayList<>();
        String sql = "SELECT * FROM stand";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                stands.add(mapRow(result));
            }

        } catch (SQLException e) {
            throw new DataAccessException("TODO", e);
        }

        return stands;
    }

    // READ : un stand par son id
    public Stand findById(int id) {
        String sql = "SELECT * FROM stand WHERE id = ?";

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
            throw new DataAccessException("TODO", e);
        }

        return null;
    }

    // READ : les stands d'un evenement (utile pour le plan des stands)
    public List<Stand> findByEvenementId(int evenementId) {
        List<Stand> stands = new ArrayList<>();
        String sql = "SELECT * FROM stand WHERE evenement_id = ? ORDER BY numero";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, evenementId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    stands.add(mapRow(result));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("TODO", e);
        }

        return stands;
    }

    // UPDATE
    public void update(Stand stand) {
        String sql = "UPDATE stand SET evenement_id = ?, categorie_id = ?, numero = ?, "
                + "emplacement = ?, superficie = ?, prix = ?, statut = ? "
                + "WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, stand.getEvenementId());
            statement.setInt(2, stand.getCategorieId());
            statement.setString(3, stand.getNumero());
            statement.setString(4, stand.getEmplacement());
            statement.setDouble(5, stand.getSuperficie());
            statement.setBigDecimal(6, stand.getPrix());
            statement.setString(7, stand.getStatut().name());
            statement.setInt(8, stand.getId());
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("TODO", e);
        }
    }

    // DELETE
    public void delete(int id) {
        String sql = "DELETE FROM stand WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("TODO", e);
        }
    }

    // Convertit la ligne courante du ResultSet en objet Stand
    private Stand mapRow(ResultSet result) throws SQLException {
        Stand stand = new Stand();
        stand.setId(result.getInt("id"));
        stand.setEvenementId(result.getInt("evenement_id"));
        stand.setCategorieId(result.getInt("categorie_id"));
        stand.setNumero(result.getString("numero"));
        stand.setEmplacement(result.getString("emplacement"));
        stand.setSuperficie(result.getDouble("superficie"));
        stand.setPrix(result.getBigDecimal("prix"));
        stand.setStatut(StatutStand.valueOf(result.getString("statut")));
        return stand;
    }
}