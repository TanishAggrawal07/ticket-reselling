const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * POST /api/tickets/:id/confirm-entry
 * Buyer confirms entry after purchasing a ticket.
 * Updates the debit transaction status from 'processing' to 'completed'.
 */
router.post('/:id', authMiddleware, async (req, res) => {
    const connection = await pool.getConnection();

    try {
        const { id } = req.params;
        const userId = req.user.userId;

        await connection.beginTransaction();

        // Verify the ticket exists and was purchased by this user
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

        if (ticket.buyer_id !== userId) {
            await connection.rollback();
            return res.status(403).json({
                success: false,
                error: {
                    code: 'NOT_AUTHORIZED',
                    message: 'You did not purchase this ticket'
                }
            });
        }

        // Update the buyer's debit transaction from 'processing' to 'completed'
        const [result] = await connection.execute(
            `UPDATE transactions SET status = 'completed'
             WHERE user_id = ? AND ticket_id = ? AND type = 'debit' AND status = 'processing'`,
            [userId, id]
        );

        await connection.commit();

        if (result.affectedRows === 0) {
            // Already completed or no processing transaction found
            return res.json({
                success: true,
                data: {
                    message: 'Entry already confirmed',
                    ticketId: id
                }
            });
        }

        res.json({
            success: true,
            data: {
                message: 'Entry confirmed successfully',
                ticketId: id
            }
        });

    } catch (error) {
        await connection.rollback();
        console.error('Confirm entry error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while confirming entry'
            }
        });
    } finally {
        connection.release();
    }
});

module.exports = router;