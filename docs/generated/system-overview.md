# System Overview

This document provides a logical structural overview of the codebase components, classified by system modules, API endpoints, and database schemas.

## Codebase Modules

The code modules discovered during incremental static analysis:

### Frontend Angular Client

| Module File / Path | Language | Architectural Layer |
| :--- | :--- | :--- |
| `frontend/angular.json` | configuration | Configuration |
| `frontend/package.json` | Node.js manifest | Configuration |
| `frontend/src/app/app.config.ts` | TypeScript | Configuration |
| `frontend/src/app/app.routes.ts` | TypeScript | Api |
| `frontend/src/app/app.spec.ts` | TypeScript | Application |
| `frontend/src/app/app.ts` | TypeScript | Application |
| `frontend/src/app/transaction.service.spec.ts` | TypeScript | Service |
| `frontend/src/app/transaction.service.ts` | TypeScript | Service |
| `frontend/src/main.ts` | TypeScript | Application |
| `frontend/tsconfig.app.json` | configuration | Configuration |
| `frontend/tsconfig.json` | configuration | Configuration |
| `frontend/tsconfig.spec.json` | configuration | Configuration |

### Omnistore Backend API

| Module File / Path | Language | Architectural Layer |
| :--- | :--- | :--- |
| `backend/pom.xml` | configuration | Configuration |
| `backend/src/main/java/com/omnistore/mockui/MockOmnistoreUiApplication.java` | Java | Application |
| `backend/src/main/java/com/omnistore/mockui/config/CorsConfig.java` | Java | Configuration |
| `backend/src/main/java/com/omnistore/mockui/config/DatabaseSeeder.java` | Java | Configuration |
| `backend/src/main/java/com/omnistore/mockui/config/QuotationClientConfig.java` | Java | Configuration |
| `backend/src/main/java/com/omnistore/mockui/controller/ProductController.java` | Java | Api |
| `backend/src/main/java/com/omnistore/mockui/controller/TransactionController.java` | Java | Api |
| `backend/src/main/java/com/omnistore/mockui/model/Payment.java` | Java | Domain |
| `backend/src/main/java/com/omnistore/mockui/model/PaymentMethod.java` | Java | Domain |
| `backend/src/main/java/com/omnistore/mockui/model/Product.java` | Java | Domain |
| `backend/src/main/java/com/omnistore/mockui/model/QuotationResponse.java` | Java | Domain |
| `backend/src/main/java/com/omnistore/mockui/model/Transaction.java` | Java | Domain |
| `backend/src/main/java/com/omnistore/mockui/model/TransactionItem.java` | Java | Domain |
| `backend/src/main/java/com/omnistore/mockui/model/TransactionStatus.java` | Java | Domain |
| `backend/src/main/java/com/omnistore/mockui/repository/PaymentRepository.java` | Java | Persistence |
| `backend/src/main/java/com/omnistore/mockui/repository/ProductRepository.java` | Java | Persistence |
| `backend/src/main/java/com/omnistore/mockui/repository/TransactionItemRepository.java` | Java | Persistence |
| `backend/src/main/java/com/omnistore/mockui/repository/TransactionRepository.java` | Java | Persistence |
| `backend/src/main/java/com/omnistore/mockui/service/ProductService.java` | Java | Service |
| `backend/src/main/java/com/omnistore/mockui/service/TransactionService.java` | Java | Service |
| `backend/src/test/java/com/omnistore/mockui/ProductServiceTest.java` | Java | Service |
| `backend/src/test/java/com/omnistore/mockui/TransactionServiceTest.java` | Java | Service |
| `backend/src/test/java/com/omnistore/mockui/controller/ProductControllerTest.java` | Java | Api |
| `backend/src/test/java/com/omnistore/mockui/controller/TransactionControllerTest.java` | Java | Api |

### Quotation Microservice

| Module File / Path | Language | Architectural Layer |
| :--- | :--- | :--- |
| `Quotation/pom.xml` | configuration | Configuration |
| `Quotation/src/main/java/com/quotation/api/QuotationApiApplication.java` | Java | Application |
| `Quotation/src/main/java/com/quotation/api/config/CorsConfig.java` | Java | Configuration |
| `Quotation/src/main/java/com/quotation/api/config/QuotationSeeder.java` | Java | Configuration |
| `Quotation/src/main/java/com/quotation/api/controller/QuotationController.java` | Java | Api |
| `Quotation/src/main/java/com/quotation/api/model/QuotationItem.java` | Java | Domain |
| `Quotation/src/main/java/com/quotation/api/repository/QuotationItemRepository.java` | Java | Persistence |
| `Quotation/src/main/java/com/quotation/api/service/QuotationService.java` | Java | Service |
| `Quotation/src/test/java/com/quotation/api/controller/QuotationControllerTest.java` | Java | Api |
| `Quotation/src/test/java/com/quotation/api/service/QuotationServiceTest.java` | Java | Service |


## API Routes & Endpoints

REST API endpoints exposed by the backend services:

### Omnistore Backend API

| Method | Endpoint Path | Code Handler | Framework |
| :--- | :--- | :--- | :--- |
| `POST` | `/abandon` | `PostMapping` | Spring-like |
| `GET` | `/active` | `GetMapping` | Spring-like |
| `ANY` | `/api/products` | `RequestMapping` | Spring-like |
| `ANY` | `/api/transactions` | `RequestMapping` | Spring-like |
| `GET` | `/barcode/{barcode}` | `GetMapping` | Spring-like |
| `POST` | `/invoice` | `PostMapping` | Spring-like |
| `POST` | `/items` | `PostMapping` | Spring-like |
| `DELETE` | `/items/{itemId}` | `DeleteMapping` | Spring-like |
| `PUT` | `/items/{itemId}` | `PutMapping` | Spring-like |
| `POST` | `/pay` | `PostMapping` | Spring-like |
| `POST` | `/resume/{id}` | `PostMapping` | Spring-like |
| `GET` | `/search` | `GetMapping` | Spring-like |
| `POST` | `/suspend` | `PostMapping` | Spring-like |
| `GET` | `/suspended` | `GetMapping` | Spring-like |

### Quotation Microservice

| Method | Endpoint Path | Code Handler | Framework |
| :--- | :--- | :--- | :--- |
| `ANY` | `/api/quotation` | `RequestMapping` | Spring-like |
| `GET` | `/barcode/{barcode}` | `GetMapping` | Spring-like |
| `GET` | `/search` | `GetMapping` | Spring-like |


## Database & Domain Entities

Data models, database tables, and schema mappings:

### Omnistore Backend API

| Entity Name | Source File | Discovered Attributes / Fields |
| :--- | :--- | :--- |
| **Payment** | `backend/src/main/java/com/omnistore/mockui/model/Payment.java` | `amount`, `createdAt`, `id`, `method`, `transaction` |
| **Product** | `backend/src/main/java/com/omnistore/mockui/model/Product.java` | `barcode`, `id`, `imageUrl`, `name`, `price`, `taxRate` |
| **QuotationResponse** | `backend/src/main/java/com/omnistore/mockui/model/QuotationResponse.java` | `barcode`, `description`, `imageUrl`, `name`, `price`, `priceChanged`, `taxRate` |
| **Transaction** | `backend/src/main/java/com/omnistore/mockui/model/Transaction.java` | `createdAt`, `discountAmount`, `id`, `invoiceRequired`, `items`, `payments`, `status`, `taxAmount`, `totalAmount`, `updatedAt` |
| **TransactionItem** | `backend/src/main/java/com/omnistore/mockui/model/TransactionItem.java` | `id`, `oldPrice`, `price`, `priceChanged`, `product`, `quantity`, `transaction` |

### Quotation Microservice

| Entity Name | Source File | Discovered Attributes / Fields |
| :--- | :--- | :--- |
| **QuotationItem** | `Quotation/src/main/java/com/quotation/api/model/QuotationItem.java` | `barcode`, `description`, `id`, `imageUrl`, `name`, `price`, `priceChanged`, `taxRate` |

