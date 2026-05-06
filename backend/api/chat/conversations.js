const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * GET /api/chat/conversations
 * Get all conversations for the current user
 */
router.get('/', authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;

        const [conversations] = await pool.execute(
            `SELECT c.*,
                    u1.name as user1_name,
                    u2.name as user2_name,
                    (SELECT message FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message,
                    (SELECT created_at FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message_at
             FROM conversations c
             JOIN users u1 ON c.user1_id = u1.id
             JOIN users u2 ON c.user2_id = u2.id
             WHERE c.user1_id = ? OR c.user2_id = ?
             ORDER BY last_message_at DESC`,
            [userId, userId]
        );

        const formattedConversations = conversations.map(conv => {
            const otherUser = conv.user1_id === userId ? conv.user2_name : conv.user1_name;
            const otherUserId = conv.user1_id === userId ? conv.user2_id : conv.user1_id;

            return {
                id: conv.id,
                otherUserId: otherUserId,
                otherUserName: otherUser,
                lastMessage: conv.last_message || '',
                lastMessageAt: conv.last_message_at,
                createdAt: conv.created_at
            };
        });

        res.json({
            success: true,
            data: {
                conversations: formattedConversations
            }
        });

    } catch (error) {
        console.error('Fetch conversations error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching conversations'
            }
        });
    }
});

module.exports = router;
