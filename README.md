# Mock Omnistore UI & Quotation API

A full-stack application demonstrating a simulated retail/omnichannel checkout interface with real-time price synchronization from a microservice (Quotation API).

## 🚀 Project Overview

The project consists of three main components:
1. **Frontend (`frontend/`)**: An Angular-based user interface for scanning/adding items and completing transactions.
2. **Omnistore Backend (`backend/`)**: A Spring Boot application managing the active transaction, inventory database, and business logic.
3. **Quotation API (`Quotation/`)**: A Spring Boot microservice providing quotation rates for products, simulating an external pricing engine.

---

## 🛠️ Architecture & Price Sync Flow

Price synchronization with the Quotation API is on-demand to optimize performance:
- **Trigger**: Happens **only** when a user adds a new item to the cart or updates an item's quantity.
- **Behavior**: The Omnistore backend queries the Quotation API for the latest rate and updates the local cart item.
- **Fallback**: If the Quotation API is unreachable, the backend gracefully falls back to the locally cached prices.

### 🔌 Quotation API Integration Contract

The Omnistore backend interacts with the Quotation API using the following communication structure:

#### **Request**
- **Endpoint**: `GET /api/quotation/barcode/{barcode}?currentPrice={price}` (e.g., `http://localhost:8081/api/quotation/barcode/123456789?currentPrice=13.00`)
- **Headers**: Standard JSON requests
- **Request Body**: None (the barcode is passed strictly as a path variable)
- **Query Parameters**:
  - `currentPrice` (optional, decimal): The product's current stored price in the Omnistore backend database. Used by the Quotation API to detect mismatches.
- **Timeout**: `5 seconds` (configured in backend `WebClient` call)

#### **Response**
- **HTTP Status Codes**:
  - `200 OK`: When the barcode is recognized.
  - `404 Not Found`: When the barcode is not found in the Quotation database.
- **JSON Response Body (200 OK)**:
  ```json
  {
    "id": "uuid-string-here", 
    "name": "Product Name",
    "barcode": "123456789",
    "price": 14.50,
    "taxRate": 20.00,
    "imageUrl": "http://example.com/image.jpg",
    "description": "Product Description",
    "priceChanged": true,
    "oldPrice": 13.00
  }
  ```
  *(Note: The backend deserializes this JSON into a `QuotationResponse` DTO, mapping the price change acknowledgement fields: `priceChanged` and `oldPrice`).*

#### **🔄 Price Mismatch Acknowledgement & UI Alert**
- **Quotation Acknowledgement**: When `currentPrice` is provided and differs from the Quotation database price, the Quotation API responds with `"priceChanged": true` and `"oldPrice": <passedPrice>`.
- **Backend Sync & Recalculation**: If a mismatch is detected, the backend updates the product's price/taxRate locally, adjusts the active cart item's price, and sets transient attributes `priceChangedSync: true` and `priceChangedMessage` on the transaction object returned to the frontend.
- **Frontend Alert Dialog**: When the Angular UI processes the transaction response and detects `priceChangedSync === true`, it triggers a modal popup (`⚠️ AJUSTEMENT DE TARIF`) showing the updated price details to the cashier.

---


## 📋 Prerequisites

Before running the application, ensure you have the following installed:
- **Java 17+** (available on your `PATH`)
- **Node.js 18+** & npm
- **PostgreSQL** running locally on port `5432`
- *Note: Maven is pre-bundled in the project at `apache-maven-3.9.6/`.*

---

## ⚙️ Setup & Installation

### 1. Database Configuration
Open your PostgreSQL client and run the following queries to create the databases:
```sql
CREATE DATABASE omnistore;
CREATE DATABASE quotation;
```

> ⚙️ **Database Credentials**
> Both services connect using the following credentials configured in their respective `application.properties`:
> - **Username**: `postgres`
> - **Password**: `ayushdey`
>
> If your credentials differ, update the configuration files in:
> - `backend/src/main/resources/application.properties`
> - `Quotation/src/main/resources/application.properties`

### 2. Start the Quotation API (Port 8081)
Open a terminal in the project root directory and run:
```bash
cd Quotation
..\apache-maven-3.9.6\bin\mvn spring-boot:run
```
*Verify it is running by visiting [http://localhost:8081/api/quotation](http://localhost:8081/api/quotation) (should return a JSON array of 6 products).*

### 3. Start the Omnistore Backend (Port 8080)
Open a new terminal window in the project root directory and run:
```bash
cd backend
..\apache-maven-3.9.6\bin\mvn spring-boot:run
```
*Verify it is running by visiting [http://localhost:8080/api/products](http://localhost:8080/api/products).*

### 4. Start the Frontend App (Port 4200)
Open a new terminal window in the project root directory and run:
```bash
cd frontend
npm install   # Only required on first startup
npm start
```
*Open your browser and navigate to [http://localhost:4200](http://localhost:4200).*

---

## 🔄 Startup Order Summary

| Order | Service | Port | Directory | Command |
| :---: | :--- | :---: | :--- | :--- |
| **1** | **Quotation API** | `8081` | `Quotation/` | `..\apache-maven-3.9.6\bin\mvn spring-boot:run` |
| **2** | **Omnistore Backend** | `8080` | `backend/` | `..\apache-maven-3.9.6\bin\mvn spring-boot:run` |
| **3** | **Angular Frontend** | `4200` | `frontend/` | `npm start` |

---

## 🧪 Testing Price Sync

To make testing repeatable, database seeders reset prices back to baseline on startup:
- **Backend Startup**: Resets local prices to baseline (lower rates).
- **Quotation API Startup**: Resets quotation rates to correct (higher rates).

### Test Flow:
1. Open the UI at [http://localhost:4200](http://localhost:4200).
2. Observe **CLE MEULEUSE WOLFCRAFT** in the cart (starts at **€13.00**). Adjust its quantity to see it sync to **€14.50** from the Quotation API.
3. Try scanning or searching for any of these barcodes to add new products and witness instant price sync:

| Barcode | Product Name | Baseline Price | Synced (Quotation) Price |
| :--- | :--- | :---: | :---: |
| `0088381612036` | SCIE CIRCULAIRE MAKITA 1200W | €99.90 | **€109.99** |
| `3165140829120` | MEULEUSE D'ANGLE BOSCH PROFESSIONAL | €45.00 | **€49.90** |
| `8004828014524` | POSTE A SOUDER DECA I-ARC 120 | €119.00 | **€129.50** |
| `3148517894236` | COFFRET DE DOUILLES FACOM 38PCS | €149.00 | **€159.99** |
| `3253561771370` | NIVEAU LASER ROTATIF STANLEY FATMAX | €229.00 | **€245.00** |
| `4054278120364` | NETTOYEUR HAUTE PRESSION KARCHER K4 | €189.90 | **€199.99** |
| `3760015907421` | COMPRESSEUR BICYLINDRE MICHELIN 50L | €159.00 | **€175.00** |
| `3276007123985` | ETABLI D'ATELIER EN BOIS DEXTER | €89.00 | **€99.00** |
| `4054278049689` | ASPIRATEUR DE CHANTIER KARCHER WD3 | €69.90 | **€74.90** |
| `3276000702453` | PROJECTEUR DE CHANTIER LED DEXTER | €19.90 | **€24.90** |

---

## 🏃 Running Tests

You can run unit and integration tests for each project layer:

```bash
# Test Quotation API
cd Quotation
..\apache-maven-3.9.6\bin\mvn test

# Test Omnistore Backend
cd backend
..\apache-maven-3.9.6\bin\mvn test

# Test Frontend
cd frontend
npm test
```

---

## 🛠️ Troubleshooting

| Issue | Root Cause & Resolution |
| :--- | :--- |
| **`Connection refused` on 5432** | Ensure PostgreSQL local service is started. |
| **Database does not exist** | Verify you created `omnistore` and `quotation` databases. |
| **Port already in use** | Find and kill the process using port `8080`, `8081`, or `4200`, or reconfigure ports in `application.properties`. |
| **Quotation API Offline** | Backend will log a fallback warning and continue using the cached database price. |
| **CORS / Frontend API errors** | Verify backend is running on port `8080`. |
