# Docker dla aplikacji + istniejący PostgreSQL

To jest nota do kroku 18. Opisuje sposób, w jaki uruchamiamy aplikację Spring Boot w kontenerze Docker, a bazę PostgreSQL zostawiamy jako istniejący serwis Docker już uruchomiony na hoście.

## Cel

Uruchomić aplikację jako kontener Docker, ale bez tworzenia pełnego stacku `app + db` w jednym `docker-compose.yml`.

To jest wariant lekkiego i praktycznego podejścia dla obecnego projektu:

- PostgreSQL pozostaje istniejącym serwisem Docker
- aplikacja Java działa w osobnym kontenerze
- aplikacja łączy się z bazą przez `host.docker.internal`

## Dlaczego taki wariant

W projekcie masz już uruchomione bazy PostgreSQL w Dockerze:

- produkcja: `localhost:5432`
- test: `localhost:5433`

Czyli nie ma sensu duplikować tej samej bazy w kolejnym kontenerze. Główny cel kroku 18 to nie „zapakowanie bazy”, tylko „zapakowanie aplikacji” i przygotowanie jej do uruchomienia w kontenerze.

## Pliki

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### .dockerignore

```dockerignore
.git
.gradle
.idea
*.iml
```

## Finalny zestaw komend do zaktualizowania obrazu i uruchomienia aplikacji

### 1) Zatrzymaj poprzedni kontener

```powershell
docker stop delta-app
```

### 2) Zbuduj nowy JAR aplikacji

```powershell
cd C:\dev\delta
./gradlew bootJar
```

### 3) Zbuduj nowy obraz Docker

```powershell
cd C:\dev\delta
docker build -t delta-app .
```

### 4) Uruchom nowy kontener aplikacji podłączony do istniejącej bazy PostgreSQL

```powershell
docker run --rm -d `
  --name delta-app `
  -p 8080:8080 `
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/delta `
  -e SPRING_DATASOURCE_USERNAME=delta `
  -e SPRING_DATASOURCE_PASSWORD=delta `
  delta-app
```

### 5) Sprawdź aplikację

```powershell
curl.exe http://localhost:8080/health
```

Oczekiwany wynik:

```json
{"status":"UP","service":"delta-mini-bank"}
```

### 6) Zatrzymaj kontener

```powershell
docker stop delta-app
```

## Ważne: dlaczego `host.docker.internal`

W Docker Desktop na Windows aplikacja w kontenerze nie ma dostępu do `localhost` hosta w sensie „to jest moja maszyna”, bo `localhost` w kontenerze oznacza sam kontener.

Dlatego używamy:

```text
host.docker.internal
```

To jest poprawny sposób, żeby kontener wyszedł na host i trafił do działającego PostgreSQL na porcie 5432.

## Rozwiązywanie problemów

### 1) `connection refused` do bazy

Upewnij się, że PostgreSQL jest uruchomione:

```powershell
docker ps
```

Sprawdź, czy baza `delta` jest nasłuchująca na porcie 5432.

### 2) `curl` daje ostrzeżenie w PowerShellu

W Windows PowerShell używaj `curl.exe` zamiast zwykłego `curl`:

```powershell
curl.exe http://localhost:8080/health
```

### 3) Błąd checksum migracji Flyway

To nie jest problem Docker. To problem migracji Flyway:

- jakaś migracja została zmieniona po zastosowaniu
- albo baza ma nieaktualny `flyway_schema_history`

Najprościej zresetować bazę lokalnie:

```powershell
psql -U delta -d postgres -c "DROP DATABASE delta;"
psql -U delta -d postgres -c "CREATE DATABASE delta;"
```

Potem uruchom aplikację ponownie.

## Co jest zaletą tego podejścia

Dla Twojego projektu to jest dobre i praktyczne rozwiązanie:

- baza już istnieje i działa
- aplikacja jest odseparowana w kontenerze
- możesz ćwiczyć deployment readiness bez komplikowania całego stacku
- nie duplikujesz DB w kolejnym compose

## Kiedy to ma sens

To ma sens wtedy, gdy chcesz:

- uruchamiać aplikację „jak w produkcji”
- mieć osobny runtime dla backendu
- zachować istniejący, już działający PostgreSQL
- nauczyć się właściwej separacji: app vs database

## Kiedy nie trzeba tego robić

Jeśli aplikacja działa lokalnie przez `./gradlew bootRun` i nie chcesz jeszcze komplikować procesu, to ten krok jest opcjonalny. Dla nauki to jest fajny krok „na przyszłość”, ale nie jest obowiązkowy do działania mini-banku.

## Podsumowanie

W Twoim projekcie sensowny krok 18 to:

- kontener dla aplikacji
- baza PostgreSQL pozostaje istniejącym Dockerowym serwisem
- aplikacja łączy się do niej przez `host.docker.internal`

To jest prosty, czysty i praktyczny wariant, który nie powtarza tego, co już masz zrobione.
