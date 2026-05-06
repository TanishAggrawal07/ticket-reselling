const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * GET /api/wallet/monthly-earnings
 * Get monthly credit totals for the last 6 months for the authenticated user.
 * Used by the sales trend chart.
 */
router.get('/', authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;

        // Get last 6 months of credit totals
        const [rows] = await pool.execute(
            `SELECT DATE_FORMAT(created_at, '%b') as label,
                    COALESCE(SUM(amount), 0) as total
             FROM transactions
             WHERE user_id = ? AND type = 'credit'
               AND created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
             GROUP BY YEAR(created_at), MONTH(created_at)
             ORDER BY MIN(created_at) ASC`,
            [userId]
        );

        // Fill in missing months with zero
        const monthLabels = [];
        const now = new Date();
        for (let i = 5; i >= 0; i--) {
            const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
            monthLabels.push(d.toLocaleString('en', { month: 'short' }));
        }

        const monthlyEarnings = monthLabels.map(label => {
            const match = rows.find(r => r.label === label);
            return {
                label: label,
                total: match ? parseInt(match.total) : 0
            };
        });

        res.json({
            success: true,
            data: {
                monthlyEarnings: monthlyEarnings
            }
        });

    } catch (error) {
        console.error('Monthly earnings error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching monthly earnings'
            }
        });
    }
});

module.exports = router;