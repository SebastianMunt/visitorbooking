# Visitor Booking

Booking-system til besøg under udveksling.

## Funktioner

- Offentlig booking-side
- Kalender med optagede/blokerede datoer
- Admin-login
- Admin kan blokere datoer
- Admin kan slette bookinger
- Admin kan skifte kodeord
- Førstegangs-setup via `/setup`
- MySQL database
- Docker Compose setup

## Lokal kørsel med IntelliJ

Start MySQL-container:

```bash
docker start visitorbooking-mysql

##
Miljøvariabler

Opret en fil i projektets rod:

.env

Indsæt:

MYSQL_ROOT_PASSWORD=vælg-et-sikkert-password
MYSQL_DATABASE=visitorbooking

DB_URL=jdbc:mysql://mysql:3306/visitorbooking
DB_USERNAME=root
DB_PASSWORD=vælg-et-sikkert-password

MAIL_USERNAME=din-email@gmail.com
MAIL_PASSWORD=dit-google-app-password
MAIL_TO=din-email@gmail.com

Commit aldrig .env til GitHub.

Tilføj dette til .gitignore:

.env
target/
.idea/
*.iml
.DS_Store
Google App Password

For at sende emails skal Gmail bruge et App Password.

Aktivér totrinsbekræftelse på Google-kontoen.
Opret et App Password.
Gem App Password som MAIL_PASSWORD.
Brug aldrig dit normale Google-password.

Eksempel:

MAIL_USERNAME=eksempel@gmail.com
MAIL_PASSWORD=xxxx-xxxx-xxxx-xxxx
MAIL_TO=eksempel@gmail.com
Database med Docker

Start kun MySQL:

docker compose up -d mysql

Tjek containeren:

docker compose ps

MySQL kan tilgås fra computeren på:

localhost:3308

Eksempel på lokal database-URL:

jdbc:mysql://localhost:3308/visitorbooking
Kør appen lokalt fra IntelliJ
Åbn projektet i IntelliJ.
Vent på Maven-import.
Find VisitorbookingApplication.java.
Tryk på den grønne Run-knap.
Åbn siden i browseren.
http://localhost:8080

Booking-side:

http://localhost:8080/bookings

Adminside:

http://localhost:8080/admin
Kør appen fra terminalen

Start databasen:

docker compose up -d mysql

Start Spring Boot:

mvn spring-boot:run

Eller byg projektet:

mvn clean package

Hvis tests midlertidigt fejler:

mvn clean package -DskipTests

Start den byggede .jar:

java -jar target/visitorbooking-0.0.1-SNAPSHOT.jar
Kør hele systemet med Docker

Byg appens Docker-image:

docker build -t visitorbooking-app .

Start app og database:

docker compose up -d

Tjek status:

docker compose ps

Se logs:

docker compose logs app --tail=100

Stop systemet:

docker compose down

Brug ikke:

docker compose down -v

-v kan slette databasevolumen og dermed alle bookinger.

Docker Compose

Et eksempel på docker-compose.yml:

services:
  mysql:
    image: mysql:8.4
    container_name: visitorbooking-mysql-compose
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
    ports:
      - "3308:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  app:
    image: visitorbooking-app
    container_name: visitorbooking-app
    restart: unless-stopped
    ports:
      - "8081:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod

      DB_URL: ${DB_URL}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}

      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      MAIL_TO: ${MAIL_TO}
    depends_on:
      - mysql

volumes:
  mysql_data:
    name: visitorbooking_mysql_data
Databaseændring til FOOTBALL

Hvis databasen allerede eksisterede, før FOOTBALL blev tilføjet, skal kolonnen opdateres.

Gå ind i MySQL:

docker exec -it visitorbooking-mysql-compose mysql -uroot -p visitorbooking

Kør:

ALTER TABLE booking
MODIFY COLUMN booking_type
ENUM('GUEST', 'BLOCKED', 'EVENT', 'FOOTBALL')
NOT NULL;

Alternativt kan kolonnen ændres til almindelig tekst:

ALTER TABLE booking
MODIFY COLUMN booking_type VARCHAR(50) NOT NULL;

VARCHAR er ofte nemmere, hvis der senere skal tilføjes flere bookingtyper.

FC Barcelona-kampe

Administrator kan synkronisere kampe manuelt via:

Synkroniser Barça-kampe ⚽️

Derudover kan servicen køre automatisk:

@Scheduled(
    cron = "0 0 3 * * *",
    zone = "Europe/Copenhagen"
)

Det betyder, at kalenderen synkroniseres hver nat kl. 03:00.

Scheduling aktiveres i hovedklassen:

@EnableScheduling
@SpringBootApplication
public class VisitorbookingApplication {
}

Kampe vises som:

⚽️

Når brugeren klikker på bolden, vises:

Modstander
Dato
Tidspunkt
Stadion
Turnering

Football-events blokerer ikke kalenderen.

Tilpas systemet

Din ven bør ændre følgende.

Projektnavn

Ret i:

pom.xml

Eksempel:

<artifactId>mit-bookingsystem</artifactId>
<name>mit-bookingsystem</name>
Java package

Det eksisterende package er:

org.example.visitorbooking

Det kan ændres til eksempelvis:

dk.mitnavn.booking

Det kræver, at mapper og package-linjer ændres i alle Java-filer.

Tekster

Tilpas HTML-filerne i:

src/main/resources/templates

De vigtigste filer er:

index.html
bookings.html
admin.html
login.html
edit-booking.html
change-password.html
setup.html
Logo

Erstat:

src/main/resources/static/img/munt-logo.png

Husk også favicon i HTML:

<link rel="icon" type="image/png" href="/img/dit-logo.png">
Farver

Farverne ligger øverst i:

src/main/resources/static/css/style.css

Eksempel:

:root {
    --primary: #ff7a3d;
    --secondary: #2f80ed;
    --text: #1f2933;
    --background: #fff8ef;
}
Domæne

Erstat links til:

https://hossebogemma.dk

med eget domæne.

Søg i hele projektet efter:

hossebogemma.dk
Barcelona
Sebastian
Emma

I IntelliJ kan man bruge:

Cmd + Shift + F

på Mac eller:

Ctrl + Shift + F

på Windows.

Deployment på en server

Projektet kan deployes på eksempelvis:

Azure VM
DigitalOcean
Hetzner
AWS EC2
En almindelig Ubuntu-server

På serveren:

git clone DIN-GITHUB-REPOSITORY-URL
cd visitorbooking

Opret .env:

nano .env

Byg og start:

docker build -t visitorbooking-app .
docker compose up -d

Efter en kodeopdatering:

cd ~/visitorbooking
git pull
docker build -t visitorbooking-app .
docker compose up -d --force-recreate app
docker compose logs app --tail=100

Disse kommandoer opdaterer appen uden at slette MySQL-data.

Caddy og HTTPS

Et eksempel på en Caddy-konfiguration:

ditdomæne.dk, www.ditdomæne.dk {
    reverse_proxy localhost:8081
}

Caddy sørger for:

HTTPS
Automatisk TLS-certifikat
Redirect fra HTTP til HTTPS
Reverse proxy til Spring Boot

Husk at åbne portene:

80
443

på serverens firewall eller cloud-platform.

Vigtige sikkerhedspunkter
Commit aldrig .env
Brug aldrig rigtige passwords direkte i Java-koden
Brug et sikkert admin-password
Brug HTTPS i produktion
Begræns admin-endpoints med Spring Security
Brug CSRF-beskyttelse
Sørg for backup af databasen
Brug et separat Gmail App Password
Brug ikke MySQL root-brugeren i større produktionssystemer
Backup af databasen

Lav en databasebackup:

docker exec visitorbooking-mysql-compose \
mysqldump -uroot -p visitorbooking > visitorbooking-backup.sql

Gendan backup:

cat visitorbooking-backup.sql | \
docker exec -i visitorbooking-mysql-compose \
mysql -uroot -p visitorbooking
Kommandoer der ikke bør bruges

Følgende kan slette databaseindhold:

docker compose down -v
docker volume rm visitorbooking_mysql_data
docker system prune --volumes

Normal genstart er sikker:

docker compose restart

Ny app-version er også sikker:

docker compose up -d --force-recreate app
Typiske fejl
Maven findes ikke
zsh: command not found: mvn

Installer Maven:

brew install maven
Porten er allerede i brug
Port 8080 was already in use

Find processen:

lsof -i :8080

Stop processen:

kill PID
Appen kan ikke finde MySQL

Ved lokal kørsel fra IntelliJ:

jdbc:mysql://localhost:3308/visitorbooking

Ved kørsel inde i Docker:

jdbc:mysql://mysql:3306/visitorbooking

localhost inde i app-containeren betyder app-containeren selv. Derfor skal Docker bruge servicenavnet mysql.

FOOTBALL giver databasefejl

Eksempel:

Data truncated for column 'booking_type'

Opdater kolonnen:

ALTER TABLE booking
MODIFY COLUMN booking_type VARCHAR(50) NOT NULL;
Gamle CSS-filer vises

Lav en hard refresh:

Cmd + Shift + R

eller på Windows:

Ctrl + Shift + R
Mulige videreudviklinger
Godkendelse eller afvisning af bookinger
Email til gæsten ved godkendelse
Bookingstatus som PENDING, APPROVED og DECLINED
Automatisk opdatering af ændrede fodboldkampe
Flypriser til den valgte periode
Events og festivaler i lokalområdet
Guide til restauranter og seværdigheder
Gæstebog
Flere administratorer
Roller og rettigheder
Database migration med Flyway
Automatisk deployment med GitHub Actions
Tests med JUnit og Mockito
Licens og brug

Projektet må bruges som udgangspunkt for et personligt bookingsystem.

Husk at ændre:

Branding
Logo
Domæne
Email
Database-password
Admin-password
Personlige tekster
Produktionsindstillinger

Ved offentlig eller kommerciel brug bør der tilføjes en rigtig open source-licens, eksempelvis MIT License.
