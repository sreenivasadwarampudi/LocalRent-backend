# LocalRent Backend

Spring Boot 3 + MongoDB REST API for the LocalRent local rentals marketplace (bikes, cars, properties).

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

## Roles

- `OWNER` — can create/update/delete listings.
- `SEEKER` — can browse and search listings.

## API

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | public | Register (`role`: `OWNER` or `SEEKER`), returns JWT |
| POST | `/api/auth/login` | public | Login, returns JWT |
| GET | `/api/auth/me` | JWT | Current user |
| GET | `/api/listings/search` | public | Search listings |
| GET | `/api/listings/{id}` | public | Listing detail |
| GET | `/api/listings/mine` | JWT (owner) | Owner's listings |
| POST | `/api/listings` | JWT (owner) | Create listing |
| PUT | `/api/listings/{id}` | JWT (owner) | Update own listing |
| DELETE | `/api/listings/{id}` | JWT (owner) | Delete own listing |

### Search parameters

`category` (`BIKE`/`CAR`/`PROPERTY`), `lat`, `lng`, `radiusKm` (default 20, max 100), `area`, `maxPrice`.

When `lat`/`lng` are supplied the query uses a MongoDB 2dsphere `$geoWithin/$centerSphere` search and results are
returned sorted by distance with a `distanceKm` field. Otherwise `area` matches area name or city
case-insensitively.

### Example

```bash
curl -X POST http://localhost:8080/api/auth/signup -H 'Content-Type: application/json' \
  -d '{"name":"Ravi","email":"ravi@example.com","password":"secret123","role":"OWNER"}'

curl "http://localhost:8080/api/listings/search?lat=17.4401&lng=78.3489&radiusKm=20&category=BIKE"
```
