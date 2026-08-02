# LocalRent Backend

Spring Boot 3 + MongoDB REST API for the LocalRent local rentals marketplace (vehicles, machinery,
equipment and properties).

## Requirements

- Java 17+
- Maven 3.6+
- MongoDB running locally (`mongodb://localhost:27017/localrent`)

## Run

```bash
mvn spring-boot:run
```

Environment overrides: `MONGODB_URI`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`.

Server runs on http://localhost:8080.

## Accounts

Browsing and searching are fully public — no account needed. Accounts exist only for **owners** who post
rentals, and are identified by **phone number** (no email), since the target users often have no email address.

## Categories

`BIKE`, `SCOOTER`, `CAR`, `AUTO_RICKSHAW`, `TRUCK`, `TRACTOR`, `HEAVY_VEHICLE`, `FARM_EQUIPMENT`,
`CONSTRUCTION_EQUIPMENT`, `POWER_TOOLS`, `EVENT_EQUIPMENT`, `FURNITURE`, `ELECTRONICS`, `PROPERTY`, `OTHER`.

## API

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | public | Register an owner (`name`, `phone`, `password`), returns JWT |
| POST | `/api/auth/login` | public | Login with `phone` + `password`, returns JWT |
| GET | `/api/auth/me` | JWT | Current user |
| GET | `/api/listings/search` | public | Search listings |
| GET | `/api/listings/{id}` | public | Listing detail |
| GET | `/api/listings/mine` | JWT (owner) | Owner's listings |
| POST | `/api/listings` | JWT (owner) | Create listing |
| PUT | `/api/listings/{id}` | JWT (owner) | Update own listing |
| DELETE | `/api/listings/{id}` | JWT (owner) | Delete own listing |

### Search parameters

`category` (see Categories), `lat`, `lng`, `radiusKm` (default 20, max 100), `area`, `maxPrice`.

When `lat`/`lng` are supplied the query uses a MongoDB 2dsphere `$geoWithin/$centerSphere` search and results are
returned sorted by distance with a `distanceKm` field. Otherwise `area` matches area name or city
case-insensitively.

### Example

```bash
curl -X POST http://localhost:8080/api/auth/signup -H 'Content-Type: application/json' \
  -d '{"name":"Ravi","phone":"9876543210","password":"secret123"}'

curl "http://localhost:8080/api/listings/search?lat=17.4401&lng=78.3489&radiusKm=20&category=BIKE"
```
