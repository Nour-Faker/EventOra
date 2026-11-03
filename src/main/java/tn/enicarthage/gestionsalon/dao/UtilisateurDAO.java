package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.model.AgentAccueil;
import tn.enicarthage.gestionsalon.model.Administrateur;
import tn.enicarthage.gestionsalon.model.Exposant;
import tn.enicarthage.gestionsalon.model.Role;
import tn.enicarthage.gestionsalon.model.Utilisateur;
import tn.enicarthage.gestionsalon.model.Visiteur;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UtilisateurDAO {

    // Renvoie le role de l'utilisateur, ou null s'il n'existe pas
    public Role findRole(int id) {
        String sql = "SELECT role FROM utilisateur WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Role.valueOf(result.getString("role"));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }

        return null;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM utilisateur WHERE email = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }

    }

    // CREATE : un utilisateur sans table propre (Visiteur, Administrateur)
    public void create(Utilisateur utilisateur) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            insertUtilisateur(connection, utilisateur);
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // CREATE : un exposant = une ligne dans utilisateur ET une ligne dans exposant.
    // Les deux INSERT forment une transaction : tout est enregistre, ou rien.
    public void createExposant(Exposant exposant) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                insertUtilisateur(connection, exposant);

                String sql = "INSERT INTO exposant (id, nom_entreprise, logo_url, description) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, exposant.getId());
                    statement.setString(2, exposant.getNomEntreprise());
                    statement.setString(3, exposant.getLogoUrl());
                    statement.setString(4, exposant.getDescription());
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // READ : un utilisateur par son email, avec les champs propres a son role
    public Utilisateur findByEmail(String email) {
        String sql = "SELECT u.*, e.nom_entreprise, e.logo_url, "
                + "e.description AS exposant_description, a.matricule "
                + "FROM utilisateur u "
                + "LEFT JOIN exposant e ON e.id = u.id "
                + "LEFT JOIN agent_accueil a ON a.id = u.id "
                + "WHERE u.email = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapRow(result);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }

        return null;
    }

    // UPDATE : bloquer ou debloquer un compte
    public void setBloque(int id, boolean bloque) {
        String sql = "UPDATE utilisateur SET est_bloque = ? WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBoolean(1, bloque);
            statement.setInt(2, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // DELETE (supprime aussi la ligne exposant ou agent_accueil : ON DELETE CASCADE)
    public void delete(int id) {
        String sql = "DELETE FROM utilisateur WHERE id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
    }

    // INSERT dans la table utilisateur, sur une connexion deja ouverte
    private void insertUtilisateur(Connection connection, Utilisateur utilisateur)
            throws SQLException {
        String sql = "INSERT INTO utilisateur "
                + "(nom, prenom, email, mot_de_passe_hash, telephone, role, est_bloque) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, utilisateur.getNom());
            statement.setString(2, utilisateur.getPrenom());
            statement.setString(3, utilisateur.getEmail());
            statement.setString(4, utilisateur.getMotDePasseHash());
            statement.setString(5, utilisateur.getTelephone());
            statement.setString(6, utilisateur.getRole().name());
            statement.setBoolean(7, utilisateur.isEstBloque());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    utilisateur.setId(keys.getInt(1));
                }
            }
        }
    }

    // Construit la bonne sous-classe selon le role
    private Utilisateur mapRow(ResultSet result) throws SQLException {
        Role role = Role.valueOf(result.getString("role"));
        Utilisateur utilisateur;

        if (role == Role.EXPOSANT) {
            Exposant exposant = new Exposant();
            exposant.setNomEntreprise(result.getString("nom_entreprise"));
            exposant.setLogoUrl(result.getString("logo_url"));
            exposant.setDescription(result.getString("exposant_description"));
            utilisateur = exposant;
        } else if (role == Role.AGENT_ACCUEIL) {
            AgentAccueil agent = new AgentAccueil();
            agent.setMatricule(result.getString("matricule"));
            utilisateur = agent;
        } else if (role == Role.ADMINISTRATEUR) {
            utilisateur = new Administrateur();
        } else {
            utilisateur = new Visiteur();
        }

        utilisateur.setId(result.getInt("id"));
        utilisateur.setNom(result.getString("nom"));
        utilisateur.setPrenom(result.getString("prenom"));
        utilisateur.setEmail(result.getString("email"));
        utilisateur.setMotDePasseHash(result.getString("mot_de_passe_hash"));
        utilisateur.setTelephone(result.getString("telephone"));
        utilisateur.setRole(role);
        utilisateur.setEstBloque(result.getBoolean("est_bloque"));
        return utilisateur;
    }
}