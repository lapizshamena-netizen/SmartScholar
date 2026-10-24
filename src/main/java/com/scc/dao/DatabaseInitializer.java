
package com.scc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {

        String createUsers = """
            CREATE TABLE IF NOT EXISTS Users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                department TEXT,
                status TEXT NOT NULL DEFAULT 'PENDING'
            )
            """;

        try (
                Connection connection = DatabaseConnection.connect();
                Statement statement = connection.createStatement()
        ) {
            statement.execute(createUsers);

            // Fix an existing database that has UNIQUE(full_name).
            if (hasUniqueFullName(connection)) {
                System.out.println(
                        "[Database] Fixing Users.full_name constraint..."
                );

                migrateUsersTable(connection);

                System.out.println(
                        "[Database] Users table migrated successfully."
                );
            } else if (!hasColumn(connection, "Users", "status")) {
                // Add status to older databases without deleting records.
                statement.execute("""
                    ALTER TABLE Users
                    ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'
                    """);

                System.out.println(
                        "[Database] Added Users.status column."
                );
            }

            // Create Scholarships table.
            statement.execute("""
                CREATE TABLE IF NOT EXISTS Scholarships (
                    scholarship_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    grant_amount REAL NOT NULL,
                    min_gpa REAL NOT NULL,
                    available_slots INTEGER NOT NULL,
                    deadline TEXT NOT NULL,
                    created_by INTEGER,
                    FOREIGN KEY (created_by)
                        REFERENCES Users(user_id)
                )
                """);

            // Create Application table.
            statement.execute("""
                CREATE TABLE IF NOT EXISTS Application (
                    application_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scholarship_id INTEGER,
                    student_id INTEGER,
                    evaluator_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    gpa_submitted REAL,
                    submission_date TEXT,
                    FOREIGN KEY (scholarship_id)
                        REFERENCES Scholarships(scholarship_id),
                    FOREIGN KEY (student_id)
                        REFERENCES Users(user_id),
                    FOREIGN KEY (evaluator_id)
                        REFERENCES Users(user_id)
                )
                """);

            System.out.println(
                    "[Database] All tables initialized successfully."
            );
        }
    }

    private static boolean hasColumn(
            Connection connection,
            String table,
            String column
    ) throws SQLException {

        try (
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "PRAGMA table_info('" + table + "')"
                )
        ) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasUniqueFullName(
            Connection connection
    ) throws SQLException {

        try (
                Statement statement = connection.createStatement();
                ResultSet indexes = statement.executeQuery(
                        "PRAGMA index_list('Users')"
                )
        ) {
            while (indexes.next()) {
                if (indexes.getInt("unique") != 1) {
                    continue;
                }

                String indexName = indexes.getString("name");

                // Index names come from SQLite metadata.
                String escapedName = indexName.replace("'", "''");

                try (
                        Statement indexStatement =
                                connection.createStatement();
                        ResultSet columns = indexStatement.executeQuery(
                                "PRAGMA index_info('" + escapedName + "')"
                        )
                ) {
                    int columnCount = 0;
                    boolean containsFullName = false;

                    while (columns.next()) {
                        columnCount++;

                        if ("full_name".equalsIgnoreCase(
                                columns.getString("name"))) {
                            containsFullName = true;
                        }
                    }

                    // Only migrate a unique index on full_name alone.
                    if (columnCount == 1 && containsFullName) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void migrateUsersTable(
            Connection connection
    ) throws SQLException {

        // Foreign-key enforcement must be disabled before the transaction.
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
        }

        boolean committed = false;

        try {
            connection.setAutoCommit(false);

            boolean hasStatus =
                    hasColumn(connection, "Users", "status");

            try (Statement statement = connection.createStatement()) {

                statement.execute("""
                    CREATE TABLE Users_new (
                        user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        full_name TEXT NOT NULL,
                        email TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        role TEXT NOT NULL,
                        department TEXT,
                        status TEXT NOT NULL DEFAULT 'PENDING'
                    )
                    """);

                String statusExpression = hasStatus
                        ? "COALESCE(status, 'PENDING')"
                        : "'PENDING'";

                String copyUsers = """
                    INSERT INTO Users_new (
                        user_id,
                        full_name,
                        email,
                        password_hash,
                        role,
                        department,
                        status
                    )
                    SELECT
                        user_id,
                        full_name,
                        email,
                        password_hash,
                        role,
                        department,
                        %s
                    FROM Users
                    """.formatted(statusExpression);

                statement.executeUpdate(copyUsers);

                statement.execute("DROP TABLE Users");

                statement.execute(
                        "ALTER TABLE Users_new RENAME TO Users"
                );
            }

            connection.commit();
            committed = true;

        } catch (SQLException e) {
            connection.rollback();
            throw e;

        } finally {
            try {
                connection.setAutoCommit(true);

                try (Statement statement =
                             connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = ON");
                }

            } catch (SQLException restoreError) {
                if (committed) {
                    throw restoreError;
                }
            }
        }
    }
}
