package tn.enicarthage.gestionsalon.dao;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AdminDAO {

    public record UtilisateurVue(int id, String nom, String email, String role, boolean bloque) {}

    public List<UtilisateurVue> utilisateurs() {
        List<UtilisateurVue> liste = new ArrayList<>();
// Adapted to your DB: nom, email, role, est_bloque
        String sql = "SELECT id, nom, email, role, est_bloque FROM utilisateur";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(new UtilisateurVue(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getBoolean("est_bloque")
                ));
}
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des utilisateurs", e);
}
        return liste;
}

    public Map<String, Integer> stats() {
        Map<String, Integer> stats = new HashMap<>();
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM evenement");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.put("evenements", rs.getInt(1));
}
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM billet");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.put("billets", rs.getInt(1));
}
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM utilisateur");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.put("utilisateurs", rs.getInt(1));
}
        } catch (SQLException e) {
            throw new RuntimeException("Erreur stats", e);
}
        return stats;
}

    public void bloquer(int adminId, int cibleId, boolean bloque) {
        // Adapted to your DB: est_bloque
        String sql = "UPDATE utilisateur SET est_bloque = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, bloque);
            ps.setInt(2, cibleId);
            ps.executeUpdate();
} catch (SQLException e) {
            throw new RuntimeException("Erreur blocage utilisateur", e);
}
    }

    public boolean compteActif(int id) {
        // Adapted to your DB: est_bloque
        String sql = "SELECT est_bloque FROM utilisateur WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return !rs.getBoolean("est_bloque");
}
                return false;
}
        } catch (SQLException e) {
            throw new RuntimeException("Erreur compteActif", e);
}
    }

    public String roleActif(int id) {
        // Adapted to your DB: est_bloque
        String sql = "SELECT role FROM utilisateur WHERE id = ? AND est_bloque = FALSE";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("role") : null;
}
        } catch (SQLException e) {
            throw new RuntimeException("Erreur roleActif pour id=" + id, e);
}
    }
}