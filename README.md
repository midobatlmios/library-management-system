# Library Management System

A Spring Boot REST API for managing library books and genres. The application uses
MySQL for storage and runs on port `9090` by default.

## Requirements

- Windows 10 or later
- Git
- Java 17 or later
- MySQL 8 or later

The Maven wrapper is included, so you do not need to install Maven separately.

## Run on Windows

1. Clone the repository and enter the project directory:

   ```powershell
   git clone https://github.com/midobatlmios/library-management-system.git
   cd library-management-system
   cd library-management-system
   ```

   The second `cd` is needed because the Spring Boot project is inside a
   subfolder of the repository.

2. Start MySQL and create the database:

   ```sql
   CREATE DATABASE library_sys;
   ```

   Hibernate creates or updates the application's tables when it starts.

3. In the same PowerShell window, provide the MySQL credentials for your
   computer. Replace the example values with your local MySQL username and
   password:

   ```powershell
   $env:SPRING_DATASOURCE_USERNAME = "root"
   $env:SPRING_DATASOURCE_PASSWORD = "your_mysql_password"
   ```

   If your MySQL account has no password, set the password to an empty string:

   ```powershell
   $env:SPRING_DATASOURCE_PASSWORD = ""
   ```

4. Start the application:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   The first run may take a few minutes while Maven downloads dependencies.
   When startup completes, open <http://localhost:9090/>. The welcome message
   confirms that the API is running.

To stop the application, press `Ctrl+C` in the PowerShell window.

## Build and tests

From the project directory (`library-management-system`):

```powershell
.\mvnw.cmd -DskipTests package
```

Run the test suite with:

```powershell
.\mvnw.cmd test
```

The tests may require a running MySQL server and the `library_sys` database.

## API overview

- `GET /` — health/welcome response
- `/api/books` — book endpoints
- `/api/genres` — genre endpoints

The `/api/**` endpoints require authentication. The project does not currently
include a complete authentication/signup flow, so those endpoints are not ready
for use without a valid JWT.

## Configuration

The default database URL is `jdbc:mysql://localhost:3306/library_sys`. Override
the database URL, username, or password with the corresponding Spring Boot
environment variables, such as `SPRING_DATASOURCE_URL` or
`SPRING_DATASOURCE_USERNAME`.

The JWT signing key currently lives in the source code and is suitable only for
local development. Do not use this key or the current configuration for a
public production deployment.
