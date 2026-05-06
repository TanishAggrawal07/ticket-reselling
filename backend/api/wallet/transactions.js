const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * GET /api/wallet/transactions
 * Get current user's transaction history
 */
router.get('/', authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;
        const limit = Math.min(parseInt(req.query.limit) || 50, 100);
        const offset = parseInt(req.query.offset) || 0;

        const [transactions] = await pool.execute(
            `SELECT t.*, tk.title as ticket_title
             FROM transactions t
             LEFT JOIN tickets tk ON t.ticket_id = tk.id
             WHERE t.user_id = ?
             ORDER BY t.created_at DESC
             LIMIT ${parseInt(limit)} OFFSET ${parseInt(offset)}`,
            [userId]
        );

        const formattedTransactions = transactions.map(txn => ({
            id: txn.id,
            type: txn.type,
            amount: txn.amount,
            description: txn.description,
            ticketId: txn.ticket_id,
            ticketTitle: txn.ticket_title,
            createdAt: txn.created_at,
            isCredit: txn.type === 'credit',
            isDebit: txn.type === 'debit'
        }));

        res.json({
            success: true,
            data: {
                transactions: formattedTransactions,
                count: formattedTransactions.length
            }
        });

    } catch (error) {
        console.error('Fetch transactions error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching transactions'
            }
        });
    }
});

module.exports = router;
