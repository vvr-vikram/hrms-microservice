# HRMS Microservices System

A decoupled, multi-module microservice architecture for Human Resource Management (HRMS) built using **Spring Boot**, **Spring Cloud (Eureka Server & API Gateway)**, and **PostgreSQL**.

---

## 🛠️ System Architecture & Ports

The project consists of 6 core modules communicating over REST and registered via Eureka:

```
  +-------------------------------------------------------------+
  |                   API Gateway (Port 8080)                   |
  +------------------------------+------------------------------+
                                 |
        +------------------------+------------------------+
        |                        |                        |
+-------v-------+        +-------v-------+        +-------v-------+
|  Employee Svc |        | Attendance Svc|        |   Leave Svc   |
|  (Port 8081)  |        |  (Port 8082)  |        |  (Port 8083)  |
+-------+-------+        +-------+-------+        +-------+-------+
        |                        |                        |
        |                        |                        |
        +------------------------+------------------------+
                                 |
                         +-------v-------+
                         |  Payroll Svc  |
                         |  (Port 8084)  |
                         +---------------+
```

1. **`eureka-server` (Port 8761)**: Service Registry for active microservice discovery and heartbeat monitoring.
2. **`api-gateway` (Port 8080)**: Central routing manager. Directs client calls:
   - `/employees/**` -> `employee-service`
   - `/api/attendance/**` -> `attendance-service`
   - `/api/leaves/**` -> `leave-service`
   - `/api/payroll/**` -> `payroll-service`
3. **`employee-service` (Port 8081)**: Employee management and audit logs.
4. **`attendance-service` (Port 8082)**: Mark daily check-ins and check-outs.
5. **`leave-service` (Port 8083)**: Apply for and approve employee leaves.
6. **`payroll-service` (Port 8084)**: Generates monthly payroll slips. Interacts over REST to pull base salary (Employee), workdays (Attendance), and approved leaves (Leave).

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
*(Spring Boot's `ddl-auto: update` will automatically create the schemas and tables in these databases upon service startup).*

### 2. Eclipse Project Import
1. In Eclipse, select **File** -> **Import...** -> **Existing Maven Projects**.
2. Select the parent directory `hrms-microservice` and finish the import.
3. Right-click parent project **`hrms-microservice`** -> **Maven** -> **Update Project...** (or press `Alt + F5`), select all modules, check "Force Update of Snapshots/Releases", and click **OK**.
4. Right-click parent project -> **Run As** -> **Maven install** to compile all submodules.

### 3. Hiding JDK 25 Deprecation & Native Access Warnings (Optional)
If running on Java 25, you can hide the JVM console warnings by adding VM arguments globally in Eclipse:
1. Go to **Window** -> **Preferences** -> **Java** -> **Installed JREs**.
2. Select your JRE (Java 25) -> **Edit...**.
3. In **Default VM arguments**, enter:
   ```
   --sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED
   ```
4. Click **Finish** -> **Apply and Close**.

---

## 🚀 Running the Services

Start the services in Eclipse by finding their main java class, **Right-clicking** on it, and selecting **Run As** -> **Java Application** in this exact order:

1. **`eureka-server`**: Run `EurekaServerApplication.java`
   - *Wait about 5-10 seconds. You can verify it by opening `http://localhost:8761` in your browser.*
2. **`api-gateway`**: Run `ApiGatewayApplication.java`
3. **`employee-service`**: Run `EmployeeServiceApplication.java`
4. **`attendance-service`**: Run `AttendanceServiceApplication.java`
5. **`leave-service`**: Run `LeaveServiceApplication.java`
6. **`payroll-service`**: Run `PayrollServiceApplication.java`

Verify that all 5 client services appear as **`UP`** on the Eureka Server dashboard.

---

## 🧪 Testing the APIs

Import the Postman collection file located in the root of the project to test the microservices through the gateway:
👉 **[`hrms-microservice_API.postman_collection.json`](file:///c:/Users/Vigneshxbs/eclipse-workspace/hrms-microservice/hrms-microservice_API.postman_collection.json)**

### Sample Walkthrough Scenario (using name Vikram V):
1. **Onboard Employee**: Send the onboarding POST request (`/employees/onboard`). This onboard's **Vikram V** with ID `1`.
2. **Apply Leave**: Send a POST request to `/api/leaves/apply` for employee `1`.
3. **Approve Leave**: Send a PUT request to approve the leave (`/api/leaves/1/approve?approvedBy=Vikram V`).
4. **Mark Attendance**: Send POST requests for check-in and check-out to log work hours.
5. **Generate Monthly Payroll**: Send a POST request to `/api/payroll/generate?employeeId=1&year=2026&month=8` to calculate earnings, deductions (with late/leave checks), and net salary.
