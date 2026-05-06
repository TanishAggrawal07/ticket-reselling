# ReTix API Documentation

## Base URL

When deployed on Vercel, the base URL will be your Vercel deployment URL.

For local development: `http://localhost:3000`

All endpoints are prefixed with `/api` when deployed on Vercel.

---

## Authentication

Most endpoints require a JWT token sent in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

The token is obtained from the login or signup endpoints.

---

## Response Format

All responses follow a consistent format:

**Success:**
```json
{
  "success": true,
  "data": { ... }
}
```

**Error:**
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message"
  }
}
```

---

## Endpoints

### Authentication

#### POST /auth/signup

Register a new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securepassword",
  "name": "John Doe"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "name": "John Doe",
      "walletBalance": 0,
      "profileImageUrl": ""
    }
  }
}
```

---

#### POST /auth/login

Authenticate an existing user.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securepassword"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "name": "John Doe",
      "walletBalance": 0,
      "profileImageUrl": ""
    }
  }
}
```

---

#### POST /auth/refresh

Refresh an expiring JWT token.

**Request:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### Users

#### GET /users/profile

Get the current user's profile. **Requires authentication.**

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "John Doe",
    "profileImageUrl": "https://...",
    "walletBalance": 1500,
    "createdAt": "2024-01-15T10:30:00.000Z"
  }
}
```

---

#### PUT /users/profile

Update the current user's profile. **Requires authentication.**

**Request:**
```json
{
  "name": "Jane Doe",
  "profileImageUrl": "https://cloudinary.com/..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "Jane Doe",
    "profileImageUrl": "https://cloudinary.com/...",
    "walletBalance": 1500,
    "createdAt": "2024-01-15T10:30:00.000Z"
  }
}
```

---

#### GET /users/stats

Get the current user's ticket statistics. **Requires authentication.**

**Response:**
```json
{
  "success": true,
  "data": {
    "activeListings": 3,
    "ticketsSold": 5,
    "totalEarnings": 7500
  }
}
```

---

### Tickets

#### GET /tickets

List all available tickets. Public endpoint (no auth required).

**Query Parameters:**
- `limit` (number, optional): Maximum tickets to return (default: 50, max: 100)
- `offset` (number, optional): Offset for pagination (default: 0)

**Response:**
```json
{
  "success": true,
  "data": {
    "tickets": [
      {
        "id": "uuid",
        "title": "Coldplay Concert",
        "description": "Front row seats",
        "price": 5000,
        "eventDate": "Sat, Jan 18 • 7:00 PM",
        "imageUrl": "https://cloudinary.com/...",
        "sellerId": "uuid",
        "sellerName": "John Doe",
        "status": "available",
        "createdAt": "2024-01-10T14:20:00.000Z",
        "isOwner": false
      }
    ],
    "count": 1
  }
}
```

---

#### GET /tickets/:id

Get details of a specific ticket.

**Response:**
```json
{
  "success": true,
  "data": {
    "ticket": {
      "id": "uuid",
      "title": "Coldplay Concert",
      "description": "Front row seats",
      "price": 5000,
      "eventDate": "Sat, Jan 18 • 7:00 PM",
      "imageUrl": "https://cloudinary.com/...",
      "sellerId": "uuid",
      "sellerName": "John Doe",
      "status": "available",
      "buyerId": null,
      "createdAt": "2024-01-10T14:20:00.000Z",
      "updatedAt": "2024-01-10T14:20:00.000Z",
      "isOwner": false
    }
  }
}
```

---

#### POST /tickets

Create a new ticket listing. **Requires authentication.**

**Request:**
```json
{
  "title": "Coldplay Concert",
  "description": "Front row seats",
  "price": 5000,
  "eventDate": "Sat, Jan 18 • 7:00 PM",
  "imageUrl": "https://cloudinary.com/..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "ticket": {
      "id": "uuid",
      "title": "Coldplay Concert",
      "description": "Front row seats",
      "price": 5000,
      "eventDate": "Sat, Jan 18 • 7:00 PM",
      "imageUrl": "https://cloudinary.com/...",
      "sellerId": "uuid",
      "sellerName": "John Doe",
      "status": "available",
      "createdAt": "2024-01-15T10:30:00.000Z"
    }
  }
}
```

---

#### GET /tickets/my-listings

Get current user's ticket listings. **Requires authentication.**

**Query Parameters:**
- `limit` (number, optional): Maximum tickets to return (default: 50, max: 100)
- `offset` (number, optional): Offset for pagination (default: 0)

**Response:** Same format as GET /tickets

---

#### GET /tickets/my-purchases

Get current user's purchased tickets. **Requires authentication.**

**Query Parameters:**
- `limit` (number, optional): Maximum tickets to return (default: 50, max: 100)
- `offset` (number, optional): Offset for pagination (default: 0)

**Response:** Same format as GET /tickets

---

#### POST /tickets/:id/buy

Purchase a ticket. **Requires authentication.**

- Deducts ticket price from buyer's wallet
- Credits ticket price to seller's wallet
- Marks ticket as sold
- Creates transaction records

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "Ticket purchased successfully",
    "ticketId": "uuid",
    "price": 5000,
    "newBalance": 10000
  }
}
```

**Errors:**
- `TICKET_NOT_AVAILABLE`: Ticket already sold
- `INSUFFICIENT_BALANCE`: Buyer doesn't have enough funds
- `CANNOT_BUY_OWN`: Buyer is the seller

---

### Wallet

#### GET /wallet/balance

Get current user's wallet balance. **Requires authentication.**

**Response:**
```json
{
  "success": true,
  "data": {
    "balance": 15000
  }
}
```

---

#### GET /wallet/transactions

Get current user's transaction history. **Requires authentication.**

**Query Parameters:**
- `limit` (number, optional): Maximum transactions to return (default: 50, max: 100)
- `offset` (number, optional): Offset for pagination (default: 0)

**Response:**
```json
{
  "success": true,
  "data": {
    "transactions": [
      {
        "id": "uuid",
        "type": "credit",
        "amount": 5000,
        "description": "Sold: Coldplay Concert",
        "ticketId": "uuid",
        "ticketTitle": "Coldplay Concert",
        "createdAt": "2024-01-15T10:30:00.000Z",
        "isCredit": true,
        "isDebit": false
      },
      {
        "id": "uuid",
        "type": "debit",
        "amount": 3000,
        "description": "Purchased: IPL Tickets",
        "ticketId": "uuid",
        "ticketTitle": "IPL 2025: RCB vs CSK",
        "createdAt": "2024-01-14T16:45:00.000Z",
        "isCredit": false,
        "isDebit": true
      }
    ],
    "count": 2
  }
}
```

---

### Chat

#### GET /chat/conversations

Get all conversations for the current user. **Requires authentication.**

**Response:**
```json
{
  "success": true,
  "data": {
    "conversations": [
      {
        "id": "user1_user2",
        "otherUserId": "uuid",
        "otherUserName": "John Doe",
        "lastMessage": "Is the ticket still available?",
        "lastMessageAt": "2024-01-15T10:30:00.000Z",
        "createdAt": "2024-01-10T14:20:00.000Z"
      }
    ]
  }
}
```

---

#### GET /chat/messages/:chatId

Get messages for a specific conversation. **Requires authentication.**

**Query Parameters:**
- `limit` (number, optional): Maximum messages to return (default: 50, max: 100)
- `offset` (number, optional): Offset for pagination (default: 0)

**Response:**
```json
{
  "success": true,
  "data": {
    "messages": [
      {
        "id": "uuid",
        "senderId": "uuid",
        "senderName": "John Doe",
        "receiverId": "uuid",
        "message": "Is the ticket still available?",
        "createdAt": "2024-01-15T10:30:00.000Z",
        "isSentByMe": false
      },
      {
        "id": "uuid",
        "senderId": "uuid",
        "senderName": "Jane Doe",
        "receiverId": "uuid",
        "message": "Yes, it is!",
        "createdAt": "2024-01-15T10:31:00.000Z",
        "isSentByMe": true
      }
    ],
    "count": 2
  }
}
```

---

#### POST /chat/send

Send a message to another user. **Requires authentication.**

**Request:**
```json
{
  "receiverId": "uuid",
  "message": "Is the ticket still available?"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": {
      "id": "uuid",
      "conversationId": "user1_user2",
      "senderId": "uuid",
      "receiverId": "uuid",
      "receiverName": "John Doe",
      "message": "Is the ticket still available?",
      "createdAt": "2024-01-15T10:30:00.000Z",
      "isSentByMe": true
    }
  }
}
```

---

### Upload

#### POST /upload/image

Upload an image to Cloudinary. **Requires authentication.**

**Request:**
- Content-Type: `multipart/form-data`
- Body: `image` (file field)

**Response:**
```json
{
  "success": true,
  "data": {
    "url": "https://res.cloudinary.com/.../image.jpg",
    "filename": "image.jpg"
  }
}
```

**Constraints:**
- Maximum file size: 10MB
- Only image files allowed

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `MISSING_FIELDS` | 400 | Required fields are missing |
| `INVALID_EMAIL` | 400 | Email format is invalid |
| `WEAK_PASSWORD` | 400 | Password is too short |
| `INVALID_PRICE` | 400 | Price must be greater than 0 |
| `NO_FILE` | 400 | No file was uploaded |
| `FILE_TOO_LARGE` | 400 | File exceeds size limit |
| `INVALID_FILE_TYPE` | 400 | File is not an image |
| `UNAUTHORIZED` | 401 | No authentication token provided |
| `INVALID_TOKEN` | 401 | Token is invalid or expired |
| `TICKET_NOT_FOUND` | 404 | Ticket does not exist |
| `USER_NOT_FOUND` | 404 | User does not exist |
| `NOT_FOUND` | 404 | Endpoint does not exist |
| `EMAIL_EXISTS` | 409 | Email already registered |
| `TICKET_NOT_AVAILABLE` | 400 | Ticket already sold |
| `CANNOT_BUY_OWN` | 400 | Cannot purchase own ticket |
| `INSUFFICIENT_BALANCE` | 400 | Not enough funds in wallet |
| `SERVER_ERROR` | 500 | Internal server error |
| `INTERNAL_ERROR` | 500 | Unhandled server error |

---

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    profile_image_url TEXT,
    wallet_balance BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Tickets Table
```sql
CREATE TABLE tickets (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price INT NOT NULL,
    event_date VARCHAR(100),
    image_url TEXT,
    seller_id VARCHAR(36) NOT NULL,
    status ENUM('available', 'sold') DEFAULT 'available',
    buyer_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_seller (seller_id),
    INDEX idx_buyer (buyer_id)
);
```

### Transactions Table
```sql
CREATE TABLE transactions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type ENUM('credit', 'debit') NOT NULL,
    amount BIGINT NOT NULL,
    description TEXT,
    ticket_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    INDEX idx_user_created (user_id, created_at)
);
```

### Conversations Table
```sql
CREATE TABLE conversations (
    id VARCHAR(73) PRIMARY KEY,
    user1_id VARCHAR(36) NOT NULL,
    user2_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user1_id) REFERENCES users(id),
    FOREIGN KEY (user2_id) REFERENCES users(id)
);
```

### Messages Table
```sql
CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(73) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    receiver_id VARCHAR(36) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id),
    INDEX idx_conversation (conversation_id, created_at)
);
```

---

## Local Development

1. Install dependencies:
```bash
cd backend
npm install
```

2. Create `.env` file from template:
```bash
cp .env.template .env
# Edit .env with your credentials
```

3. Run database migrations (see schema above)

4. Start the server:
```bash
npm start
```

Server will run on `http://localhost:3000`

---

## Deployment to Vercel

1. Install Vercel CLI:
```bash
npm i -g vercel
```

2. Login to Vercel:
```bash
vercel login
```

3. Deploy:
```bash
vercel --prod
```

4. Set environment variables in Vercel dashboard:
- DATABASE_URL
- JWT_SECRET
- CLOUDINARY_CLOUD_NAME
- CLOUDINARY_API_KEY
- CLOUDINARY_API_SECRET
- CLOUDINARY_FOLDER
