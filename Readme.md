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

### Zatrzymywanie instancji (produkcja i test)

Jeżeli Spring Boot nie startuje, a port 8080 jest zajęty, sprawdź, który proces go blokuje:

```powershell
netstat -ano | findstr :8080
```

Następnie zabij konkretny PID:

```powershell
taskkill /PID <PID> /F
```

Jeśli chcesz sprawdzić port testowy 5433:

```powershell
netstat -ano | findstr :5433
```

I zatrzymać kontener testowy:

```powershell
docker compose -f docker-compose.test.yml down
```

Jeżeli potrzebujesz zatrzymać produkcyjną bazę PostgreSQL i przypadkowo w tle działa Spring Boot:

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :5432
docker compose down
```

### Zatrzymanie testowej bazy

```bash
docker compose -f docker-compose.test.yml down
```

### Czyszczenie baz i reset Flyway

Jeśli w bazie testowej lub produkcyjnej zostaną zapisane stare migracje i pojawi się błąd typu `FlywayValidateException`, wykonaj reset środowiska:

```bash
# produkcja
docker compose down -v

# testy
docker compose -f docker-compose.test.yml down -v
```

Jeśli testowa baza nadal ma stare dane po resecie, można ją odtworzyć od zera:

```bash
docker rm -f delta-postgres-test
docker compose -f docker-compose.test.yml up -d --force-recreate --remove-orphans
```

Jeżeli chcesz wyczyścić bazę ręcznie na poziomie PostgreSQL:

```bash
docker exec delta-postgres-test psql -U delta -d delta_test -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

Uwaga to wyczyści wszystkie dane w bazie produkcyjnej i zresetuje historię migracji Flyway.
docker exec delta-postgres psql -U delta -d delta -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

Uwaga: to usunie dane i zresetuje historię migracji Flyway.

### Zatrzymanie produkcji

1. Zatrzymaj aplikację Spring Boot:

```bash
Ctrl+C
```

2. Zatrzymaj bazę produkcyjną:

```bash
docker compose down
```

## Debug checklist

### `JAVA_HOME invalid`

```powershell
$env:JAVA_HOME = "C:\devtools\jdk-21.0.12.1+1"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
```

Jeśli `JAVA_HOME` jest niepoprawne, Gradle i Spring Boot nie uruchomią się.

### `Port 8080 already in use`

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

To oznacza, że inna aplikacja lub poprzednia instancja Spring Boot nadal działa.

### `FlywayValidateException`

Zwykle oznacza, że historia migracji w bazie jest zanieczyszczona lub checksum się nie zgadza.

```powershell
docker compose down -v
docker compose -f docker-compose.test.yml down -v
```

Jeśli nadal problem występuje, zresetuj schemat bazy:

```powershell
docker exec delta-postgres-test psql -U delta -d delta_test -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
docker exec delta-postgres psql -U delta -d delta -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

### `No such file or directory` przy Dockerze

To zwykle oznacza, że kontener nie zdążył się jeszcze uruchomić.

```powershell
docker compose up -d
Start-Sleep -Seconds 5
docker ps
```

LUB sprawdź, czy kontener istnieje:

```powershell
docker ps -a
```

## Swagger / OpenAPI

Projekt może wystawiać dokumentację API przez Swagger UI.

### Dodanie zależności

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5'
```

> Wersja `2.8.x` jest kompatybilna z Spring Boot 3.4.x i Spring 6.2.x.

### Adresy po uruchomieniu aplikacji

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Uwagi

- Swagger jest przydatny do lokalnego testowania i nauki REST API.
- Nie wystawiaj publicznie Swagger UI bez zabezpieczeń w środowisku produkcyjnym.
- Jeżeli pojawia się błąd typu `NoSuchMethodError` lub `ControllerAdviceBean`, najczęściej oznacza to niezgodność wersji Spring / springdoc.

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
