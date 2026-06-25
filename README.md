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