# Hashiji Cafe -- Coffee Shop Management System

A full-stack coffee shop management platform built with **Java 17 / Spring Boot 3**. Designed for real-world operations: online ordering, POS counter sales, employee shift tracking, inventory management, AI-powered product recommendations, and a bilingual (EN/VI) customer-facing storefront.

**Live Demo**: [Deploy your own instance -- see Deployment Guide below]

---

## Key Features

### Customer Storefront (Public)
- Responsive SPA-style homepage with product carousel, careers portal, and contact form
- Full shopping cart with size and topping customization
- Order checkout with real-time tracking via unique tracking codes
- Bilingual interface (English / Vietnamese) with locale-aware content
- AI Chatbot recommending products based on natural language queries

### AI Recommendation Engine
- **5 strategies**: Cold Start (best sellers), Content-Based Filtering (TF cosine similarity), Collaborative Filtering (user-based KNN with Jaccard), Hybrid blending, and Semantic Search
- Semantic search leverages product **tags** (bilingual keywords) for accurate matching -- e.g., typing "sua" or "milk" correctly surfaces milk-based drinks only
- No external API dependencies; all algorithms run in-process

### Admin Dashboard
- Revenue analytics with Chart.js (monthly trends, daily breakdown)
- Top-selling product rankings with images
- Order management with status workflow (Pending -> Confirmed -> Shipping -> Completed)
- One-click order cancellation and direct customer contact (click-to-call)
- Monthly financial history with revenue, expenses, and net profit

### Point of Sale (POS)
- Real-time counter interface for walk-in customers
- Staff shift management with clock-in/clock-out and revenue tracking
- Automatic inventory deduction on order completion
- Printable receipts and invoice generation

### Human Resources
- Job posting management with public careers portal
- Application tracking with file upload (CV/resume)
- Applicant status pipeline: New -> Reviewed -> Interviewing -> Hired/Rejected
- Tracking codes for applicants to check status

### Inventory and Expenses
- Ingredient stock management with per-unit cost tracking
- Product recipe system linking ingredients to menu items
- Expense categorization and monthly reporting
- Automated inventory deduction based on recipes when orders complete

### Security
- Spring Security with role-based access (ADMIN, STAFF)
- BCrypt password hashing
- CSRF protection enabled by default
- Sensitive configs externalized via environment variables

---

## Tech Stack

| Layer       | Technology                                      |
|-------------|------------------------------------------------|
| Backend     | Java 17, Spring Boot 3.2, Spring Security, JPA |
| Database    | PostgreSQL (Supabase cloud or local)            |
| Frontend    | Thymeleaf, Bootstrap 5, HTMX, Chart.js          |
| AI/ML       | Custom TF-IDF, Cosine Similarity, KNN (no external APIs) |
| Caching     | Spring Cache (Simple / Redis)                   |
| Build       | Maven with wrapper (no global install needed)   |

---

## Quick Start

### Prerequisites
- Java 17+
- PostgreSQL database (or free [Supabase](https://supabase.com) account)

### 1. Clone and configure

```bash
git clone https://github.com/hashi173/Hashiji-Cafe.git
cd Hashiji-Cafe
```

Create `src/main/resources/application-dev.properties` (this file is gitignored):

```properties
DB_URL=jdbc:postgresql://YOUR_HOST:PORT/postgres
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

### 2. Run (first time with seed data)

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.seed-data=true"
```

### 3. Run (subsequent)

```powershell
.\mvnw.cmd spring-boot:run
```

Access at `http://localhost:8080`

### Default Accounts

| Role  | Username | Password |
|-------|----------|----------|
| Admin | admin    | 123456   |
| Staff | barista1 | 123456   |

---

## Deployment (Render -- Free Tier)

1. Push code to GitHub (credentials are externalized, safe to push)
2. Create a [Render](https://render.com) Web Service connected to the repo
3. Set environment variables in Render dashboard:
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (from Supabase)
   - `APP_PROFILE=prod`
4. Build command: `./mvnw clean install -DskipTests`
5. Start command: `java -jar target/*.jar`

---

## Project Structure

```
src/main/java/com/coffeeshop/
  config/       -- Security, data seeding, MVC, Redis configuration
  controller/   -- REST and MVC controllers (19 controllers)
  dto/          -- Data transfer objects for cart, POS, etc.
  entity/       -- JPA entities (19 entities)
  repository/   -- Spring Data JPA repositories
  service/      -- Business logic and AI recommendation engine

src/main/resources/
  templates/    -- Thymeleaf templates (admin, cart, checkout, tracking, etc.)
  static/       -- CSS, JS, product images
  messages*.properties -- i18n message bundles (EN/VI)
```

---

## License

This project is for educational and portfolio purposes.
