package tn.enicarthage.gestionsalon;

import java.io.IOException;
import java.sql.Connection;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;

public class Main {

    public static void main(String[] args) {
        if (!testerConnexion()) {
            System.out.println("Verifiez que MySQL est demarre et que database.sql est execute.");
            return;
        }

        // PORT permet de changer le port (ex. 8081 si Oracle occupe deja le 8080)
        int port;
        try {
            port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080").trim());
        } catch (NumberFormatException e) {
            System.out.println("PORT invalide, utilisation de 8080.");
            port = 8080;
        }

        try {
            ApiServer serveur = new ApiServer(port);
            serveur.demarrer();
            System.out.println("Serveur HTTP demarre sur http://localhost:" + port);
            System.out.println("Testez avec Postman : GET http://localhost:" + port + "/events");
        } catch (IOException e) {
            System.out.println("Impossible de demarrer le serveur : " + e.getMessage());
        }
    }

    private static boolean testerConnexion() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Connexion reussie a MySQL.");
            return true;
        } catch (Exception e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
            return false;
        }
    }
}