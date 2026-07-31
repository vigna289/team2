package com.dbtraining.reconx.integration;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class LiquibaseMigrationsIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void liquibase_applied_all_expected_changesets() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    database);
            liquibase.update();

            try (var stmt = connection.prepareStatement("SELECT COUNT(*) FROM databasechangelog")) {
                var resultSet = stmt.executeQuery();
                resultSet.next();
                int changesets = resultSet.getInt(1);
                assertThat(changesets).isGreaterThanOrEqualTo(13);
            }

            try (var stmt = connection.prepareStatement("SELECT COUNT(*) FROM instruments")) {
                var instrumentsResult = stmt.executeQuery();
                instrumentsResult.next();
                int instruments = instrumentsResult.getInt(1);
                assertThat(instruments).isGreaterThanOrEqualTo(10);
            }
        }
    }
}
