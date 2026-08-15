<div align="center">

# 🍕 Zwigato - Food Delivery Platform

**A modern, full-stack food delivery web application with real-time order tracking**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)](https://www.mysql.com/)
[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

[Features](#-features) • [Tech Stack](#-tech-stack) • [Getting Started](#-getting-started) • [API Documentation](#-api-documentation) • [Screenshots](#-screenshots)

</div>

---

## 📋 Table of Contents

- [About](#-about)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [System Architecture](#-system-architecture)
- [Getting Started](#-getting-started)
- [Database Setup](#-database-setup)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [User Roles](#-user-roles)
- [Project Structure](#-project-structure)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Contributing](#-contributing)

---

## 🎯 About

**Zwigato** is a comprehensive food delivery platform that connects customers with restaurants and delivery agents. Built with modern technologies, it provides a seamless experience for ordering food online with real-time tracking, secure payments, and an intuitive user interface.

### Why Zwigato?

- 🚀 **Fast & Responsive** - Built with React 18 and Vite for lightning-fast performance
- 🔒 **Secure** - JWT authentication, Spring Security, and encrypted data transmission
- 📱 **Mobile-First** - Responsive design that works beautifully on all devices
- 🎨 **Beautiful UI** - Modern glass-morphism design with smooth animations
- 🔄 **Real-Time Updates** - WebSocket integration for live order tracking
- 💳 **Integrated Payments** - Razorpay payment gateway support
- 📊 **Admin Dashboard** - Comprehensive analytics and management tools

---

## ✨ Features

### For Customers 👥
- ✅ Browse restaurants and menus with search and filters
- ✅ Add items to cart with customization options
- ✅ Place orders with multiple payment methods (Online, COD, UPI, Wallet)
- ✅ Real-time order tracking with status updates
- ✅ View order history and download invoices
- ✅ Rate and review restaurants and food items
- ✅ Save delivery addresses for quick checkout
- ✅ Email OTP verification for account security

### For Restaurant Owners 🏪
- ✅ Manage restaurant profile and operating hours
- ✅ Add, edit, and remove menu items with categories
- ✅ View and manage incoming orders
- ✅ Track revenue and sales analytics
- ✅ Update order status in real-time
- ✅ Manage restaurant availability

### For Delivery Agents 🛵
- ✅ View available delivery orders
- ✅ Accept and manage assigned deliveries
- ✅ Update delivery status with location tracking
- ✅ View delivery history and earnings
- ✅ Real-time order notifications

### For Admins 👨‍💼
- ✅ Manage users, restaurants, and delivery agents
- ✅ View comprehensive dashboard with analytics
- ✅ Manage food categories and menu items
- ✅ Monitor orders and resolve issues
- ✅ Generate reports and insights
- ✅ System configuration and settings

---

## 🛠️ Tech Stack

### Backend
| Technology | Purpose | Version |
|------------|---------|---------|
| **Spring Boot** | Application Framework | 3.2.5 |
| **Spring Security** | Authentication & Authorization | 6.x |
| **Spring Data JPA** | Database Access | 3.x |
| **JWT (JJWT)** | Token-based Authentication | 0.12.5 |
| **MySQL** | Relational Database | 8.0+ |
| **Redis** | Caching Layer | 7.x |
| **WebSocket** | Real-time Communication | - |
| **Razorpay SDK** | Payment Processing | 1.4.5 |
| **iTextPDF** | Invoice Generation | 5.5.13 |
| **SpringDoc OpenAPI** | API Documentation | 2.5.0 |
| **MapStruct** | DTO Mapping | 1.5.5 |
| **Lombok** | Code Generation | 1.18.32 |
| **JavaMail** | Email Service | - |

### Frontend
| Technology | Purpose | Version |
|------------|---------|---------|
| **React** | UI Library | 18.2.0 |
| **Vite** | Build Tool | 5.1.4 |
| **React Router** | Client-side Routing | 6.22.3 |
| **Axios** | HTTP Client | 1.6.8 |
| **Tailwind CSS** | Utility-first CSS | 3.4.1 |
| **Leaflet** | Interactive Maps | 1.9.4 |
| **React Leaflet** | React Map Components | 4.2.1 |

### DevOps & Tools
- **Maven** - Dependency Management
- **npm** - Package Manager
- **Git** - Version Control
- **Swagger UI** - API Testing

---

## 🏗️ System Architecture

```
┌─────────────────┐
│   React SPA     │  ← Frontend (Port 5173)
│   (Vite Dev)    │
└────────┬────────┘
         │ HTTP/HTTPS
         │ (Axios)
         ▼
┌─────────────────┐
│  Spring Boot    │  ← Backend API (Port 8080)
│   REST API      │
└────────┬────────┘
         │
    ┌────┴─────┬──────────┬──────────┐
    ▼          ▼          ▼          ▼
┌───────┐  ┌───────┐  ┌──────┐  ┌────────┐
│ MySQL │  │ Redis │  │Email │  │Razorpay│
│  DB   │  │Cache  │  │SMTP  │  │Payment │
└───────┘  └───────┘  └──────┘  └────────┘
```

### Design Patterns Used
- **MVC** - Model-View-Controller architecture
- **Repository Pattern** - Data access abstraction
- **DTO Pattern** - Data transfer between layers
- **Service Layer** - Business logic encapsulation
- **Dependency Injection** - Loose coupling
- **JWT Token** - Stateless authentication

---

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- ☕ **Java 21** or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
- 🗄️ **MySQL 8.0** or higher ([Download](https://dev.mysql.com/downloads/))
- 📦 **Node.js 18** or higher ([Download](https://nodejs.org/))
- 🔧 **Maven 3.8+** (or use included Maven wrapper)
- 🐙 **Git** ([Download](https://git-scm.com/))

### Quick Start

#### 1️⃣ Clone the Repository

```bash
git clone https://github.com/yourusername/zwigato-food-delivery.git
cd zwigato-food-delivery
```

#### 2️⃣ Database Setup

```bash
# Create MySQL database
mysql -u root -p

# In MySQL shell:
CREATE DATABASE food_delivery_db;
USE food_delivery_db;

# Run the schema setup (optional - Hibernate can auto-create)
SOURCE database/schema.sql;

# Exit MySQL
exit;
```

#### 3️⃣ Backend Setup

```bash
cd backend

# Update application.properties with your configurations
# (See Configuration section below)

# Install dependencies and run
mvn clean install
mvn spring-boot:run

# Backend will start on http://localhost:8080
```

#### 4️⃣ Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Create environment file
cp .env.example .env

# Update .env with:
VITE_API_BASE_URL=http://localhost:8080/api

# Start development server
npm run dev

# Frontend will start on http://localhost:5173
```

#### 5️⃣ Access the Application

- **Frontend**: [http://localhost:5173](http://localhost:5173)
- **Backend API**: [http://localhost:8080/api](http://localhost:8080/api)
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **API Docs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 🗄️ Database Setup

### Option 1: Auto Schema Generation (Easiest)

Set in `application.properties`:
```properties
spring.jpa.hibernate.ddl-auto=update
```

Hibernate will automatically create tables on first run.

### Option 2: Manual SQL Setup

```bash
# Run the provided SQL scripts
cd database

# Using MySQL Workbench:
# - Open and execute schema.sql

# Or using command line:
mysql -u root -p food_delivery_db < schema.sql
```

### Option 3: Use Provided Seed Data

We have SQL scripts to populate test data:

```bash
# Navigate to backend directory
cd backend

# Run the seed script (Windows)
seed_database_compatible.bat

# Or manually in MySQL:
SOURCE mysql_compatible_setup.sql;
```

**Test Accounts After Seeding:**

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@zwigato.com | Password@1 |
| Customer | rahul@customer.com | Password@1 |
| Restaurant Owner | raj@owner.com | Password@1 |
| Delivery Agent | agent1@zwigato.com | Password@1 |

---

## ⚙️ Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
# ═══════════════════════════════════════════════════════
#  DATABASE CONFIGURATION
# ═══════════════════════════════════════════════════════
spring.datasource.url=jdbc:mysql://localhost:3306/food_delivery_db
spring.datasource.username=root
spring.datasource.password=YourPassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate DDL (update | create | create-drop | validate | none)
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# ═══════════════════════════════════════════════════════
#  JWT CONFIGURATION
# ═══════════════════════════════════════════════════════
jwt.secret=YourVerySecureRandomSecretKeyAtLeast256BitsLongForHS256Algorithm
jwt.expiration-ms=86400000

# ═══════════════════════════════════════════════════════
#  EMAIL CONFIGURATION (Gmail Example)
# ═══════════════════════════════════════════════════════
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# ═══════════════════════════════════════════════════════
#  REDIS CACHE (Optional)
# ═══════════════════════════════════════════════════════
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.type=redis

# ═══════════════════════════════════════════════════════
#  RAZORPAY PAYMENT GATEWAY
# ═══════════════════════════════════════════════════════
razorpay.key.id=your_razorpay_key_id
razorpay.key.secret=your_razorpay_secret

# ═══════════════════════════════════════════════════════
#  FILE UPLOAD
# ═══════════════════════════════════════════════════════
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# ═══════════════════════════════════════════════════════
#  CORS CONFIGURATION
# ═══════════════════════════════════════════════════════
cors.allowed-origins=http://localhost:5173,http://localhost:3000
```

### Frontend Configuration

Create `frontend/.env`:

```env
# API Base URL
VITE_API_BASE_URL=http://localhost:8080/api

# Razorpay Key (for payment integration)
VITE_RAZORPAY_KEY_ID=your_razorpay_key_id

# Google Maps API Key (optional, for location features)
VITE_GOOGLE_MAPS_API_KEY=your_google_maps_api_key
```

### Important Configuration Notes

1. **JWT Secret**: Generate a secure random string (min 256 bits for HS256)
2. **Gmail App Password**: Use App Password, not regular password ([Setup Guide](https://support.google.com/accounts/answer/185833))
3. **Razorpay**: Sign up at [razorpay.com](https://razorpay.com) for test/live keys
4. **Redis**: Optional but recommended for production caching

---

## 📚 API Documentation

### Swagger UI

Access interactive API documentation at: **http://localhost:8080/swagger-ui.html**

### API Endpoint Overview

| Domain | Base Path | Description |
|--------|-----------|-------------|
| 🔐 **Authentication** | `/api/auth` | Register, Login, OTP verification |
| 👤 **Users** | `/api/users` | User profile management |
| 🏪 **Restaurants** | `/api/restaurants` | Restaurant CRUD operations |
| 🍔 **Food Items** | `/api/menu` | Menu management |
| 🍕 **Categories** | `/api/categories` | Food category operations |
| 🛒 **Cart** | `/api/cart` | Shopping cart operations |
| 📦 **Orders** | `/api/orders` | Order placement and tracking |
| 💳 **Payments** | `/api/payments` | Payment processing |
| 🚚 **Delivery** | `/api/delivery` | Delivery agent operations |
| ⭐ **Reviews** | `/api/reviews` | Restaurant/food reviews |
| 📄 **Invoices** | `/api/invoices` | Invoice generation |
| 👨‍💼 **Admin** | `/api/admin` | Admin dashboard & management |

### Sample API Calls

#### Register User
```bash
POST /api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "9876543210",
  "address": "123 Main St, City",
  "password": "SecurePass@123"
}
```

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass@123"
}

# Response includes JWT token
```

#### Get Restaurants (Authenticated)
```bash
GET /api/restaurants?page=0&size=10
Authorization: Bearer <your-jwt-token>
```

---

## 👥 User Roles

### 1. Customer (CUSTOMER)
- Browse restaurants and order food
- Manage cart and place orders
- Track deliveries in real-time
- Rate and review restaurants

### 2. Restaurant Owner (RESTAURANT_OWNER)
- Manage restaurant profile
- Add/edit menu items and categories
- Process orders and update status
- View sales analytics

### 3. Delivery Agent (DELIVERY_AGENT)
- View available deliveries
- Accept and complete delivery orders
- Update delivery status
- Track earnings

### 4. Admin (ADMIN)
- Full system access
- Manage all users and restaurants
- View comprehensive analytics
- System configuration

---

## 📁 Project Structure

```
food-delivery-app/
│
├── backend/                          # Spring Boot Backend
│   ├── src/main/java/com/fooddelivery/
│   │   ├── config/                   # Configuration classes
│   │   │   ├── SecurityConfig.java   # Spring Security config
│   │   │   ├── RedisConfig.java      # Redis cache config
│   │   │   ├── WebSocketConfig.java  # WebSocket config
│   │   │   └── SwaggerConfig.java    # API documentation
│   │   │
│   │   ├── controller/               # REST Controllers
│   │   │   ├── AuthController.java
│   │   │   ├── RestaurantController.java
│   │   │   ├── OrderController.java
│   │   │   └── ...
│   │   │
│   │   ├── dto/                      # Data Transfer Objects
│   │   │   ├── request/              # Request DTOs
│   │   │   └── response/             # Response DTOs
│   │   │
│   │   ├── entity/                   # JPA Entities
│   │   │   ├── User.java
│   │   │   ├── Restaurant.java
│   │   │   ├── Order.java
│   │   │   └── ...
│   │   │
│   │   ├── repository/               # Spring Data Repositories
│   │   │   ├── UserRepository.java
│   │   │   ├── OrderRepository.java
│   │   │   └── ...
│   │   │
│   │   ├── service/                  # Business Logic
│   │   │   ├── impl/                 # Service implementations
│   │   │   └── ...
│   │   │
│   │   ├── security/                 # Security components
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthFilter.java
│   │   │   └── ...
│   │   │
│   │   ├── exception/                # Custom exceptions
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   └── util/                     # Utility classes
│   │
│   ├── src/main/resources/
│   │   ├── application.properties    # Main config
│   │   ├── application-dev.properties
│   │   └── application-prod.properties
│   │
│   └── pom.xml                       # Maven dependencies
│
├── frontend/                         # React Frontend
│   ├── public/                       # Static assets
│   │   └── favicon.ico
│   │
│   ├── src/
│   │   ├── assets/                   # Images, fonts, etc.
│   │   │
│   │   ├── components/               # Reusable components
│   │   │   ├── common/               # Common UI components
│   │   │   ├── layout/               # Layout components
│   │   │   └── ...
│   │   │
│   │   ├── context/                  # React Context
│   │   │   ├── AuthContext.jsx       # Authentication state
│   │   │   └── ToastContext.jsx      # Toast notifications
│   │   │
│   │   ├── pages/                    # Page components
│   │   │   ├── Home.jsx
│   │   │   ├── Login.jsx
│   │   │   ├── Register.jsx
│   │   │   ├── RestaurantDetails.jsx
│   │   │   └── ...
│   │   │
│   │   ├── services/                 # API services
│   │   │   ├── authService.js
│   │   │   ├── restaurantService.js
│   │   │   └── ...
│   │   │
│   │   ├── utils/                    # Utility functions
│   │   │   └── ...
│   │   │
│   │   ├── App.jsx                   # Root component
│   │   ├── main.jsx                  # Entry point
│   │   └── index.css                 # Global styles
│   │
│   ├── package.json                  # npm dependencies
│   ├── vite.config.js                # Vite configuration
│   └── tailwind.config.js            # Tailwind CSS config
│
├── database/                         # Database files
│   ├── schema.sql                    # Database schema
│   └── er-diagram.md                 # ER diagram
│
├── .gitignore
└── README.md                         # This file
```

---

## 🧪 Testing

### Backend Testing

```bash
cd backend

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run with coverage
mvn test jacoco:report
```

### Frontend Testing

```bash
cd frontend

# Run linter
npm run lint

# Build for production
npm run build

# Preview production build
npm run preview
```

### Manual Testing

1. **Use Swagger UI** at `http://localhost:8080/swagger-ui.html`
2. **Import Postman Collection** (if provided)
3. **Test with curl**:

```bash
# Health check
curl http://localhost:8080/api/health

# Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","phone":"1234567890","address":"Test Address","password":"Test@123"}'
```

---

## 🚀 Deployment

### Backend Deployment

#### Build JAR file
```bash
cd backend
mvn clean package -DskipTests

# JAR will be in target/food-delivery-backend-1.0.0.jar
```

#### Run JAR
```bash
java -jar target/food-delivery-backend-1.0.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

### Frontend Deployment

#### Build for Production
```bash
cd frontend
npm run build

# Build output in dist/ folder
```

#### Serve Static Files
Use any static file server:
- **Nginx**
- **Apache**
- **Vercel**
- **Netlify**
- **AWS S3 + CloudFront**

### Production Checklist

- [ ] Update `application-prod.properties` with production database
- [ ] Set secure JWT secret (minimum 256 bits)
- [ ] Configure HTTPS/SSL certificates
- [ ] Set up proper CORS policies
- [ ] Enable production logging
- [ ] Set up database backups
- [ ] Configure Redis cache (recommended)
- [ ] Set up monitoring and alerts
- [ ] Review security headers
- [ ] Test payment gateway integration

---

## 🎨 Screenshots

<div align="center">

### Home Page
![Home Page](docs/screenshots/home.png)

### Restaurant Menu
![Restaurant Menu](docs/screenshots/restaurant-menu.png)

### Order Tracking
![Order Tracking](docs/screenshots/order-tracking.png)

### Admin Dashboard
![Admin Dashboard](docs/screenshots/admin-dashboard.png)

</div>

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

### Coding Standards

- Follow **Java Code Conventions** for backend
- Use **ESLint** and **Prettier** for frontend
- Write **meaningful commit messages**
- Add **unit tests** for new features
- Update **documentation** as needed

---

## 📝 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Your Name**
- GitHub: [@yourusername](https://github.com/yourusername)
- LinkedIn: [Your LinkedIn](https://linkedin.com/in/yourprofile)
- Email: your.email@example.com

---

## 🙏 Acknowledgments

- Spring Boot Team for the excellent framework
- React Team for the amazing UI library
- All open-source contributors
- Stack Overflow community

---

## 📞 Support

For support and questions:

- 📧 Email: support@zwigato.com
- 💬 Discord: [Join our server](https://discord.gg/zwigato)
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/zwigato/issues)

---

<div align="center">

**⭐ Star this repository if you found it helpful!**

Made with ❤️ by [Your Name]

</div>
