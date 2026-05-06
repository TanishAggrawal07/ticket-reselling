const express = require('express');
const { pool } = require('../../lib/db');
const { optionalAuthMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * GET /api/tickets/:id
 * Get ticket details by ID
 */
router.get('/:id', optionalAuthMiddleware, async (req, res) => {
    try {
        const { id } = req.params;

        const [tickets] = await pool.execute(
            `SELECT t.*, u.name as seller_name
             FROM tickets t
             JOIN users u ON t.seller_id = u.id
             WHERE t.id = ?`,
            [id]
        );

        if (tickets.length === 0) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'TICKET_NOT_FOUND',
                    message: 'Ticket not found'
                }
            });
        }

        const ticket = tickets[0];

        res.json({
            success: true,
            data: {
                ticket: {
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
                    buyerId: ticket.buyer_id,
                    createdAt: ticket.created_at,
                    updatedAt: ticket.updated_at,
                    isOwner: req.user ? ticket.seller_id === req.user.userId : false
                }
            }
        });

    } catch (error) {
        console.error('Get ticket error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching the ticket'
            }
        });
    }
});

module.exports = router;
