# ReTix Backend Documentation

## Overview

ReTix backend is an Express.js REST API backed by MySQL, deployed as serverless functions on Vercel. It handles authentication (JWT), ticket marketplace operations, virtual wallet with real balance tracking, buyer-seller chat, and image uploads via Cloudinary.

**Base URL:** `https://backend-three-phi-61.vercel.app/api/`

---

## Architecture

```
Client (Android/Retrofit)
  │
  ▼
Vercel (serverless)
  │
  ▼
api/index.js          ← Express app entry, CORS, JSON parser, route mounting
  │
  ├── api/auth/*      ← Signup, login, token refresh
  ├── api/users/*     ← Profile, stats
  ├── api/tickets/*   ← CRUD, buy, listings, purchases
  ├── api/wallet/*    ← Balance, transactions, earnings
  ├── api/chat/*      ← Conversations, messages
  └── api/upload/*    ← Image upload to Cloudinary
  │
  ▼
MySQL (via mysql2)    ← Connection pool, per-request connections with SSL
  ▼
Cloudinary            ← Image storage (event images, profile avatars)
```

### Key Libraries

| Library | Purpose |
|---------|---------|
| express | HTTP framework |
| mysql2 | MySQL client with connection pooling |
| jsonwebtoken | JWT sign/verify |
| bcryptjs | Password hashing |
| multer | File upload handling |
| cloudinary | Image upload/delete |
| cors | Cross-origin support |

---

## Database Schema

Five tables. Full DDL in `backend/scripts/schema.sql`.

### `users`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID (CHAR(36)) | Primary key, auto-generated |
| email | VARCHAR(255) | Unique, not null |
| password_hash | VARCHAR(255) | bcrypt hash |
| name | VARCHAR(255) | Not null |
| profile_image_url | TEXT | Nullable |
| wallet_balance | DECIMAL(10,2) | Default 0.00 |
| created_at | TIMESTAMP | Default CURRENT_TIMESTAMP |

### `tickets`

| Column | Type | Notes |
|--------|------|-------|
| id | CHAR(36) | Primary key |
| title | VARCHAR(255) | Not null |
| description | TEXT | Nullable |
| original_price | DECIMAL(10,2) | Added via migration |
| price | DECIMAL(10,2) | Selling price |
| event_date | VARCHAR(50) | Not null |
| image_url | TEXT | Nullable |
| ticket_file_url | TEXT | Nullable (PDF/image of actual ticket) |
| seller_id | CHAR(36) | FK → users.id |
| status | ENUM('available','sold','expired') | Default 'available' |
| buyer_id | CHAR(36) | Nullable, FK → users.id |
| created_at | TIMESTAMP | Default CURRENT_TIMESTAMP |

### `transactions`

| Column | Type | Notes |
|--------|------|-------|
| id | CHAR(36) | Primary key |
| user_id | CHAR(36) | FK → users.id |
| type | ENUM('credit','debit') | Not null |
| amount | DECIMAL(10,2) | Not null |
| description | VARCHAR(255) | Nullable |
| ticket_id | CHAR(36) | Nullable, FK → tickets.id |
| status | ENUM('processing','available') | Default 'processing' |
| created_at | TIMESTAMP | Default CURRENT_TIMESTAMP |

### `conversations`

| Column | Type | Notes |
|--------|------|-------|
| id | VARCHAR(255) | Composite key "user1_user2" (sorted IDs) |
| user1_id | CHAR(36) | FK → users.id |
| user2_id | CHAR(36) | FK → users.id |
| created_at | TIMESTAMP | Default CURRENT_TIMESTAMP |

### `messages`

| Column | Type | Notes |
|--------|------|-------|
| id | CHAR(36) | Primary key |
| conversation_id | VARCHAR(255) | FK → conversations.id |
| sender_id | CHAR(36) | FK → users.id |
| receiver_id | CHAR(36) | FK → users.id |
| message | TEXT | Not null |
| created_at | TIMESTAMP | Default CURRENT_TIMESTAMP |

---

## API Reference

All endpoints are prefixed with `/api`. Request/response bodies are JSON.

### Authentication

#### `POST /api/auth/signup`

Create a new account.

**Request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secret123"
}
```

**Response (201):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "token": "eyJhbGciOi...",
  "user": {
    "id": "uuid",
    "email": "john@example.com",
    "name": "John Doe"
  }
}
```

**Error (409):** `EMAIL_EXISTS`
**Error (400):** `WEAK_PASSWORD` (min 6 chars), `ALL_FIELDS_REQUIRED`

---

#### `POST /api/auth/login`

Authenticate and receive a JWT.

**Request:**
```json
{
  "email": "john@example.com",
  "password": "secret123"
}
```

**Response (200):**
```json
{
  "success": true,
  "token": "eyJhbGciOi...",
  "user": {
    "id": "uuid",
    "email": "john@example.com",
    "name": "John Doe"
  }
}
```

**Error (401):** `INVALID_CREDENTIALS`

---

#### `POST /api/auth/refresh`

Refresh an existing JWT.

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "success": true,
  "token": "eyJhbGciOi..."
}
```

---

### Users

#### `GET /api/users/profile`

**Auth: Required**

**Response (200):**
```json
{
  "success": true,
  "user": {
    "id": "uuid",
    "name": "John Doe",
    "email": "john@example.com",
    "profile_image_url": "https://res.cloudinary.com/...",
    "wallet_balance": 500.00,
    "created_at": "2025-01-15T10:30:00Z"
  }
}
```

---

#### `PUT /api/users/profile`

**Auth: Required**

**Request:**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "profile_image_url": "https://res.cloudinary.com/..."
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Profile updated",
  "user": { ... }
}
```

---

#### `GET /api/users/stats`

**Auth: Required**

**Response (200):**
```json
{
  "success": true,
  "activeListings": 3,
  "sold": 5,
  "earnings": 2500.00
}
```

---

### Tickets

#### `GET /api/tickets`

**Auth: Optional** (includes ownership info if authenticated)

**Query params:** None (returns all available tickets)

**Response (200):**
```json
{
  "success": true,
  "tickets": [
    {
      "id": "uuid",
      "title": "Coldplay Concert",
      "description": "Lower bowl seats",
      "original_price": 5000.00,
      "price": 3500.00,
      "event_date": "2025-03-20",
      "image_url": "https://res.cloudinary.com/...",
      "ticket_file_url": null,
      "seller_id": "uuid",
      "seller_name": "Arjun",
      "status": "available",
      "is_owner": false,
      "created_at": "2025-01-15T10:30:00Z"
    }
  ]
}
```

---

#### `GET /api/tickets/[id]`

**Auth: Optional**

**Response (200):** Single ticket object (same shape as above).

---

#### `POST /api/tickets/create`

**Auth: Required**

**Request (multipart/form-data via multer):**
| Field | Type | Required |
|-------|------|----------|
| title | string | Yes |
| description | string | No |
| price | number | Yes |
| original_price | number | No |
| event_date | string | Yes |
| image | file | No (uploaded to Cloudinary if provided) |

**Response (201):**
```json
{
  "success": true,
  "message": "Ticket created successfully",
  "ticket": { ... }
}
```

---

#### `GET /api/tickets/my-listings`

**Auth: Required**

Returns tickets created by the authenticated user.

---

#### `GET /api/tickets/my-purchases`

**Auth: Required**

Returns tickets purchased by the authenticated user.

---

#### `POST /api/tickets/buy/[id]`

**Auth: Required**

Purchases a ticket. Deducts price from buyer's wallet, credits seller's wallet (in processing status). Creates a debit transaction for the buyer and a credit transaction for the seller.

**Response (200):**
```json
{
  "success": true,
  "message": "Ticket purchased successfully",
  "ticket": { ... },
  "transaction": { ... }
}
```

**Error (400):** Ticket not available, insufficient balance, self-purchase
**Error (404):** Ticket not found

---

#### `POST /api/tickets/confirm-entry`

**Auth: Required**

**Request:**
```json
{
  "ticket_id": "uuid"
}
```

Confirms ticket entry (marks transaction as available for seller payout).

---

### Wallet

#### `GET /api/wallet/balance`

**Auth: Required**

**Response (200):**
```json
{
  "success": true,
  "balance": 500.00,
  "pending_balance": 200.00
}
```

---

#### `GET /api/wallet/transactions`

**Auth: Required**

**Response (200):**
```json
{
  "success": true,
  "transactions": [
    {
      "id": "uuid",
      "type": "debit",
      "amount": 3500.00,
      "description": "Purchased: Coldplay Concert",
      "ticket_id": "uuid",
      "status": "processing",
      "created_at": "2025-01-15T10:30:00Z"
    }
  ]
}
```

---

#### `GET /api/wallet/monthly-earnings`

**Auth: Required**

Returns earnings grouped by month for chart rendering.

**Response (200):**
```json
{
  "success": true,
  "earnings": [
    { "month": "Jan", "amount": 5000.00 },
    { "month": "Feb", "amount": 3200.00 }
  ]
}
```

---

### Chat

#### `GET /api/chat/conversations`

**Auth: Required**

Returns all conversations for the authenticated user, ordered by most recent message.

**Response (200):**
```json
{
  "success": true,
  "conversations": [
    {
      "id": "uuid1_uuid2",
      "other_user": {
        "id": "uuid",
        "name": "Priya"
      },
      "last_message": "Is this still available?",
      "last_message_time": "2025-01-15T10:30:00Z"
    }
  ]
}
```

---

#### `GET /api/chat/messages/[conversationId]`

**Auth: Required**

Returns messages for a conversation, ordered chronologically.

---

#### `POST /api/chat/send`

**Auth: Required**

**Request:**
```json
{
  "receiver_id": "uuid",
  "message": "Is this still available?"
}
```

Creates conversation if it doesn't exist (composite ID from sorted user IDs).

---

### Upload

#### `POST /api/upload/image`

**Auth: Required**

**Request:** `multipart/form-data` with `image` file field.

**Response (200):**
```json
{
  "success": true,
  "url": "https://res.cloudinary.com/...",
  "public_id": "retix/abc123"
}
```

---

## Authentication Flow

1. Client sends `POST /api/auth/signup` or `POST /api/auth/login`
2. Server validates credentials, hashes password (signup) or verifies hash (login)
3. Server returns JWT (default 7-day expiry)
4. Client includes `Authorization: Bearer <token>` header on all subsequent requests
5. `authMiddleware` verifies JWT and attaches `req.user = { id, email }`
6. `optionalAuthMiddleware` does the same but doesn't reject if token is missing

---

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | MySQL connection string | `mysql://user:pass@host:3306/dbname?ssl=true` |
| `JWT_SECRET` | Secret key for signing tokens | `your-secret-key` |
| `JWT_EXPIRES_IN` | Token expiry period | `7d` (default) |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | `my-cloud` |
| `CLOUDINARY_API_KEY` | Cloudinary API key | `123456789` |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret | `abc123def456` |
| `CLOUDINARY_FOLDER` | Upload folder in Cloudinary | `retix` |

---

## Deployment

### Vercel (Current)

The backend is deployed on Vercel as serverless functions. `vercel.json` routes all requests to `api/index.js` using the `@vercel/node` builder.

```json
{
  "version": 2,
  "builds": [{ "src": "api/index.js", "use": "@vercel/node" }],
  "routes": [{ "src": "/(.*)", "dest": "api/index.js" }]
}
```

### Steps

1. Push `backend/` to a Git repository
2. Import project in Vercel
3. Root directory set to `backend/`
4. Add all environment variables in Vercel dashboard
5. Deploy — Vercel auto-detects the Express app

### Database Setup

```bash
# Run schema
mysql -u root -p < backend/scripts/schema.sql

# Seed test data
cd backend && node scripts/seed-database.js

# Run migrations
mysql -u root -p < backend/scripts/migrate-add-original-price.sql
```

### Test Accounts (seeded)

| Email | Password | Name |
|-------|----------|------|
| arjun@test.com | password123 | Arjun |
| priya@test.com | password123 | Priya |
| rahul@test.com | password123 | Rahul |
| test@test.com | password123 | Test User |

---

## Testing

Tests use Jest + supertest, located in `backend/__tests__/`.

```bash
cd backend
npm test
```

Test files:
- `auth.test.js` — signup, login, refresh
- `tickets.test.js` — CRUD, buy, listings, purchases
- `users.test.js` — profile, stats
- `wallet.test.js` — balance, transactions, earnings

---

## Utility Scripts

| Script | Purpose |
|--------|---------|
| `scripts/seed-database.js` | Insert test users and sample tickets |
| `scripts/test-api.js` | Manual API endpoint testing |
| `scripts/test-suite.js` | Full test suite |
| `scripts/add-wallet-credit.js` | Admin: add credit to a user's wallet |

---

## File Structure

```
backend/
├── api/
│   ├── index.js              # Express app entry, CORS, routes, error handlers
│   ├── auth/
│   │   ├── signup.js
│   │   ├── login.js
│   │   └── refresh.js
│   ├── users/
│   │   ├── profile.js
│   │   └── stats.js
│   ├── tickets/
│   │   ├── index.js
│   │   ├── [id].js
│   │   ├── create.js
│   │   ├── my-listings.js
│   │   ├── my-purchases.js
│   │   ├── buy/
│   │   │   └── [id].js
│   │   └── confirm-entry.js
│   ├── wallet/
│   │   ├── balance.js
│   │   ├── transactions.js
│   │   └── monthly-earnings.js
│   ├── chat/
│   │   ├── conversations.js
│   │   ├── messages/
│   │   │   └── [id].js
│   │   └── send.js
│   └── upload/
│       └── image.js
├── lib/
│   ├── db.js                 # MySQL connection pool, per-request connections
│   ├── jwt.js                # JWT sign/verify/extract helpers
│   ├── auth.js               # authMiddleware, optionalAuthMiddleware
│   └── cloudinary.js         # Upload/delete image helpers
├── scripts/
│   ├── schema.sql            # Database DDL
│   ├── seed-database.js      # Test data seeder
│   ├── test-api.js           # Manual API tests
│   ├── test-suite.js         # Test suite runner
│   ├── add-wallet-credit.js  # Admin wallet credit script
│   └── migrate-add-original-price.sql
├── __tests__/                # Jest + supertest tests
├── package.json
├── vercel.json
└── .gitignore
```