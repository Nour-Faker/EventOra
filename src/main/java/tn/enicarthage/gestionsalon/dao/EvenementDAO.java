package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.model.Evenement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EvenementDAO {

    // CREATE
    public void create(Evenement evenement) {
        String sql = "INSERT INTO evenement (titre, description, date_debut, date_fin, lieu) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setString(1, evenement.getTitre());
            statement.setString(2, evenement.getDescription());
            statement.setDate(3, java.sql.Date.valueOf(evenement.getDateDebut()));
            statement.setDate(4, java.sql.Date.valueOf(evenement.getDateFin()));
            statement.setString(5, evenement.getLieu());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    evenement.setId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ : tous les evenements
    public List<Evenement> findAll() {
        List<Evenement> evenements = new ArrayList<>();
        String sql = "SELECT * FROM evenement ORDER BY date_debut";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                evenements.add(mapRow(result));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return evenements;
    }

    // READ : un evenement par son id
    public Evenement findById(int id) {
        String sql = "SELECT * FROM evenement WHERE id = ?";

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

    // UPDATE
    public void update(Evenement evenement) {
        String sql = "UPDATE evenement SET titre = ?, description = ?, "
                + "date_debut = ?, date_fin = ?, lieu = ? WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, evenement.getTitre());
            statement.setString(2, evenement.getDescription());
            statement.setDate(3, java.sql.Date.valueOf(evenement.getDateDebut()));
            statement.setDate(4, java.sql.Date.valueOf(evenement.getDateFin()));
            statement.setString(5, evenement.getLieu());
            statement.setInt(6, evenement.getId());
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DELETE (supprime aussi ses stands, sessions et billets : ON DELETE CASCADE)
    public void delete(int id) {
        String sql = "DELETE FROM evenement WHERE id = ?";

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

    // Convertit la ligne courante du ResultSet en objet Evenement
    private Evenement mapRow(ResultSet result) throws SQLException {
        Evenement evenement = new Evenement();
        evenement.setId(result.getInt("id"));
        evenement.setTitre(result.getString("titre"));
        evenement.setDescription(result.getString("description"));
        evenement.setDateDebut(result.getDate("date_debut").toLocalDate());
        evenement.setDateFin(result.getDate("date_fin").toLocalDate());
        evenement.setLieu(result.getString("lieu"));
        return evenement;
    }
}