package tn.enicarthage.gestionsalon.util;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

public final class Transaction {

    @FunctionalInterface
    public interface Work<T> {
        T execute(Connection connection);
    }

    private Transaction() {}

    public static <T> T run(Work<T> work) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur de transaction", e);
        }
    }
}