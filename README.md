# 🌿 EcoTrack: AI-Powered Carbon Footprint & Sustainability Platform

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/springboot-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Angular](https://img.shields.io/badge/angular-%23DD0031.svg?style=for-the-badge&logo=angular&logoColor=white)
![TypeScript](https://img.shields.io/badge/typescript-%23007ACC.svg?style=for-the-badge&logo=typescript&logoColor=white)
![JavaScript](https://img.shields.io/badge/javascript-%23323330.svg?style=for-the-badge&logo=javascript&logoColor=%23F7DF1E)
![PostgreSQL](https://img.shields.io/badge/postgresql-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI-%23412991.svg?style=for-the-badge&logo=openai&logoColor=white)

### 🌐 Live Deployment
**Frontend:** `[Insert Vercel/Render Link Here]`  
**Backend API:** `[Insert AWS/Render Link Here]`  

---

## 📖 What is EcoTrack?
EcoTrack is a comprehensive, full-stack sustainability management platform designed to help individuals track, analyze, and reduce their carbon footprint. By leveraging generative AI, the platform provides highly personalized lifestyle recommendations, tracks daily emissions across multiple categories, and encourages environmental action through community challenges and gamification.

### 🌍 Core Sustainability Modules
1. **Carbon Tracking:** Log daily emissions across Transportation, Energy, Food, and Waste.
2. **Goal Management:** Set, monitor, and achieve real-time CO₂ reduction targets.
3. **AI Recommendation System:** Receive predictive insights and personalized action plans.
4. **Community Challenges:** Join global sustainability challenges and track progress.
5. **Analytics & Reports:** Visualize Month-over-Month (MoM) trends with downloadable CSV reports.

---

## 🤖 AI Insights Engine
EcoTrack utilizes a dual-layered AI architecture to guarantee 100% uptime and dynamic personalization.

* **Primary Architecture — Generative AI (Spring AI + OpenAI):** Uses the `gpt-4o-mini` LLM. The system feeds the user's last 30 days of carbon logs, active goals, diet preferences, and commute modes into a highly engineered prompt. It returns a structured JSON response predicting monthly emissions and suggesting priority actions.
* **Explainability & Fallback — Rule-Based Heuristic Engine:** An in-house mathematical engine (`AIRuleServiceImpl`) that acts as a fail-safe. If the OpenAI API rate-limits or times out, this engine calculates percentage breakdowns (e.g., *if Transport > 40%, flag as Weakness*) to ensure the user always receives actionable insights.
* **Data Pipeline:** `CarbonEmissionRepository` + `UserRepository` ➡️ `GenerativeAIService` ➡️ `BeanOutputConverter` (Structured Output) ➡️ Angular Frontend.

---

## 🛠️ Tech Stack & Libraries

### **Backend**
* **Java 17 & Spring Boot:** Core REST API framework.
* **Spring Security & JWT:** Stateless authentication and role-based access.
* **Spring AI:** Seamless LLM integration and structured prompt engineering.
* **Hibernate / Spring Data JPA:** ORM and database management.

### **Frontend**
* **Angular 16+ & TypeScript:** Component-based SPA architecture.
* **JavaScript (ES6+) & RxJS:** Reactive programming for state and API handling.
* **Chart.js:** Rendering dynamic emission breakdown charts.
* **Bootstrap & Custom CSS:** Responsive, dark-mode compatible UI (Eco-Cards).

### **Database & Infrastructure**
* **PostgreSQL (Neon Cloud DB):** Primary relational database.
* **Cloudinary:** Cloud storage for user profile pictures.
* **Google OAuth2:** Alternative social login flow.

---

## 📁 Folder Structure

The project follows an industry-standard Monorepo layout:

```text
eco-track/
├── backend/                # Spring Boot Java Application
│   ├── src/main/java/      # Controllers, Services, Entities, Repositories
│   └── src/main/resources/ # application.properties, static assets
├── frontend/               # Angular SPA Application
│   ├── src/app/core/       # Interceptors, Auth Guards, Base Services
│   └── src/app/features/   # AI, Dashboard, Challenges, Carbon Tracker
├── database/               # SQL schema and seed data scripts
├── docker/                 # Dockerfiles and docker-compose.yml
├── docs/                   # API documentation and setup guides
└── postman/                # Exported Postman API collections
