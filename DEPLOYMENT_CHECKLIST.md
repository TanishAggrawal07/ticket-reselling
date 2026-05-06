# ReTix Deployment Checklist

## Backend Status: ✅ Ready
**Deployed at:** `https://backend-three-phi-61.vercel.app/`

## Database Setup Required ⚙️

### Step 1: Create Database Tables
Run this SQL in your MySQL database:

```sql
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    profile_image_url TEXT,
    wallet_balance BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
);

-- Tickets table
CREATE TABLE IF NOT EXISTS tickets (
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
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_seller (seller_id),
    INDEX idx_buyer (buyer_id)
);

-- Transactions table (wallet history)
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type ENUM('credit', 'debit') NOT NULL,
    amount BIGINT NOT NULL,
    description TEXT,
    ticket_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE SET NULL,
    INDEX idx_user_created (user_id, created_at)
);

-- Chat conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(73) PRIMARY KEY,
    user1_id VARCHAR(36) NOT NULL,
    user2_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id)
);

-- Messages table
CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(73) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    receiver_id VARCHAR(36) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_conversation (conversation_id, created_at)
);
```

### Step 2: Set Vercel Environment Variables

Go to Vercel Dashboard → Your Project → Settings → Environment Variables

Add these variables:

| Variable | Required | Description |
|----------|----------|-------------|
| `DATABASE_URL` | ✅ | MySQL connection string: `mysql://user:pass@host:3306/retix` |
| `JWT_SECRET` | ✅ | Long random string (min 32 chars) |
| `JWT_EXPIRES_IN` | ✅ | Token expiry: `7d` |
| `CLOUDINARY_CLOUD_NAME` | ✅ | Your Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | ✅ | Your Cloudinary API key |
| `CLOUDINARY_API_SECRET` | ✅ | Your Cloudinary API secret |
| `CLOUDINARY_FOLDER` | ✅ | Folder for uploads: `retix_tickets` |

### Step 3: Seed Database (Optional)

After environment variables are set, seed with test data:

```bash
cd backend
npm install
npm run seed
```

Or insert a test user manually:
```sql
INSERT INTO users (id, email, password_hash, name, wallet_balance) VALUES
(UUID(), 'test@example.com', 
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'Test User', 50000);
-- Password is 'test123'
```

### Step 4: Redeploy Backend

After setting environment variables:
```bash
vercel --prod
```

## Android App Status: ✅ Ready

The Android app is configured with your API URL:
```java
// In ApiClient.java
private static final String DEFAULT_BASE_URL = "https://backend-three-phi-61.vercel.app/api/";
```

### Build Instructions:

1. Open project in Android Studio
2. Sync Gradle files (File → Sync Project with Gradle Files)
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. Run on emulator or device

### Test Accounts (after seeding):

| Email | Password | Wallet |
|-------|----------|--------|
| arjun@example.com | password123 | ₹15,000 |
| priya@example.com | password123 | ₹25,000 |
| rahul@example.com | password123 | ₹8,000 |
| test@example.com | test123 | ₹50,000 |

## API Verification

Test your API is working:

```bash
# Health check
curl https://backend-three-phi-61.vercel.app/

# Expected response:
# {"success":true,"message":"ReTix API is running","version":"1.0.0",...}
```

Or run the test script:
```bash
cd backend
node scripts/test-api.js
```

## File Structure Summary

```
backend/
├── api/                    # API endpoints
│   ├── auth/              # Login, signup, refresh
│   ├── users/             # Profile, stats
│   ├── tickets/           # Tickets CRUD + buy
│   ├── wallet/            # Balance, transactions
│   ├── chat/              # Conversations, messages
│   ├── upload/            # Image upload
│   └── index.js           # Main entry point
├── lib/                   # Shared utilities
│   ├── db.js             # MySQL connection
│   ├── jwt.js            # JWT handling
│   ├── auth.js           # Auth middleware
│   └── cloudinary.js     # Cloudinary config
├── scripts/              # Helper scripts
│   ├── schema.sql        # Database schema
│   ├── seed-database.js  # Seed data
│   └── test-api.js      # API tests
├── docs/
│   └── api.md            # Full API docs
└── vercel.json           # Vercel config

app/src/main/java/com/tanish/retix/
├── ApiClient.java         # Retrofit client
├── ApiService.java        # API interface
├── TokenManager.java      # JWT storage
├── ApiManager.java        # Data operations
└── ... (updated activities)
```

## Troubleshooting

### Database Connection Failed
- Check DATABASE_URL format
- Verify MySQL allows external connections
- Try adding `?sslmode=required` to DATABASE_URL

### API Returns 500
- Check Vercel function logs
- Verify all environment variables are set
- Redeploy after env var changes

### Android Can't Connect
- Check internet permission in AndroidManifest.xml
- Verify API URL is correct
- Check logcat for detailed errors

## Next Steps

1. ✅ Backend deployed to Vercel
2. ⬜ Set environment variables in Vercel
3. ⬜ Run database schema
4. ⬜ Seed database with test data
5. ⬜ Build and test Android app
6. ⬜ Deploy Android app to Play Store

## Support Files

- `SETUP_GUIDE.md` - Detailed setup instructions
- `MIGRATION_SUMMARY.md` - What changed from Firebase
- `backend/docs/api.md` - Complete API documentation
