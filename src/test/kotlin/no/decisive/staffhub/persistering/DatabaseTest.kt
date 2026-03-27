package no.decisive.staffhub.persistering

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@Testcontainers
abstract class DatabaseTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    protected lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun setUp() {
        val dataSource = DriverManagerDataSource(
            postgres.jdbcUrl,
            postgres.username,
            postgres.password
        )
        jdbc = JdbcTemplate(dataSource)

        Flyway.configure()
            .dataSource(dataSource)
            .cleanDisabled(false)
            .load()
            .also { it.clean() }
            .also { it.migrate() }
    }
}
