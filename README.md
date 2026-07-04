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

## Notes

- The application stores data in an embedded H2 database file under `./data/banking_db`.
- Schema and seed data are initialized automatically when the app starts.
