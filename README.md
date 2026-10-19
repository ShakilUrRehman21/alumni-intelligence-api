# Alumni Intelligence API

A production-quality Spring Boot REST API that searches LinkedIn alumni profiles using the **PhantomBuster** integration and persists results in **PostgreSQL**.

---

## Table of Contents

1. [Architecture](#architecture)
2. [Prerequisites](#prerequisites)
3. [Quick Start (Docker)](#quick-start-docker)
4. [Quick Start (Local Dev)](#quick-start-local-dev)
5. [Configuration](#configuration)
6. [API Reference](#api-reference)
7. [PhantomBuster Setup](#phantombuster-setup)
8. [Running Tests](#running-tests)
9. [Project Structure](#project-structure)
10. [Troubleshooting](#troubleshooting)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      REST Controller                         │
│                    AlumniController                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                   AlumniService                              │
│  - initiateSearch()  → saves PENDING job, fires @Async       │
│  - executeSearch()   → calls PB client, upserts profiles     │
│  - getAllProfiles(), getProfileById(), searchProfiles()       │
└──────────┬──────────────────────────┬───────────────────────┘
           │                          │
┌──────────▼──────────┐   ┌───────────▼──────────────────────┐
│ PhantomBusterClient │   │  AlumniProfileRepository          │
│  (interface)        │   │  SearchJobRepository              │
├─────────────────────┤   └──────────────┬─────────────────────┘
│ MockPhantomBuster   │                  │
│ Client (no creds)   │       ┌──────────▼──────────┐
│                     │       │      PostgreSQL      │
│ PhantomBusterClient │       │    (Flyway schema)   │
│ Impl  (real HTTP)   │       └─────────────────────-┘
└─────────────────────┘
```

**Key design decisions:**
- **Interface-driven PB client** — real client activates only when `PHANTOMBUSTER_API_KEY` is set; mock client is used automatically otherwise.
- **Async search jobs** — `POST /api/alumni/search` returns immediately (202) with a job ID; results are fetched in the background.
- **Upsert deduplication** — `linkedin_profile_url` is the stable deduplication key (UNIQUE constraint).
- **Consistent response envelope** — every endpoint returns `{ "status", "message", "data", "errors", "timestamp" }`.

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| Docker + Docker Compose | any recent version |
| PostgreSQL | 16 (via Docker) |

---

## Quick Start (Docker)

```bash
# 1. Clone and enter the project
cd alumni-intelligence-api

# 2. Copy env template and fill in credentials
cp .env.example .env
# Edit .env with your values

# 3. Build the JAR
mvn clean package -DskipTests

# 4. Start everything
docker-compose up -d

# 5. Check health
curl http://localhost:8080/actuator/health

# 6. Open Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Quick Start (Local Dev)

```bash
# 1. Start only the database
docker-compose up -d postgres

# 2. Copy and configure environment
cp .env.example .env

# 3. Run the application (reads .env automatically via OS env or IDE)
export $(cat .env | xargs)
mvn spring-boot:run

# OR with PowerShell:
# Get-Content .env | ForEach-Object { if ($_ -notmatch '^#') { $kv = $_ -split '=', 2; [System.Environment]::SetEnvironmentVariable($kv[0], $kv[1]) } }
# mvn spring-boot:run
```

---

## Configuration

All configuration is in `src/main/resources/application.yml`. Every sensitive value is driven by an environment variable:

| Env Var | Required | Description |
|---|---|---|
| `DB_URL` | Yes | PostgreSQL JDBC URL |
| `DB_USERNAME` | Yes | Database username |
| `DB_PASSWORD` | Yes | Database password |
| `PHANTOMBUSTER_API_KEY` | For real PB | PhantomBuster workspace API key |
| `PHANTOMBUSTER_AGENT_ID` | For real PB | ID of the LinkedIn Search Export Phantom |
| `LINKEDIN_SESSION_COOKIE` | For real PB | LinkedIn `li_at` cookie |

See [`.env.example`](.env.example) for the full template.

> **Without PhantomBuster credentials:** The application starts and works normally, but uses the `MockPhantomBusterClient` which returns deterministic test fixtures.

---

## API Reference

### POST `/api/alumni/search`

Triggers a PhantomBuster LinkedIn alumni search.

**Request:**
```json
{
  "university": "University of XYZ",
  "designation": "Software Engineer",
  "passoutYear": 2020
}
```

**Response (202 Accepted):**
```json
{
  "status": "success",
  "message": "Search initiated. Check job status for results.",
  "data": {
    "id": 1,
    "university": "University of XYZ",
    "designation": "Software Engineer",
    "passoutYear": 2020,
    "status": "PENDING",
    "resultCount": 0,
    "createdAt": "2024-01-01T12:00:00Z"
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### GET `/api/alumni/all`

Returns all persisted alumni profiles (paginated).

**Query params:** `page` (default 0), `size` (default 20, max 100)

### GET `/api/alumni/{id}`

Single profile by database ID.

### GET `/api/alumni/search`

Filter saved profiles.

**Query params:** `university`, `currentRole`, `passoutYear`, `page`, `size`

### GET `/api/alumni/jobs/{jobId}`

Search job status and metadata.

### GET `/api/alumni/jobs`

Paginated list of all search jobs. Optional `status` filter: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`.

### GET `/actuator/health`

Spring Boot health endpoint.

---

## PhantomBuster Setup

To use real LinkedIn data:

1. **Create a PhantomBuster account** at [phantombuster.com](https://phantombuster.com)
2. **Add a LinkedIn Search Export Phantom**:
   - Go to Phantoms → + New Phantom → search "LinkedIn Search Export"
   - Configure and save it (don't run yet)
   - Copy the agent ID from the URL
3. **Get your API key**: Workspace Settings → API Keys → Copy
4. **Get your LinkedIn cookie** ([guide](https://hub.phantombuster.com/reference/how-to-get-your-linkedin-session-cookie))
5. Set `PHANTOMBUSTER_API_KEY`, `PHANTOMBUSTER_AGENT_ID`, `LINKEDIN_SESSION_COOKIE` in your `.env`

> **Note:** Scraping LinkedIn data must comply with LinkedIn's Terms of Service and applicable laws. This application uses PhantomBuster's official API, which is responsible for operating within platform limits.

---

## Running Tests

```bash
# All tests (requires Docker for Testcontainers)
mvn test

# Skip integration tests (no Docker required)
mvn test -Dexclude="**/*RepositoryTest.java"

# Specific test class
mvn test -Dtest=AlumniServiceTest
mvn test -Dtest=AlumniControllerTest
mvn test -Dtest=MockPhantomBusterClientTest
mvn test -Dtest=AlumniProfileRepositoryTest
```

**Test coverage:**
- `AlumniServiceTest` — service logic (Mockito, no DB)
- `AlumniControllerTest` — HTTP validation & routing (@WebMvcTest)
- `MockPhantomBusterClientTest` — stub behavior
- `AlumniProfileRepositoryTest` — real DB integration (Testcontainers PostgreSQL)

---

## Project Structure

```
src/
├── main/java/com/alumniintelligence/
│   ├── AlumniIntelligenceApplication.java
│   ├── client/
│   │   ├── PhantomBusterClient.java        # Interface
│   │   ├── PhantomBusterClientImpl.java    # Real HTTP client
│   │   ├── MockPhantomBusterClient.java    # Test stub
│   │   ├── PhantomBusterProfile.java       # Raw profile record
│   │   └── PhantomBusterException.java
│   ├── config/
│   │   ├── AppConfig.java
│   │   ├── AsyncConfig.java
│   │   ├── OpenApiConfig.java
│   │   └── PhantomBusterProperties.java
│   ├── controller/
│   │   └── AlumniController.java
│   ├── dto/
│   │   ├── request/AlumniSearchRequest.java
│   │   └── response/
│   │       ├── ApiResponse.java
│   │       ├── AlumniProfileResponse.java
│   │       └── SearchJobResponse.java
│   ├── entity/
│   │   ├── AlumniProfile.java
│   │   ├── JobStatus.java
│   │   └── SearchJob.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── ResourceNotFoundException.java
│   ├── mapper/
│   │   └── AlumniMapper.java
│   ├── repository/
│   │   ├── AlumniProfileRepository.java
│   │   └── SearchJobRepository.java
│   └── service/
│       └── AlumniService.java
├── main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__create_search_jobs_table.sql
│       └── V2__create_alumni_profiles_table.sql
└── test/java/com/alumniintelligence/
    ├── client/MockPhantomBusterClientTest.java
    ├── controller/AlumniControllerTest.java
    ├── repository/AlumniProfileRepositoryTest.java
    └── service/AlumniServiceTest.java
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| App starts with mock data warnings | `PHANTOMBUSTER_API_KEY` not set | Set env vars or use mock client intentionally |
| `Flyway validation failed` | Schema mismatch | Run `mvn flyway:repair` or drop and recreate DB |
| `Connection refused :5432` | PostgreSQL not running | `docker-compose up -d postgres` |
| `PhantomBuster 401` | Wrong API key | Verify key in PhantomBuster Settings |
| `PhantomBuster 429` | Rate limited | Wait and retry; reduce search frequency |
| Tests fail: `Docker not available` | Testcontainers needs Docker | Start Docker Desktop |
