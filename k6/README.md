# DevCommand Performance Testing with k6

This directory contains load tests for the DevCommand backend using [k6](https://k6.io/).

## What is being tested
These scripts test the real, running Spring Boot application under various concurrent workloads. They cover authentication (via JWT), REST API endpoint latency, throughput, and error rates across all modules (Auth, DSA, Tasks, Jobs, Learning, Projects, Analytics).

## Prerequisites
1. **k6**: You must install k6. On Windows, you can install it via winget:
   ```powershell
   winget install k6
   ```
2. **PostgreSQL**: Must be running on `localhost:5433` with the `devcommand` database.
3. **Spring Boot**: Must be running on `localhost:8080` (`mvn spring-boot:run`).

## Environment Variables
- `BASE_URL`: Base URL of the API (Default: `http://localhost:8080`)
- `K6_TEST_EMAIL`: Test user email to authenticate (Default: `k6-test@example.com`)
- `K6_TEST_PASSWORD`: Test user password (Default: `password123`)
- `K6_REGISTER_EMAIL`: Email to register if the test user doesn't exist (Default: `k6-test@example.com`)

## Data Safety Notes
**WARNING:** These tests run against the environment specified by `BASE_URL`. By default, they will create a test user `k6-test@example.com` if one does not exist. 
The test logic prioritizes `GET` requests for load to avoid polluting your development database with thousands of mock records. Do not run these tests against a production database without careful consideration.

## How to Run Tests

Run these commands from the root directory of the project.

### 1. Smoke Test (1-2 VUs for 30s)
Use this to verify the environment and authentication are working.
```powershell
k6 run --vus 2 --duration 30s k6/scenarios/analytics.js
```

### 2. Baseline Test (10 VUs for 1m)
```powershell
k6 run --vus 10 --duration 1m k6/scenarios/analytics.js
```

### 3. Load Test (25 VUs for 2m)
```powershell
k6 run --vus 25 --duration 2m k6/scenarios/mixed-workload.js
```

### 4. Stress Test (50 VUs for 3m)
*Only run this after verifying the baseline and load tests.*
```powershell
k6 run --vus 50 --duration 3m k6/scenarios/mixed-workload.js
```

## Interpreting Results
- **http_req_duration (p95)**: 95% of requests completed faster than this time. Target is `<1000ms`.
- **http_req_failed**: The error rate. Target is `<1%` (0.01).
- **http_reqs**: Total throughput of requests.
- **iterations / VUs**: The number of virtual users executing the scenario logic.

## Stopping a Test
To stop a test mid-run, simply press `Ctrl+C` in your terminal.
