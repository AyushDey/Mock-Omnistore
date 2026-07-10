# Components

## CorsConfig

- Kind: `class`
- Layer: `configuration`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/config/CorsConfig.java`
- Methods: `addCorsMappings`, `corsConfigurer`

## DatabaseSeeder

- Kind: `class`
- Layer: `configuration`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/config/DatabaseSeeder.java`
- Bases: `CommandLineRunner`
- Methods: `recalculateTotals`, `run`
- Fields: `productRepository`, `transactionRepository`

## QuotationClientConfig

- Kind: `class`
- Layer: `configuration`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/config/QuotationClientConfig.java`
- Methods: `quotationWebClient`
- Fields: `quotationApiBaseUrl`

## ProductController

- Kind: `controller`
- Layer: `api`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/controller/ProductController.java`
- Methods: `create`, `getByBarcode`, `listAll`, `search`
- Fields: `productService`

## TransactionController

- Kind: `controller`
- Layer: `api`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/controller/TransactionController.java`
- Methods: `abandon`, `addItem`, `getActive`, `getAll`, `getSuspended`, `pay`, `removeItem`, `resume`, `suspend`, `toggleInvoice`, `updateItemQuantity`
- Fields: `transactionService`

## MockOmnistoreUiApplication

- Kind: `class`
- Layer: `application`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/MockOmnistoreUiApplication.java`
- Methods: `main`

## Payment

- Kind: `entity`
- Layer: `domain`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/model/Payment.java`
- Methods: `onCreate`
- Fields: `amount`, `createdAt`, `id`, `method`, `transaction`

## PaymentMethod

- Kind: `enum`
- Layer: `domain`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/model/PaymentMethod.java`

## Product

- Kind: `entity`
- Layer: `domain`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/model/Product.java`
- Fields: `barcode`, `id`, `imageUrl`, `name`, `price`, `taxRate`

## QuotationResponse

- Kind: `entity`
- Layer: `domain`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/model/QuotationResponse.java`
- Fields: `barcode`, `description`, `imageUrl`, `name`, `price`, `priceChanged`, `taxRate`

## Transaction

- Kind: `entity`
- Layer: `domain`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/model/Transaction.java`
- Methods: `onCreate`, `onUpdate`
- Fields: `createdAt`, `discountAmount`, `id`, `invoiceRequired`, `items`, `payments`, `status`, `taxAmount`, `totalAmount`, `updatedAt`

## TransactionItem

- Kind: `entity`
- Layer: `domain`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/model/TransactionItem.java`
- Fields: `id`, `oldPrice`, `price`, `priceChanged`, `product`, `quantity`, `transaction`

## TransactionStatus

- Kind: `enum`
- Layer: `domain`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/model/TransactionStatus.java`

## PaymentRepository

- Kind: `interface`
- Layer: `persistence`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/repository/PaymentRepository.java`
- Bases: `JpaRepository`

## ProductRepository

- Kind: `interface`
- Layer: `persistence`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/repository/ProductRepository.java`
- Bases: `JpaRepository`

## TransactionItemRepository

- Kind: `interface`
- Layer: `persistence`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/repository/TransactionItemRepository.java`
- Bases: `JpaRepository`

## TransactionRepository

- Kind: `interface`
- Layer: `persistence`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/repository/TransactionRepository.java`
- Bases: `JpaRepository`

## ProductService

- Kind: `service`
- Layer: `service`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/service/ProductService.java`
- Methods: `getProductByBarcode`, `getProductById`, `listAllProducts`, `saveProduct`, `searchProducts`
- Fields: `productRepository`

## TransactionService

- Kind: `service`
- Layer: `service`
- Language: `Java`
- Source: `backend/src/main/java/com/omnistore/mockui/service/TransactionService.java`
- Methods: `abandonActiveTransaction`, `addItemToActiveTransaction`, `addPaymentToActiveTransaction`, `getActiveTransaction`, `listAllTransactions`, `listSuspendedTransactions`, `recalculateTotals`, `removeItem`, `resumeTransaction`, `setInvoiceRequired`, `suspendActiveTransaction`, `updateItemQuantity`
- Fields: `log`, `productRepository`, `quotationWebClient`, `transactionItemRepository`, `transactionRepository`

## ProductControllerTest

- Kind: `controller`
- Layer: `api`
- Language: `Java`
- Source: `backend/src/test/java/com/omnistore/mockui/controller/ProductControllerTest.java`
- Fields: `mockMvc`, `product1`, `product2`, `productService`

## TransactionControllerTest

- Kind: `controller`
- Layer: `api`
- Language: `Java`
- Source: `backend/src/test/java/com/omnistore/mockui/controller/TransactionControllerTest.java`
- Fields: `itemId`, `mockMvc`, `transaction`, `transactionId`, `transactionService`

## ProductServiceTest

- Kind: `service`
- Layer: `service`
- Language: `Java`
- Source: `backend/src/test/java/com/omnistore/mockui/ProductServiceTest.java`
- Fields: `product1`, `product2`, `productRepository`, `productService`

## TransactionServiceTest

- Kind: `service`
- Layer: `service`
- Language: `Java`
- Source: `backend/src/test/java/com/omnistore/mockui/TransactionServiceTest.java`
- Methods: `mockQuotationApiFailure`, `mockQuotationApiResponse`
- Fields: `activeTransaction`, `product1`, `product2`, `productRepository`, `quotationWebClient`, `requestHeadersSpec`, `requestHeadersUriSpec`, `responseSpec`, `transactionItemRepository`, `transactionRepository`, `transactionService`

## App

- Kind: `class`
- Layer: `application`
- Language: `TypeScript`
- Source: `frontend/src/app/app.ts`

## Toast

- Kind: `interface`
- Layer: `application`
- Language: `TypeScript`
- Source: `frontend/src/app/app.ts`

## TransactionService

- Kind: `service`
- Layer: `service`
- Language: `TypeScript`
- Source: `frontend/src/app/transaction.service.ts`
- Methods: `loadActiveTransaction`, `loadAllTransactions`, `loadSuspendedTransactions`, `removeItem`, `scanAndAddItem`, `suspendTransaction`, `tap`, `updateItemQuantity`

## Product

- Kind: `interface`
- Layer: `service`
- Language: `TypeScript`
- Source: `frontend/src/app/transaction.service.ts`

## TransactionItem

- Kind: `interface`
- Layer: `service`
- Language: `TypeScript`
- Source: `frontend/src/app/transaction.service.ts`

## Payment

- Kind: `interface`
- Layer: `service`
- Language: `TypeScript`
- Source: `frontend/src/app/transaction.service.ts`

## Transaction

- Kind: `interface`
- Layer: `service`
- Language: `TypeScript`
- Source: `frontend/src/app/transaction.service.ts`

## CorsConfig

- Kind: `class`
- Layer: `configuration`
- Language: `Java`
- Source: `Quotation/src/main/java/com/quotation/api/config/CorsConfig.java`
- Methods: `addCorsMappings`, `corsConfigurer`

## QuotationSeeder

- Kind: `class`
- Layer: `configuration`
- Language: `Java`
- Source: `Quotation/src/main/java/com/quotation/api/config/QuotationSeeder.java`
- Bases: `CommandLineRunner`
- Methods: `run`
- Fields: `quotationItemRepository`

## QuotationController

- Kind: `controller`
- Layer: `api`
- Language: `Java`
- Source: `Quotation/src/main/java/com/quotation/api/controller/QuotationController.java`
- Methods: `getAll`, `getByBarcode`, `search`
- Fields: `log`, `quotationService`

## QuotationItem

- Kind: `entity`
- Layer: `domain`
- Language: `Java`
- Source: `Quotation/src/main/java/com/quotation/api/model/QuotationItem.java`
- Fields: `barcode`, `description`, `id`, `imageUrl`, `name`, `price`, `priceChanged`, `taxRate`

## QuotationApiApplication

- Kind: `class`
- Layer: `application`
- Language: `Java`
- Source: `Quotation/src/main/java/com/quotation/api/QuotationApiApplication.java`
- Methods: `main`

## QuotationItemRepository

- Kind: `interface`
- Layer: `persistence`
- Language: `Java`
- Source: `Quotation/src/main/java/com/quotation/api/repository/QuotationItemRepository.java`
- Bases: `JpaRepository`

## QuotationService

- Kind: `service`
- Layer: `service`
- Language: `Java`
- Source: `Quotation/src/main/java/com/quotation/api/service/QuotationService.java`
- Methods: `getAll`, `getByBarcode`, `getById`, `search`
- Fields: `quotationItemRepository`

## QuotationControllerTest

- Kind: `controller`
- Layer: `api`
- Language: `Java`
- Source: `Quotation/src/test/java/com/quotation/api/controller/QuotationControllerTest.java`
- Fields: `item1`, `item2`, `mockMvc`, `quotationService`

## QuotationServiceTest

- Kind: `service`
- Layer: `service`
- Language: `Java`
- Source: `Quotation/src/test/java/com/quotation/api/service/QuotationServiceTest.java`
- Fields: `item1`, `item2`, `quotationItemRepository`, `quotationService`
