# Food Delivery App

Full-stack food delivery web application built with **Spring Boot** and **React + Vite**.

## Tech Stack

| Layer      | Technology                                      |
|------------|-------------------------------------------------|
| Backend    | Spring Boot, Spring Security, JWT, Spring Data JPA |
| Database   | MySQL                                           |
| Frontend   | React 18, Vite, Axios                           |

## Project Structure

```
food-delivery-app/
├── backend/    # Spring Boot REST API
└── frontend/   # React + Vite SPA
```

## Getting Started

### Backend
1. Create MySQL database: `CREATE DATABASE food_delivery_db;`
2. Update `application.properties` with your DB credentials and JWT secret
3. Run: `./mvnw spring-boot:run`
4. API runs on `http://localhost:8080`
5. Swagger UI: `http://localhost:8080/swagger-ui.html`

### Frontend
1. `cd frontend`
2. `npm install`
3. Create `.env` with `VITE_API_BASE_URL=http://localhost:8080/api`
4. `npm run dev`
5. App runs on `http://localhost:5173`

## API Endpoints Overview

| Domain       | Base Path            |
|--------------|----------------------|
| Auth         | `/api/auth`          |
| Users        | `/api/users`         |
| Restaurants  | `/api/restaurants`   |
| Menu         | `/api/menu`          |
| Cart         | `/api/cart`          |
| Orders       | `/api/orders`        |
| Payments     | `/api/payments`      |
| Delivery     | `/api/delivery`      |
