# Docker Setup

## Stack

This project is containerized as three services:

- `mysql` for the database
- `backend` for the Spring Boot API
- `frontend` for the Angular SPA served by nginx

The frontend proxies `/api` requests to the backend container, so the browser only needs to open one public URL.

## Files

- [docker-compose.yml](C:\Users\LENOVO\Desktop\angular projects\RideFlow\docker-compose.yml)
- [RideFlow-Backend\Dockerfile](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow-Backend\Dockerfile)
- [RideFlow\Dockerfile](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow\Dockerfile)
- [RideFlow\nginx\default.conf](C:\Users\LENOVO\Desktop\angular projects\RideFlow\RideFlow\nginx\default.conf)

## Run

From the project root:

```powershell
docker compose up --build
```

## Access

- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8080/api`
- MySQL: internal to Docker by default

## Default Credentials

- Admin email: `admin@rideflow.local`
- Admin password: `Admin1234`
- MySQL root password: `root`

## Stop

```powershell
docker compose down
```

If you also want to remove the MySQL volume:

```powershell
docker compose down -v
```

## Notes

- The Angular Docker build uses a Docker-specific index file with `rideflow-api-base-url=/api`.
- The backend still exposes port `8080` so Postman or direct API testing remains possible outside the frontend container.
- Replace the default JWT secret and database password before using this anywhere beyond local development.
