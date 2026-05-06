const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * GET /api/tickets/my-listings
 * Get current user's ticket listings
 */
router.get('/', authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;
        const limit = Math.min(parseInt(req.query.limit) || 50, 100);
        const offset = parseInt(req.query.offset) || 0;

        const [tickets] = await pool.execute(
            `SELECT t.*, u.name as seller_name
             FROM tickets t
             JOIN users u ON t.seller_id = u.id
             WHERE t.seller_id = ?
             ORDER BY t.created_at DESC
             LIMIT ${parseInt(limit)} OFFSET ${parseInt(offset)}`,
            [userId]
        );

        const formattedTickets = tickets.map(ticket => ({
            id: ticket.id,
            title: ticket.title,
            description: ticket.description,
            originalPrice: ticket.original_price || ticket.price,
            price: ticket.price,
            eventDate: ticket.event_date,
            imageUrl: ticket.image_url || '',
            ticketFileUrl: ticket.ticket_file_url || '',
            sellerId: ticket.seller_id,
            sellerName: ticket.seller_name,
            status: ticket.status,
            buyerId: ticket.buyer_id,
            createdAt: ticket.created_at,
            updatedAt: ticket.updated_at
        }));

        res.json({
            success: true,
            data: {
                tickets: formattedTickets,
                count: formattedTickets.length
            }
        });

    } catch (error) {
        console.error('Fetch my listings error:', error.message);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching your listings: ' + error.message
            }
        });
    }
});

module.exports = router;
