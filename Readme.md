# Delta Mini Bank

## Cel

Mini-bank na **Java 21**, **Spring Boot 3**, **PostgreSQL w Dockerze** i **Spock/Groovy**.

## Środowiska

### Produkcja

- Docker Compose: `docker-compose.yml`
- PostgreSQL: `localhost:5432`
- Baza: `delta`
- Użytkownik: `delta`
- Hasło: `delta`

### Testy

- Docker Compose: `docker-compose.test.yml`
- PostgreSQL: `localhost:5433`
- Baza: `delta_test`
- Użytkownik: `delta`
- Hasło: `delta`

## Pliki konfiguracyjne Spring

### `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: delta-mini-bank

  datasource:
    url: jdbc:postgresql://localhost:5432/delta
    username: delta
    password: delta
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true

server:
  port: 8080
```

### `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/delta_test
    username: delta
    password: delta
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  flyway:
    enabled: true
```

## Gradle

### `build.gradle`

```groovy
plugins {
    id 'java'
    id 'groovy'
    id 'org.springframework.boot' version '3.4.5'
    id 'io.spring.dependency-management' version '1.1.7'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
    implementation 'org.postgresql:postgresql'

    implementation localGroovy()

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation platform('org.spockframework:spock-bom:2.3-groovy-3.0')
    testImplementation 'org.spockframework:spock-core'
    testImplementation 'org.spockframework:spock-spring'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}

tasks.withType(Test).configureEach {
    testLogging {
        events 'passed', 'failed', 'skipped'
    }
}
```

## Database migration

### `src/main/resources/db/migration/V1__init_accounts.sql`

```sql
create table bank_accounts (
    number varchar(64) primary key,
    owner varchar(128) not null,
    balance numeric(19,2) not null,
    currency varchar(3) not null
);
```

## Aplikacja

### `src/main/java/com/delta/bank/DeltaApplication.java`

```java
package com.delta.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeltaApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeltaApplication.class, args);
    }
}
```

## DBVisualizer

### Profile

| Nazwa | Cel | Host | Port | DB | User |
|---|---|---:|---:|---|---|
| `bank-prod` | produkcja | `localhost` | `5432` | `delta` | `delta` |
| `bank-test` | testy | `localhost` | `5433` | `delta_test` | `delta` |
| `bank-admin` | techniczne / awaryjne | `localhost` | `5432` | `delta` | `postgres` |

## Uruchamianie

### Produkcja

1. Uruchom bazę produkcyjną:

```bash
docker compose up -d
```

2. Uruchom aplikację:

```bash
./gradlew bootRun
```

### Testy integracyjne

1. Uruchom bazę testową:

```bash
docker compose -f docker-compose.test.yml up -d
```

2. Uruchom testy:

```bash
./gradlew test
```

### Zatrzymanie testowej bazy

```bash
docker compose -f docker-compose.test.yml down
```

### Zatrzymanie produkcji

1. Zatrzymaj aplikację Spring Boot:

```bash
Ctrl+C
```

2. Zatrzymaj bazę produkcyjną:

```bash
docker compose down
```

## Uwagi o testach Spock + Spring

- Testy integracyjne na Spring Boot + Spock wymagają bazowej klasy, np. `BaseIntegrationSpec`.
- `BaseIntegrationSpec` ustawia kontekst Springa i dynamiczne właściwości bazy testowej.
- `BankAccountRepositorySpec` dziedziczy po tej klasie i korzysta z testowego Postgresa na `localhost:5433`.
- Testy domenowe nadal mogą zostać zwykłymi specyfikacjami Spock bez Springa.

## DBVisualizer

### Profile

| Nazwa | Cel | Host | Port | DB | User |
|---|---|---:|---:|---|---|
| `bank-prod` | produkcja | `localhost` | `5432` | `delta` | `delta` |
| `bank-test` | testy | `localhost` | `5433` | `delta_test` | `delta` |
| `bank-admin` | techniczne / awaryjne | `localhost` | `5432` | `delta` | `postgres` |

## Java

- Wymagana: **Java 21**
- Zalecane `JAVA_HOME`: `C:\devtools\jdk-21.0.12.1+1`
