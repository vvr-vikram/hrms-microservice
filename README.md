# HRMS Microservices System

A decoupled, multi-module microservice architecture for Human Resource Management (HRMS) built using **Spring Boot**, **Spring Cloud (Eureka Server, API Gateway & Centralized Config Server)**, **Resilience4j (Circuit Breakers & Bulkheads)**, **Distributed Request Tracing (Correlation IDs)**, and **PostgreSQL**.

---

## 🛠️ System Architecture & Ports

The project consists of 7 core modules communicating over REST and registered via Eureka:

```
  +-------------------------------------------------------------+
  |                   API Gateway (Port 8080)                   |
  +------------------------------+------------------------------+
                                 |
         +-----------------------+-----------------------+
         |                       |                       |
 +-------v-------+       +-------v-------+       +-------v-------+
 |  Employee Svc |       | Attendance Svc|       |   Leave Svc   |
 |  (Port 8081)  |       |  (Port 8082)  |       |  (Port 8083)  |
 +-------+-------+       +-------+-------+       +-------+-------+
         |                       |                       |
         |                       |                       |
         +-----------------------+-----------------------+
                                 |
                         +-------v-------+
                         |  Payroll Svc  |
                         |  (Port 8084)  |
                         +-------+-------+
                                 |
                         +-------v-------+
                         |  Config Svc   |
                         |  (Port 8888)  |
                         +---------------+
```

1. **`eureka-server` (Port 8761)**: Service Registry for active microservice discovery and heartbeat monitoring.
2. **`config-server` (Port 8888)**: Centralized configuration manager serving parameters from the local `config-repo` directory.
3. **`api-gateway` (Port 8080)**: Central routing manager and request trace generator. Directs client calls:
   - `/employees/**` -> `employee-service`
   - `/api/attendance/**` -> `attendance-service`
   - `/api/leaves/**` -> `leave-service`
   - `/api/payroll/**` -> `payroll-service`
4. **`employee-service` (Port 8081)**: Employee management, input validation, and audit logs.
5. **`attendance-service` (Port 8082)**: Mark daily check-ins and check-outs.
6. **`leave-service` (Port 8083)**: Apply for and approve employee leaves.
7. **`payroll-service` (Port 8084)**: Generates monthly payroll slips. Interacts over REST to pull data from other modules and uses Resilience4j for communication safety.

---

## 🔍 Distributed Observability & Request Tracing (Day 11)

To trace and debug requests as they flow across multiple microservice boundaries, we implement **Correlation ID-based distributed tracing**:
* **Gateway Filter (`CorrelationTrackingFilter`)**: Inspects every incoming HTTP request. If no `X-Correlation-Id` header is found, it generates a unique UUID and attaches it to the request and response headers.
* **MDC Servlet Filters (`CorrelationFilter`)**: Extracts the `X-Correlation-Id` from HTTP headers and stores it in SLF4J's **MDC (Mapped Diagnostic Context)** inside `payroll-service` and `employee-service`.
* **REST Client Interceptor (`RestTemplateConfig`)**: Copies the correlation ID from MDC and automatically appends it to downstream REST requests.
* **Unified Console Log Pattern**: All microservices log statements are formatted with `[TraceId: %X{correlationId}]`:
  ```text
  2026-09-01 11:30:15 [http-nio-8080-exec-1] [TraceId: c4b1e5a2-97dc-4a61-82ef-4e3b1c67d8f9] INFO  CorrelationTrackingFilter - Generated new Correlation-ID
  2026-09-01 11:30:16 [http-nio-8084-exec-2] [TraceId: c4b1e5a2-97dc-4a61-82ef-4e3b1c67d8f9] INFO  PayrollService - Generating payroll for employee: 1
  2026-09-01 11:30:16 [http-nio-8081-exec-3] [TraceId: c4b1e5a2-97dc-4a61-82ef-4e3b1c67d8f9] INFO  EmployeeService - Fetching employee with id: 1
  ```

---

## 🛡️ Resilience & Fault Tolerance (Day 8 & 9)

We use **Resilience4j** to protect the communication link between `payroll-service` and `employee-service`:
* **Circuit Breaker (`employeeServiceCB`)**: Automatically opens the gate and redirects calls to a safe fallback method if the error rate exceeds 50% within a 10-call sliding window.
* **Bulkhead (`employeeServiceBH`)**: Restricts concurrent API traffic to a maximum of 5 active threads, preventing slow responses from hogging server memory and causing cascading failures.
* **Fallback Behavior**: If a downstream service is down or slow, the payroll generator degrades gracefully (returning `"User temporarily unavailable"`) instead of throwing 500 exceptions or crashing.

---

## 🗃️ Centralized Configuration Repository (Day 7)

Configurations are completely decoupled from microservice code files. 
* All database credentials, port maps, JPA configurations, and Resilience4j thresholds live centrally inside the **[`config-repo/`](file:///c:/Users/Vigneshxbs/eclipse-workspace/hrms-microservice/hrms-microservice/config-repo/)** directory.
* Client services pull their active profile (e.g. `dev`, `test`) dynamically on boot from the Config Server.

---

## 📝 Request Validation & Standard Error Schema (Day 10)

* **DTO Request Validation**: The payroll generation endpoint accepts a JSON request body validated against strict rules:
  * `employeeId`: Cannot be null, must be a positive number.
  * `year`: Must be between 2000 and 2100.
  * `month`: Must be between 1 and 12.
* **Centralized Exception Handling**: Validation failures and microservice crashes are intercepted globally and returned in a unified **`ApiError`** JSON schema rather than exposing Java stack traces.

---

## 📋 Prerequisites

Before running the application, make sure you have:
1. **Java Development Kit (JDK)**: Java 17 or higher (Java 25 supported).
2. **PostgreSQL Database Server**:
   - Host: `localhost`
   - Port: `5000` *(Custom configuration)*
   - Username: `postgres`
   - Password: `1254`

---

## ⚙️ Initial Setup

### 1. Database Creation
Connect to your PostgreSQL server (port `5000`) and run the following commands to create the 4 required databases:

```sql
CREATE DATABASE hrms_employee_db;
CREATE DATABASE hrms_attendance_db;
CREATE DATABASE hrms_leave_db;
CREATE DATABASE hrms_payroll_db;
```

### 2. Eclipse Project Import & Compilation
1. In Eclipse, select **File** -> **Import...** -> **Existing Maven Projects**.
2. Select the parent directory `hrms-microservice` and finish the import.
3. Right-click parent project **`hrms-microservice`** -> **Maven** -> **Update Project...** (or press `Alt + F5`), select all modules, check "Force Update of Snapshots/Releases", and click **OK**.
4. Right-click parent project -> **Run As** -> **Maven install** to compile all submodules.

---

## 🚀 Running the Services (Docker)

To start the entire integrated system with Eureka, Config Server, Gateway, databases, and microservices in a single command, open PowerShell and run:

```powershell
cd C:\Users\Vigneshxbs\eclipse-workspace\hrms-microservice\hrms-microservice
docker compose up -d --build
```

---

## 🧪 Testing the APIs

Import the Postman collection file located in the root of the project to test the microservices through the gateway:
👉 **[`hrms-microservice_API.postman_collection.json`](file:///c:/Users/Vigneshxbs/eclipse-workspace/hrms-microservice/hrms-microservice_API.postman_collection.json)**

### 1. Validating DTO Request Bodies (Day 10 Test):
* **URL**: `POST http://localhost:8080/api/payroll/generate` (Headers: `Content-Type: application/json`)
* **Success Payload**:
  ```json
  {
      "employeeId": 1,
      "year": 2026,
      "month": 8
  }
  ```
* **Invalid Validation Payload**:
  ```json
  {
      "employeeId": -5,
      "year": 2026,
      "month": 15
  }
  ```
  *Expected Response:* **`400 Bad Request`** returning validation error fields.

### 2. Verifying Distributed Request Tracing (Day 11 Test):
Send the payroll generation request in Postman, then check your container logs in PowerShell:
```powershell
docker logs api-gateway
docker logs payroll-service
docker logs employee-service
```
Verify that the exact same `[TraceId: ...]` appears in the logs of all three containers for that single request.
