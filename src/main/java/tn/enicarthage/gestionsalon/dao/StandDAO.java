package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.Stand;
import tn.enicarthage.gestionsalon.model.StatutStand;

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
            throw new DataAccessException(
                    "Erreur lors de la creation du stand " + stand.getNumero(), e);
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
            throw new DataAccessException("Erreur lors de la lecture des stands", e);
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
            throw new DataAccessException("Erreur lors de la recherche du stand " + id, e);
        }

        return null;
    }

    // READ : les stands d'un evenement (plan des stands)
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
            throw new DataAccessException(
                    "Erreur lors de la lecture des stands de l'evenement " + evenementId, e);
        }

        return stands;
    }

    // READ avec verrou : la ligne reste verrouillee jusqu'au commit/rollback.
    // Utilise la Connection de l'appelant : ne PAS la fermer ici.
    public Stand findByIdForUpdate(Connection connection, int id) {
        String sql = "SELECT * FROM stand WHERE id = ? FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapRow(result) : null;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors du verrouillage du stand " + id, e);
        }
    }

    // UPDATE : le statut n'est PAS modifiable ici.
    // Il change uniquement via les demandes (updateStatut dans une transaction).
    public void update(Stand stand) {
        String sql = "UPDATE stand SET evenement_id = ?, categorie_id = ?, numero = ?, "
                + "emplacement = ?, superficie = ?, prix = ? "
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
            statement.setInt(7, stand.getId());
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Erreur lors de la modification du stand " + stand.getId(), e);
        }
    }

    // UPDATE du statut seul, dans la transaction de l'appelant.
    // Ne PAS fermer la connexion ici.
    public void updateStatut(Connection connection, int standId, StatutStand statut) {
        String sql = "UPDATE stand SET statut = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, statut.name());
            statement.setInt(2, standId);
            if (statement.executeUpdate() != 1) {
                throw new ResourceNotFoundException("Stand introuvable : " + standId);
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Erreur lors du changement de statut du stand " + standId, e);
        }
    }

    // UPDATE garde : ne change le statut que si le stand est encore EN_ATTENTE.
    // Dans la transaction de l'appelant : ne PAS fermer la connexion.
    public void updateStatutSiEnAttente(Connection connection, int standId, StatutStand nouveau) {
        String sql = "UPDATE stand SET statut = ? WHERE id = ? AND statut = 'EN_ATTENTE'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nouveau.name());
            statement.setInt(2, standId);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Erreur lors du changement de statut du stand " + standId, e);
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
            throw new DataAccessException("Erreur lors de la suppression du stand " + id, e);
        }
    }

    // DELETE garde : ne supprime que si le stand est encore LIBRE (condition dans le WHERE,
    // donc pas de fenetre entre la verification et la suppression). Renvoie false sinon.
    public boolean supprimerSiLibre(int id) {
        String sql = "DELETE FROM stand WHERE id = ? AND statut = 'LIBRE'";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la suppression du stand " + id, e);
        }
    }

    // (evenement_id, numero) est UNIQUE : verifie avant pour renvoyer 409 et non 500.
    // exceptId = id a ignorer lors d'une modification (0 pour une creation).
    public boolean existeNumero(int evenementId, String numero, int exceptId) {
        String sql = "SELECT 1 FROM stand WHERE evenement_id = ? AND numero = ? AND id <> ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, evenementId);
            statement.setString(2, numero);
            statement.setInt(3, exceptId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la verification du numero de stand", e);
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
