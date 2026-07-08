# Visitor Booking

Bookingsystem til besøg under udveksling.

## Funktioner

* Offentlig booking-side
* Kalender med bookede og blokerede datoer
* Admin-login
* Opret, rediger og slet bookinger
* Bloker datoer
* Skift admin-kodeord
* Førstegangsopsætning via `/setup`
* Email ved nye bookinger
* MySQL og Docker Compose
* FC Barcelona-kampe i kalenderen

## Teknologier

* Java 21
* Spring Boot
* Thymeleaf
* Spring Security
* Spring Data JPA
* MySQL
* Maven
* Docker
* FullCalendar

## Miljøvariabler

Opret en `.env`-fil i projektets rod:

```env
MYSQL_ROOT_PASSWORD=vælg-et-sikkert-password
MYSQL_DATABASE=visitorbooking

DB_URL=jdbc:mysql://mysql:3306/visitorbooking
DB_USERNAME=root
DB_PASSWORD=vælg-et-sikkert-password

MAIL_USERNAME=din-email@gmail.com
MAIL_PASSWORD=dit-google-app-password
MAIL_TO=din-email@gmail.com
```

Tilføj dette til `.gitignore`:

```gitignore
.env
target/
.idea/
*.iml
.DS_Store
```

Commit aldrig `.env` til GitHub.

Gmail kræver et Google App Password. Brug ikke dit normale Google-password.

## Kør lokalt

Start MySQL:

```text
docker compose up -d mysql
```

Når appen køres fra IntelliJ, skal databaseforbindelsen bruge:

```text
jdbc:mysql://localhost:3308/visitorbooking
```

Når appen køres med Docker, skal den bruge:

```text
jdbc:mysql://mysql:3306/visitorbooking
```

Start appen fra IntelliJ ved at køre:

```text
VisitorbookingApplication.java
```

Eller start fra terminalen:

```text
mvn spring-boot:run
```

Åbn:

```text
http://localhost:8080
```

Booking-side:

```text
http://localhost:8080/bookings
```

Adminside:

```text
http://localhost:8080/admin
```

## Byg projektet

```text
mvn clean package
```

Hvis tests midlertidigt fejler:

```text
mvn clean package -DskipTests
```

## Kør med Docker

Byg appen:

```text
docker build -t visitorbooking-app .
```

Start app og database:

```text
docker compose up -d
```

Se status:

```text
docker compose ps
```

Se logs:

```text
docker compose logs app --tail=100
```

Stop systemet:

```text
docker compose down
```

Brug ikke følgende kommando, da den kan slette databasen:

```text
docker compose down -v
```

## FC Barcelona-kampe

Admin kan synkronisere hjemmekampe med knappen:

```text
Synkroniser Barça-kampe ⚽️
```

Kampene synkroniseres også automatisk hver nat kl. 03:00.

Football-events vises i kalenderen, men blokerer ikke for bookinger.

Hvis `FOOTBALL` ikke findes i en ældre database, kan kolonnen ændres til tekst:

```sql
ALTER TABLE booking
MODIFY COLUMN booking_type VARCHAR(50) NOT NULL;
```

## Tilpas projektet

Ret projektets navn i:

```text
pom.xml
```

Tilpas HTML-filerne i:

```text
src/main/resources/templates
```

Tilpas CSS i:

```text
src/main/resources/static/css/style.css
```

Udskift logoet i:

```text
src/main/resources/static/img
```

Søg i hele projektet efter følgende og erstat med egne oplysninger:

```text
hossebogemma.dk
Barcelona
Sebastian
Emma
```

I IntelliJ:

```text
Cmd + Shift + F
```

På Windows:

```text
Ctrl + Shift + F
```

## Deployment

På serveren:

```text
git clone DIN-GITHUB-REPOSITORY-URL
cd visitorbooking
nano .env
docker build -t visitorbooking-app .
docker compose up -d
```

Efter en opdatering:

```text
cd ~/visitorbooking
git pull
docker build -t visitorbooking-app .
docker compose up -d --force-recreate app
docker compose logs app --tail=100
```

Dette opdaterer appen uden at slette MySQL-data.

## HTTPS med Caddy

Eksempel på `Caddyfile`:

```text
ditdomæne.dk, www.ditdomæne.dk {
    reverse_proxy localhost:8081
}
```

Husk at åbne port 80 og 443 på serveren.

## Backup

Lav en backup:

```text
docker exec visitorbooking-mysql-compose \
mysqldump -uroot -p visitorbooking > visitorbooking-backup.sql
```

Gendan backup:

```text
cat visitorbooking-backup.sql | \
docker exec -i visitorbooking-mysql-compose \
mysql -uroot -p visitorbooking
```

## Sikkerhed

* Gem aldrig passwords i GitHub
* Brug HTTPS i produktion
* Brug et sikkert admin-password
* Brug Google App Password til email
* Tag regelmæssig backup af databasen
* Brug ikke `docker compose down -v`

## Mulige videreudviklinger

* Godkendelse af bookinger
* Email til gæsten
* Flere administratorer
* Events og festivaler
* Flypriser
* Automatisk deployment med GitHub Actions
* Flere tests
