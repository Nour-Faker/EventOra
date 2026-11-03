package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import tn.enicarthage.gestionsalon.model.Categorie;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategorieDAO {

    // CREATE : INSERT
    public void create(Categorie categorie) {
        String sql = "INSERT INTO categorie (nom, description) VALUES (?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setString(1, categorie.getNom());
            statement.setString(2, categorie.getDescription());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    categorie.setId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la creation de la categorie", e);
        }
    }

    // READ : SELECT de toutes les lignes
    public List<Categorie> findAll() {
        List<Categorie> categories = new ArrayList<>();
        String sql = "SELECT * FROM categorie";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                categories.add(mapRow(result));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la lecture des categories", e);
        }

        return categories;
    }

    // READ : SELECT d'une ligne par son id
    public Categorie findById(int id) {
        String sql = "SELECT * FROM categorie WHERE id = ?";

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
            throw new DataAccessException("Erreur lors de la recherche de la categorie " + id, e);
        }

        return null;
    }

    // UPDATE
    public void update(Categorie categorie) {
        String sql = "UPDATE categorie SET nom = ?, description = ? WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, categorie.getNom());
            statement.setString(2, categorie.getDescription());
            statement.setInt(3, categorie.getId());
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la modification de la categorie " + categorie.getId(), e);
        }
    }

    // DELETE
    public void delete(int id) {
        String sql = "DELETE FROM categorie WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la suppression de la categorie " + id, e);
        }
    }

    // Convertit la ligne courante du ResultSet en objet Categorie
    private Categorie mapRow(ResultSet result) throws SQLException {
        Categorie categorie = new Categorie();
        categorie.setId(result.getInt("id"));
        categorie.setNom(result.getString("nom"));
        categorie.setDescription(result.getString("description"));
        return categorie;
    }
}