const express = require('express');
const { pool } = require('../../../lib/db');
const { authMiddleware } = require('../../../lib/auth');

const router = express.Router();

/**
 * GET /api/chat/messages/:chatId
 * Get messages for a specific conversation
 */
router.get('/:chatId', authMiddleware, async (req, res) => {
    try {
        const { chatId } = req.params;
        const userId = req.user.userId;

        // Verify user is part of this conversation
        const [conversations] = await pool.execute(
            'SELECT * FROM conversations WHERE id = ? AND (user1_id = ? OR user2_id = ?)',
            [chatId, userId, userId]
        );

        if (conversations.length === 0) {
            return res.status(403).json({
                success: false,
                error: {
                    code: 'ACCESS_DENIED',
                    message: 'You are not a participant in this conversation'
                }
            });
        }

        const limit = Math.min(parseInt(req.query.limit) || 50, 100);
        const offset = parseInt(req.query.offset) || 0;

        const [messages] = await pool.execute(
            `SELECT m.*, u.name as sender_name
             FROM messages m
             JOIN users u ON m.sender_id = u.id
             WHERE m.conversation_id = ?
             ORDER BY m.created_at ASC
             LIMIT ${parseInt(limit)} OFFSET ${parseInt(offset)}`,
            [chatId]
        );

        const formattedMessages = messages.map(msg => ({
            id: msg.id,
            senderId: msg.sender_id,
            senderName: msg.sender_name,
            receiverId: msg.receiver_id,
            message: msg.message,
            createdAt: msg.created_at,
            isSentByMe: msg.sender_id === userId
        }));

        res.json({
            success: true,
            data: {
                messages: formattedMessages,
                count: formattedMessages.length
            }
        });

    } catch (error) {
        console.error('Fetch messages error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching messages'
            }
        });
    }
});

module.exports = router;
