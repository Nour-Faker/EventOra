package tn.enicarthage.gestionsalon.util;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

public final class Transaction {

    private Transaction() {}

    public static void run(Consumer<Connection> work) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.accept(connection);
                connection.commit();
            } catch (RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError); // keep the original error visible
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur de transaction", e);
        }
    }
}