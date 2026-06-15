# Plataforma Bancaria

Proyecto de microservicios bancarios desarrollado con Java 17, Spring Boot, RxJava, Maven, MongoDB, 
Lombok, Logback, Config Server, Eureka, API Gateway, Redis, Kafka, Resilience4j, contratos OpenAPI

## Requisitos Previos

- Java 17
- Maven
- Docker Desktop
- PowerShell

## Variable JWT

El proyecto requiere una clave JWT. En PowerShell:

```powershell
$env:JWT_SECRET='temporary-review-secret-32-bytes!!'
```

Esta variable solo vive en la terminal actual. Si abres otra consola, debes volver a definirla.

## Compilar y Validar

Desde la raiz del proyecto:

```powershell
mvn -q clean verify
```

Este comando ejecuta:

- generacion OpenAPI
- compilacion
- pruebas unitarias
- Checkstyle
- reportes Jacoco

Los reportes Jacoco quedan en:

```text
<microservicio>/target/site/jacoco/index.html
```

## Levantar proyecto con Docker ejecutando archivo docker-compose.yml

Desde la raiz del proyecto:

```powershell
docker compose up -d --build         | Comando para ejecutar archivo docker-compose.yml
```

Verificar contenedores:

```powershell
docker compose ps
```

Ver logs:

```powershell
docker compose logs -f
```

```text
http://localhost:8080
```

## Endpoints Principales

Clientes:

- `POST /api/v1/customers`
- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `PUT /api/v1/customers/{id}`
- `DELETE /api/v1/customers/{id}`

Cuentas:

- `POST /api/v1/accounts`
- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{id}`
- `PUT /api/v1/accounts/{id}`
- `DELETE /api/v1/accounts/{id}`
- `POST /api/v1/accounts/{id}/deposits`
- `POST /api/v1/accounts/{id}/withdrawals`
- `GET /api/v1/accounts/{id}/balance`
- `GET /api/v1/accounts/{id}/movements`
- `POST /api/v1/accounts/transfers`
- `POST /api/v1/accounts/debit-cards`
- `POST /api/v1/accounts/debit-cards/{id}/payments`
- `GET /api/v1/accounts/{id}/debit-card/movements/recent`

Creditos:

- `POST /api/v1/credits`
- `GET /api/v1/credits`
- `GET /api/v1/credits/{id}`
- `PUT /api/v1/credits/{id}`
- `DELETE /api/v1/credits/{id}`
- `POST /api/v1/credits/{id}/payments`
- `POST /api/v1/credits/{id}/charges`
- `GET /api/v1/credits/{id}/balance`
- `GET /api/v1/credits/{id}/movements`
- `GET /api/v1/credits/{id}/credit-card/movements/recent`

Yanki:

- `POST /api/v1/wallets`
- `GET /api/v1/wallets`
- `GET /api/v1/wallets/{id}`
- `PUT /api/v1/wallets/{id}/debit-card`
- `POST /api/v1/wallets/payments`


## Diagramas y Postman

- Diagrama de arquitectura y secuencia: `docs/architecture.drawio`
- Colecciones Postman: carpeta `postman`
