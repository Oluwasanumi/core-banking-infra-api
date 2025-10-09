# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.6 banking API application using Java 17. The project uses Maven for build management and PostgreSQL as the database.

## Development Commands

### Building and Running
- **Compile**: `./mvnw compile`
- **Package**: `./mvnw package`
- **Run application**: `./mvnw spring-boot:run`
- **Run tests**: `./mvnw test`
- **Clean and build**: `./mvnw clean package`

### Database Setup
- **Start PostgreSQL**: `docker-compose up -d`
- **Stop PostgreSQL**: `docker-compose down`

Database credentials (from docker-compose.yml):
- Database: `banking_db`
- Username: `banking_user`
- Password: `banking_password`
- Port: `5432`

## Architecture

### Package Structure
- `com.caspercodes.bankingapi` - Root package
  - `controller/` - REST controllers
  - `model/` - JPA entities
  - `repository/` - Spring Data JPA repositories

### Key Technologies
- **Spring Boot 3.5.6** - Main framework
- **Spring Data JPA** - Database access
- **Spring Security** - Security framework
- **Spring Validation** - Input validation
- **PostgreSQL** - Database
- **Lombok** - Reduces boilerplate code
- **Maven** - Build tool

### Configuration
- Main config: `src/main/resources/application.yml`
- Database URL: `jdbc:postgresql://localhost:5432/banking_db`
- Server port: `8080`
- JPA: Uses `create-drop` DDL mode with SQL logging enabled

### Current Implementation
The application currently has a basic health check feature:
- `HealthCheck` entity with `id` and `status` fields
- `HealthCheckRepository` extends `JpaRepository`
- `TestController` provides endpoints:
  - `GET /api/test/create` - Creates a health check record
  - `GET /api/test/all` - Returns all health check records

### Testing
- Uses JUnit 5 with Spring Boot Test
- Test location: `src/test/java/`
- Run with: `./mvnw test`