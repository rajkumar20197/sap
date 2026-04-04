# 🚀 SuccessFactors Lite (Interview Demo Project)

This project is your secret weapon for the SAP SuccessFactors interview. 
It demonstrates that you don't just "know Java" — you know how to build cloud-native, multi-tenant enterprise software mirroring SAP's architecture.

## 🏗️ What We Built (Sprints 1 & 2)

**1. Core Spring Boot + Java 17:**
- `EmployeeDTO`: Uses Java 17 `record` for immutable data transfer.
- `EmployeeService`: Uses Java Stream API for advanced in-memory filtering.
- `EmployeeController`: RESTful API supporting OData-style `$filter` parameters.

**2. Multi-Tenancy (The SAP Way):**
- `TenantInterceptor`: Extracts the `X-Tenant-ID` header from every API request.
- `TenantContext`: Stores the tenant ID in a `ThreadLocal` for thread-safe access.
- `EmployeeRepository`: Enforces `tenant_id = ?` isolation on every database query.

**3. Database & Optimization:**
- Flyway migrations (`V1__init.sql` & `V2__seed_data.sql`) manage the schema.
- Composite indexes added to prevent `Seq Scan` (sequential scans) in PostgreSQL.

**4. Quality & Testing (Sprint 4 Preview):**
- `EmployeeServiceTest`: Business logic tested using Mockito + JUnit 5.
- `GlobalExceptionHandler`: Centralized JSON error serialization.

## 🛠️ How to Run & Test It

### Prerequisites
- Docker & Docker Compose (for PostgreSQL)
- Java 17
- Maven (or use your IDE's built-in Maven like IntelliJ / VS Code Java Extension)

### 1. Start the Database
Open a terminal in this folder and run:
```bash
docker-compose up -d
```
*This starts a PostgreSQL 15 database on port 5432 and seeds it automatically upon app startup.*

### 2. Run the Application
You can run `SuccessFactorsLiteApplication.java` directly from your IDE, or via terminal (if Maven is installed):
```bash
mvn spring-boot:run
```

### 3. Test the Multi-Tenancy (API Calls)
Once running on `localhost:8080`, try these commands to see multi-tenancy in action!

**Get employees for Tenant 1 (acme-corp):**
```bash
curl -H "X-Tenant-ID: acme-corp" http://localhost:8080/api/v1/employees
```

**Get employees for Tenant 2 (globex-inc):**
```bash
curl -H "X-Tenant-ID: globex-inc" http://localhost:8080/api/v1/employees
```
*Notice how the data is completely isolated even though it's the same database table!*

**OData-Style Filtering Demo:**
```bash
curl -H "X-Tenant-ID: acme-corp" "http://localhost:8080/api/v1/employees?department=Engineering&skill=Java"
```

## 🎤 Interview Prep Note
Keep this project open in your IDE during the interview. When Dhiraj asks "How would you handle data isolation for thousands of clients?", you can confidently reply: 
> "I actually built a prototype for this. I used a ThreadLocal context to store the tenant ID from the request header, and intercepted every request to apply it at the repository level. This ensures no engineer can accidentally write a query that leaks data across companies."
