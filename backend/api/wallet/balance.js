const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * GET /api/wallet/balance
 * Get current user's wallet balance and pending balance
 */
router.get('/', authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;

        const [users] = await pool.execute(
            'SELECT wallet_balance FROM users WHERE id = ?',
            [userId]
        );

        if (users.length === 0) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'USER_NOT_FOUND',
                    message: 'User not found'
                }
            });
        }

        const balance = parseInt(users[0].wallet_balance) || 0;

        // Compute pending balance from processing debit transactions
        const [pendingRows] = await pool.execute(
            'SELECT COALESCE(SUM(amount), 0) as pending FROM transactions WHERE user_id = ? AND type = ? AND status = ?',
            [userId, 'debit', 'processing']
        );

        const pendingBalance = parseInt(pendingRows[0].pending) || 0;

        res.json({
            success: true,
            data: {
                balance: balance,
                pendingBalance: pendingBalance
            }
        });

    } catch (error) {
        console.error('Get balance error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching balance'
            }
        });
    }
});

module.exports = router;