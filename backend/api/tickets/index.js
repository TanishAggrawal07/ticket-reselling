const express = require('express');
const { pool } = require('../../lib/db');
const { optionalAuthMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * GET /api/tickets
 * List available tickets (public endpoint)
 * Query params: limit (default 50), offset (default 0)
 */
router.get('/', optionalAuthMiddleware, async (req, res) => {
    try {
        const limit = Math.min(parseInt(req.query.limit) || 50, 100);
        const offset = parseInt(req.query.offset) || 0;

        const [tickets] = await pool.execute(
            `SELECT t.*, u.name as seller_name
             FROM tickets t
             JOIN users u ON t.seller_id = u.id
             WHERE t.status = ?
             ORDER BY t.created_at DESC
             LIMIT ${parseInt(limit)} OFFSET ${parseInt(offset)}`,
            ['available']
        );

        // Format tickets
        const formattedTickets = tickets.map(ticket => ({
            id: ticket.id,
            title: ticket.title,
            description: ticket.description,
            originalPrice: ticket.original_price || ticket.price,
            price: ticket.price,
            eventDate: ticket.event_date,
            imageUrl: ticket.image_url || '',
            sellerId: ticket.seller_id,
            sellerName: ticket.seller_name,
            status: ticket.status,
            createdAt: ticket.created_at,
            isOwner: req.user ? ticket.seller_id === req.user.userId : false
        }));

        res.json({
            success: true,
            data: {
                tickets: formattedTickets,
                count: formattedTickets.length
            }
        });

    } catch (error) {
        console.error('Fetch tickets error:', error.message);
        console.error('Error stack:', error.stack);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching tickets: ' + error.message
            }
        });
    }
});

module.exports = router;
