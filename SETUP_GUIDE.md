# ReTix Setup Guide

## Backend Deployment (Vercel)

Your API is deployed at: `https://backend-three-phi-61.vercel.app/`

### Step 1: Database Setup

1. Create a MySQL database (you can use PlanetScale, Railway, or any MySQL provider)

2. Run the schema SQL to create tables:
   ```sql
   -- Run this in your MySQL database
   -- Copy contents from backend/scripts/schema.sql
   ```

3. Set environment variables in Vercel Dashboard:
   - Go to your Vercel project settings
   - Add these environment variables:

   | Variable | Value | Example |
   |----------|-------|---------|
   | `DATABASE_URL` | MySQL connection string | `mysql://user:pass@host:3306/retix` |
   | `JWT_SECRET` | Random secret key | `your-super-secret-jwt-key-min-32-chars` |
   | `JWT_EXPIRES_IN` | Token expiry | `7d` |
   | `CLOUDINARY_CLOUD_NAME` | Your cloud name | `your-cloud` |
   | `CLOUDINARY_API_KEY` | API key | `1234567890` |
   | `CLOUDINARY_API_SECRET` | API secret | `secret-here` |
   | `CLOUDINARY_FOLDER` | Upload folder | `retix_tickets` |

4. Redeploy the backend after setting environment variables

### Step 2: Seed the Database

Option 1: Run locally (requires Node.js and DATABASE_URL in .env):
```bash
cd backend
npm install
npm run seed
```

Option 2: Run SQL directly in your database:
```sql
-- Insert test users (password is 'password123' hashed)
INSERT INTO users (id, email, password_hash, name, wallet_balance) VALUES
(UUID(), 'arjun@example.com', '$2a$10$YourHashedPassword', 'Arjun Sharma', 15000),
(UUID(), 'priya@example.com', '$2a$10$YourHashedPassword', 'Priya Patel', 25000),
(UUID(), 'rahul@example.com', '$2a$10$YourHashedPassword', 'Rahul Verma', 8000),
(UUID(), 'test@example.com', '$2a$10$YourHashedPassword', 'Test User', 50000);

-- Insert sample tickets
INSERT INTO tickets (id, title, description, price, event_date, seller_id, status) VALUES
(UUID(), 'Coldplay: Music of the Spheres', 'Front row seats', 5500, 'Sat, Jan 18 • 7:00 PM', (SELECT id FROM users WHERE email='arjun@example.com'), 'available');
```

### Step 3: Test the API

Test endpoints with curl:
```bash
# Health check
curl https://backend-three-phi-61.vercel.app/

# Signup
curl -X POST https://backend-three-phi-61.vercel.app/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","name":"Test User"}'

# Login
curl -X POST https://backend-three-phi-61.vercel.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# Get tickets (public)
curl https://backend-three-phi-61.vercel.app/api/tickets
```

## Android App Setup

### Step 1: Verify API URL

The API URL is already set in `ApiClient.java`:
```java
private static final String DEFAULT_BASE_URL = "https://backend-three-phi-61.vercel.app/api/";
```

### Step 2: Build and Run

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on emulator or device

### Step 3: Test Accounts

After seeding the database, use these test accounts:

| Email | Password | Name |
|-------|----------|------|
| `arjun@example.com` | `password123` | Arjun Sharma |
| `priya@example.com` | `password123` | Priya Patel |
| `rahul@example.com` | `password123` | Rahul Verma |
| `test@example.com` | `test123` | Test User |

## API Endpoints Reference

### Authentication
- `POST /api/auth/signup` - Register
- `POST /api/auth/login` - Login
- `POST /api/auth/refresh` - Refresh token

### Users
- `GET /api/users/profile` - Get profile (auth)
- `PUT /api/users/profile` - Update profile (auth)
- `GET /api/users/stats` - Get stats (auth)

### Tickets
- `GET /api/tickets` - List available tickets
- `GET /api/tickets/:id` - Get ticket details
- `POST /api/tickets/create` - Create ticket (auth)
- `GET /api/tickets/my-listings` - My listings (auth)
- `GET /api/tickets/my-purchases` - My purchases (auth)
- `POST /api/tickets/buy/:id` - Buy ticket (auth)

### Wallet
- `GET /api/wallet/balance` - Get balance (auth)
- `GET /api/wallet/transactions` - Get transactions (auth)

### Chat
- `GET /api/chat/conversations` - Get conversations (auth)
- `GET /api/chat/messages/:chatId` - Get messages (auth)
- `POST /api/chat/send` - Send message (auth)

### Upload
- `POST /api/upload/image` - Upload image (auth, multipart/form-data)

## Troubleshooting

### Database Connection Issues
1. Verify DATABASE_URL format: `mysql://user:password@host:port/database`
2. Check if MySQL allows external connections
3. Verify SSL settings (may need `?sslmode=required`)

### API Errors
1. Check Vercel function logs in Vercel Dashboard
2. Verify all environment variables are set
3. Redeploy after changing environment variables

### Android App Issues
1. Check internet permission in AndroidManifest.xml
2. Verify API URL is correct
3. Check logcat for error details
4. Ensure JWT token is being sent in Authorization header

## Security Notes

1. Change JWT_SECRET to a long random string in production
2. Use strong passwords for database
3. Enable SSL for database connections
4. Set up Cloudinary with proper upload presets
5. Consider adding rate limiting to the API
