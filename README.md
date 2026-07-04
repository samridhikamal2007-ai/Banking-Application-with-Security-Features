# Banking App

This is a standalone JavaFX banking application that uses an embedded H2 database.

## Requirements

- Java 21 or later
- Maven 3.9.x

## Run the application

From the project root:

```powershell
$env:USERPROFILE\tools\apache-maven-3.9.6\bin\mvn.cmd -U -DskipTests javafx:run
```
Or, run the bundled PowerShell wrapper:

```powershell
.
un.ps1
```
If you have `mvn` available on your PATH, you can also run:

```powershell
mvn -U -DskipTests javafx:run
```

## Default accounts

- Admin user:
  - Username: `admin`
  - Password: `Admin@123`

- Customer user:
  - Username: `customer`
  - Password: `Customer@123`

## Remote API integration

The application can optionally connect to an external API server for event publishing and health checks.

- Set environment variable `BANK_API_BASE_URL` to the API root, for example:

```powershell
$env:BANK_API_BASE_URL = "https://api.example.com"
```

- Or pass a system property when launching:

```powershell
mvn javafx:run -Dbank.api.base.url=https://api.example.com
```

When configured, the app will publish events for deposit, withdrawal, and transfer actions.

## Notes

- The application stores data in an embedded H2 database file under `./data/banking_db`.
- Schema and seed data are initialized automatically when the app starts.
