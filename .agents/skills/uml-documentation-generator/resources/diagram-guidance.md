# Diagram Guidance

## Component Diagram

Use for high-level architecture.

```mermaid
flowchart LR
    User[User] --> UI[UI / Client Layer]
    UI --> API[API / Controller Layer]
    API --> Service[Service Layer]
    Service --> Repository[Persistence Layer]
    Repository --> Database[(Database)]
```

## Class Diagram

Use for classes, interfaces, inheritance, and simple relationships.

```mermaid
classDiagram
    class User
    class UserService
    class UserRepository
    UserService --> UserRepository
```

## Sequence Diagram

Use for request or workflow behavior.

```mermaid
sequenceDiagram
    actor User
    participant Controller
    participant Service
    participant Repository
    participant Database
    User->>Controller: Request
    Controller->>Service: Process
    Service->>Repository: Load or save
    Repository->>Database: Query
    Database-->>Repository: Result
    Repository-->>Service: Entity
    Service-->>Controller: Response
    Controller-->>User: Response
```

## ER Diagram

Use for SQL, Prisma, ORM entities, and schema relationships.

```mermaid
erDiagram
    USER ||--o{ ORDER : places
    USER {
        string id
        string name
    }
    ORDER {
        string id
        string user_id
    }
```