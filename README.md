# SupportCenter

_SupportCenter is a microservice for managing and updating cases within the POB (Point of Business) system. It ensures
efficient handling of case updates, status changes, and synchronization with POB._

## Getting Started

### Prerequisites

- **Java 25 or higher**
- **Maven**
- **Git**
- **MariaDB**
- **[Dependent Microservices](#dependencies)**

### Installation

1. **Clone the repository:**

   ```bash
   git clone git@github.com:Sundsvallskommun/api-service-supportcenter.git
   ```
2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#Configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   If this microservice depends on other services, make sure they are up and accessible.
   See [Dependencies](#dependencies) for more details.

4. **Build and run the application:**

   ```bash
   mvn spring-boot:run
   ```

## Dependencies

This microservice depends on the following services:

- **POB**
  - **Purpose:** POB is the case management system where IT support handles cases.
  - **Website:** [https://www.serviceaide.com/products/pob](https://www.serviceaide.com/products/pob)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **SysMan**
  - **Purpose:** SysMan sends messages to the computers themselves. The end of lease job asks it to message every
    computer whose lease is ending.
  - **Setup Instructions:** Two installations are in use, one run by Sundsvall and one by Ånge. Each needs its own url
    and NTLM account.

Ensure that these services are running and properly configured before starting this microservice.

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Usage

### API Endpoints

Refer to the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X GET http://localhost:8080/api/2281/assets
```

## Scheduled Jobs

Computers that have reached end of lease are reported in two runs. Both are off in the checked-in configuration.

The lookup run (`end-of-lease-lookup`) reads each waiting computer's municipality from POB and writes it on the row.
That municipality decides which SysMan installation the computer belongs to, so nothing goes out before this has run.

The dispatch run (`end-of-lease-dispatch`) asks that installation to send the message, one call per municipality.

Turn both on by setting their cron expressions. The dispatch run also needs a real message id:

```yaml
scheduler:
  end-of-lease:
    lookup:
      cron: '0 0 * * * *'
    dispatch:
      cron: '0 30 * * * *'
      message-id: 42
```

`message-id: 1` is checked in as a placeholder. Turn the crons on without replacing it and the first run sends whatever
message 1 happens to be in each installation.

Both runs report on `/actuator/health`. The `endOfLeaseQueue` component answers RESTRICTED when the queue stops moving,
or when a computer has run out of attempts and needs a person to look at it.

A computer that has run out of attempts is left alone by both runs, and stays counted on the health endpoint until
somebody deals with it. This is how you put it back in the queue, with its attempts reset:

```bash
# every computer the municipality has been given up on
curl -X POST http://localhost:8080/api/2281/endOfLeaseComputers/retry \
  -H 'Content-Type: application/json' -d '{}'

# or just the ones you name
curl -X POST http://localhost:8080/api/2281/endOfLeaseComputers/retry \
  -H 'Content-Type: application/json' -d '{"serialNumbers": ["J123ABC"]}'
```

The municipality in the path is the one that registered the batch, not the one the lookup read from POB. A computer
that never got as far as a lookup goes back to the lookup run, and one that did goes straight to the dispatch run.

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in
`application.yml`.

### Key Configuration Parameters

- **Server Port:**

  ```yaml
  server:
    port: 8080
  ```
- **POB Service:**

  ```yaml
  integration:
    pob:
      url: http://dependency_service_url
      key: your_pob_key
  ```

  `key` is the service's own POB identity and only the end of lease job uses it. Every API request carries the caller's
  key instead, so leaving it out stops that job and nothing else.

- **SysMan Installations:**

  ```yaml
  integration:
    sysman:
      sundsvall:
        url: http://sysman_sundsvall_url
        username: account
        password: secret
      ange:
        url: http://sysman_ange_url
        username: account
        password: secret
  ```
- **Database:**

  ```yaml
  spring:
    datasource:
      url: jdbc:mariadb://localhost:3306/supportcenter
      username: username
      password: password
    flyway:
      enabled: true
  ```

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please
see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Code status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-supportcenter&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-supportcenter)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-supportcenter&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-supportcenter)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-supportcenter&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-supportcenter)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-supportcenter&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-supportcenter)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-supportcenter&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-supportcenter)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-supportcenter&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-supportcenter)

---

© 2024 Sundsvalls kommun
