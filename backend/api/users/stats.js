const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * GET /api/users/stats
 * Get user's ticket statistics (active listings, sold count, total earnings)
 */
router.get('/', authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;

        // Get active listings count
        const [activeResult] = await pool.execute(
            'SELECT COUNT(*) as count FROM tickets WHERE seller_id = ? AND status = ?',
            [userId, 'available']
        );

        // Get sold tickets count
        const [soldResult] = await pool.execute(
            'SELECT COUNT(*) as count FROM tickets WHERE seller_id = ? AND status = ?',
            [userId, 'sold']
        );

        // Get total earnings from sales
        const [earningsResult] = await pool.execute(
            'SELECT COALESCE(SUM(price), 0) as total FROM tickets WHERE seller_id = ? AND status = ?',
            [userId, 'sold']
        );

        const activeListings = parseInt(activeResult[0].count) || 0;
        const ticketsSold = parseInt(soldResult[0].count) || 0;
        const totalEarnings = parseInt(earningsResult[0].total) || 0;

        res.json({
            success: true,
            data: {
                activeListings,
                ticketsSold,
                totalEarnings
            }
        });

    } catch (error) {
        console.error('Get stats error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching stats'
            }
        });
    }
});

module.exports = router;
