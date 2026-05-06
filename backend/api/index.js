require('dotenv').config();

const express = require('express');
const cors = require('cors');

// Import route handlers
const authSignup = require('./auth/signup');
const authLogin = require('./auth/login');
const authRefresh = require('./auth/refresh');
const usersProfile = require('./users/profile');
const usersStats = require('./users/stats');
const ticketsList = require('./tickets/index');
const ticketsCreate = require('./tickets/create');
const ticketsDetail = require('./tickets/[id]');
const ticketsMyListings = require('./tickets/my-listings');
const ticketsMyPurchases = require('./tickets/my-purchases');
const ticketsBuy = require('./tickets/buy/[id]');
const ticketsConfirmEntry = require('./tickets/confirm-entry');
const walletBalance = require('./wallet/balance');
const walletTransactions = require('./wallet/transactions');
const walletMonthlyEarnings = require('./wallet/monthly-earnings');
const chatConversations = require('./chat/conversations');
const chatMessages = require('./chat/messages/[id]');
const chatSend = require('./chat/send');
const uploadImage = require('./upload/image');

const app = express();

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Health check endpoint
app.get('/', (req, res) => {
    res.json({
        success: true,
        message: 'ReTix API is running',
        version: '1.0.0',
        timestamp: new Date().toISOString()
    });
});

// Mount routes under /api prefix
app.use('/api/auth/signup', authSignup);
app.use('/api/auth/login', authLogin);
app.use('/api/auth/refresh', authRefresh);
app.use('/api/users/profile', usersProfile);
app.use('/api/users/stats', usersStats);
app.use('/api/tickets/my-listings', ticketsMyListings);
app.use('/api/tickets/my-purchases', ticketsMyPurchases);
app.use('/api/tickets/buy', ticketsBuy);
app.use('/api/tickets/confirm-entry', ticketsConfirmEntry);
app.use('/api/tickets/create', ticketsCreate);
app.use('/api/tickets', ticketsDetail); // Dynamic [id] route - must be after specific routes
app.use('/api/tickets', ticketsList);     // Root tickets route - must be last
app.use('/api/wallet/balance', walletBalance);
app.use('/api/wallet/transactions', walletTransactions);
app.use('/api/wallet/monthly-earnings', walletMonthlyEarnings);
app.use('/api/chat/conversations', chatConversations);
app.use('/api/chat/messages', chatMessages);
app.use('/api/chat/send', chatSend);
app.use('/api/upload/image', uploadImage);

// 404 handler
app.use((req, res) => {
    res.status(404).json({
        success: false,
        error: {
            code: 'NOT_FOUND',
            message: 'Endpoint not found'
        }
    });
});

// Error handler
app.use((err, req, res, next) => {
    console.error('Unhandled error:', err);
    res.status(500).json({
        success: false,
        error: {
            code: 'INTERNAL_ERROR',
            message: 'An internal server error occurred'
        }
    });
});

// Start server if running locally (not on Vercel)
if (process.env.NODE_ENV !== 'production') {
    const PORT = process.env.PORT || 3000;
    app.listen(PORT, () => {
        console.log(`Server running on port ${PORT}`);
    });
}

// Export for Vercel serverless functions
module.exports = app;
