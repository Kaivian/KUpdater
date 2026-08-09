package io.github.kaivian.kupdater.core.database.execution;

import io.github.kaivian.kupdater.api.database.DatabaseProvider;
import io.github.kaivian.kupdater.api.database.PersistenceExecutor;
import io.github.kaivian.kupdater.api.database.Repository;
import io.github.kaivian.kupdater.core.database.dialect.DatabaseDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Base JDBC repository helper class for implementing feature-specific repositories.
 *
 * @param <T>  Entity type
 * @param <ID> Entity identifier type
 */
public abstract class AbstractJdbcRepository<T, ID> implements Repository<T, ID> {

    protected final DatabaseProvider provider;
    protected final DatabaseDialect dialect;
    protected final PersistenceExecutor executor;

    protected AbstractJdbcRepository(DatabaseProvider provider, DatabaseDialect dialect, PersistenceExecutor executor) {
        this.provider = provider;
        this.dialect = dialect;
        this.executor = executor;
    }

    /**
     * Functional interface for SQL query callbacks.
     */
    @FunctionalInterface
    public interface ResultSetMapper<R> {
        R map(ResultSet resultSet) throws SQLException;
    }

    /**
     * Functional interface for transactional database operations.
     */
    @FunctionalInterface
    public interface TransactionCallback<R> {
        R doInTransaction(Connection connection) throws SQLException;
    }

    /**
     * Executes a SQL query with parameter binding and result mapping synchronously.
     */
    protected <R> R executeQuery(String sql, PreparedStatementBinder binder, ResultSetMapper<R> mapper) {
        try (Connection conn = provider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(pstmt);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapper.map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a SQL DML update statement synchronously.
     */
    protected int executeUpdate(String sql, PreparedStatementBinder binder) {
        try (Connection conn = provider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(pstmt);
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Executes database operations within an isolated transaction.
     */
    protected <R> R executeInTransaction(TransactionCallback<R> callback) {
        try (Connection conn = provider.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                R result = callback.doInTransaction(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Transaction failed and was rolled back: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database transaction connection error: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface PreparedStatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
