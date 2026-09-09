package com.delta.bank

import com.delta.bank.bootstrap.DeltaApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Specification

@SpringBootTest(classes = DeltaApplication)
@ContextConfiguration(classes = DeltaApplication)
@ActiveProfiles("test")
abstract class BaseIntegrationSpec extends Specification {

    static final int LOCAL_PG_PORT = 5433
    static final String LOCAL_PG_DB = "delta_test"
    static final String LOCAL_PG_USER = "delta"
    static final String LOCAL_PG_PASS = "delta"

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    JdbcTemplate jdbcTemplate

    def setup() {
        jdbcTemplate.execute('TRUNCATE TABLE bank_transactions RESTART IDENTITY CASCADE')
        jdbcTemplate.execute('TRUNCATE TABLE bank_accounts RESTART IDENTITY CASCADE')
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", { "jdbc:postgresql://localhost:${LOCAL_PG_PORT}/${LOCAL_PG_DB}" })
        registry.add("spring.datasource.username", { LOCAL_PG_USER })
        registry.add("spring.datasource.password", { LOCAL_PG_PASS })
        registry.add("spring.datasource.driverClassName", { "org.postgresql.Driver" })
        registry.add("spring.jpa.hibernate.ddl-auto", { "none" })
        registry.add("spring.flyway.enabled", { "true" })
    }
}
