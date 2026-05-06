const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');
const { v4: uuidv4 } = require('uuid');

const router = express.Router();

/**
 * POST /api/tickets/:id/buy
 * Buy a ticket (deducts from buyer wallet, credits seller)
 */
router.post('/:id/buy', authMiddleware, async (req, res) => {
    const connection = await pool.getConnection();

    try {
        const { id } = req.params;
        const buyerId = req.user.userId;

        await connection.beginTransaction();

        // Get ticket details
        const [tickets] = await connection.execute(
            'SELECT * FROM tickets WHERE id = ? FOR UPDATE',
            [id]
        );

        if (tickets.length === 0) {
            await connection.rollback();
            return res.status(404).json({
                success: false,
                error: {
                    code: 'TICKET_NOT_FOUND',
                    message: 'Ticket not found'
                }
            });
        }

        const ticket = tickets[0];

        // Check if ticket is available
        if (ticket.status !== 'available') {
            await connection.rollback();
            return res.status(400).json({
                success: false,
                error: {
                    code: 'TICKET_NOT_AVAILABLE',
                    message: 'This ticket is no longer available'
                }
            });
        }

        // Can't buy your own ticket
        if (ticket.seller_id === buyerId) {
            await connection.rollback();
            return res.status(400).json({
                success: false,
                error: {
                    code: 'CANNOT_BUY_OWN',
                    message: 'You cannot buy your own ticket'
                }
            });
        }

        // Get buyer's wallet balance
        const [buyers] = await connection.execute(
            'SELECT wallet_balance FROM users WHERE id = ? FOR UPDATE',
            [buyerId]
        );

        const buyerBalance = parseInt(buyers[0].wallet_balance) || 0;

        // Check if buyer has enough balance
        if (buyerBalance < ticket.price) {
            await connection.rollback();
            return res.status(400).json({
                success: false,
                error: {
                    code: 'INSUFFICIENT_BALANCE',
                    message: 'Insufficient wallet balance'
                }
            });
        }

        // Deduct from buyer
        await connection.execute(
            'UPDATE users SET wallet_balance = wallet_balance - ? WHERE id = ?',
            [ticket.price, buyerId]
        );

        // Credit seller
        await connection.execute(
            'UPDATE users SET wallet_balance = wallet_balance + ? WHERE id = ?',
            [ticket.price, ticket.seller_id]
        );

        // Mark ticket as sold
        await connection.execute(
            'UPDATE tickets SET status = ?, buyer_id = ? WHERE id = ?',
            ['sold', buyerId, id]
        );

        // Create transaction records
        const buyerTxnId = uuidv4();
        const sellerTxnId = uuidv4();

        // Debit transaction for buyer
        await connection.execute(
            `INSERT INTO transactions (id, user_id, type, amount, description, ticket_id)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [
                buyerTxnId,
                buyerId,
                'debit',
                ticket.price,
                `Purchased: ${ticket.title}`,
                id
            ]
        );

        // Credit transaction for seller
        await connection.execute(
            `INSERT INTO transactions (id, user_id, type, amount, description, ticket_id)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [
                sellerTxnId,
                ticket.seller_id,
                'credit',
                ticket.price,
                `Sold: ${ticket.title}`,
                id
            ]
        );

        await connection.commit();

        res.json({
            success: true,
            data: {
                message: 'Ticket purchased successfully',
                ticketId: id,
                price: ticket.price,
                newBalance: buyerBalance - ticket.price
            }
        });

    } catch (error) {
        await connection.rollback();
        console.error('Buy ticket error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while purchasing the ticket'
            }
        });
    } finally {
        connection.release();
    }
});

module.exports = router;
