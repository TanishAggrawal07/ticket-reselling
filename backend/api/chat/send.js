const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');
const { v4: uuidv4 } = require('uuid');

const router = express.Router();

// Helper to build chat ID from two user IDs
function buildChatId(userId1, userId2) {
    return userId1 < userId2 ? `${userId1}_${userId2}` : `${userId2}_${userId1}`;
}

/**
 * POST /api/chat/send
 * Send a message to another user
 */
router.post('/', authMiddleware, async (req, res) => {
    try {
        const senderId = req.user.userId;
        const { receiverId, message } = req.body;

        // Validate input
        if (!receiverId || !message || message.trim() === '') {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'MISSING_FIELDS',
                    message: 'Receiver ID and message are required'
                }
            });
        }

        if (receiverId === senderId) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'SELF_MESSAGE',
                    message: 'Cannot send message to yourself'
                }
            });
        }

        // Verify receiver exists
        const [receivers] = await pool.execute(
            'SELECT id, name FROM users WHERE id = ?',
            [receiverId]
        );

        if (receivers.length === 0) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'USER_NOT_FOUND',
                    message: 'Receiver not found'
                }
            });
        }

        const receiverName = receivers[0].name;

        // Build chat ID
        const chatId = buildChatId(senderId, receiverId);

        // Ensure conversation exists
        const [existingConv] = await pool.execute(
            'SELECT id FROM conversations WHERE id = ?',
            [chatId]
        );

        if (existingConv.length === 0) {
            // Create conversation
            await pool.execute(
                `INSERT INTO conversations (id, user1_id, user2_id)
                 VALUES (?, ?, ?)`,
                [
                    chatId,
                    senderId < receiverId ? senderId : receiverId,
                    senderId < receiverId ? receiverId : senderId
                ]
            );
        }

        // Insert message
        const messageId = uuidv4();
        await pool.execute(
            `INSERT INTO messages (id, conversation_id, sender_id, receiver_id, message)
             VALUES (?, ?, ?, ?, ?)`,
            [messageId, chatId, senderId, receiverId, message.trim()]
        );

        res.status(201).json({
            success: true,
            data: {
                message: {
                    id: messageId,
                    conversationId: chatId,
                    senderId: senderId,
                    receiverId: receiverId,
                    receiverName: receiverName,
                    message: message.trim(),
                    createdAt: new Date().toISOString(),
                    isSentByMe: true
                }
            }
        });

    } catch (error) {
        console.error('Send message error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while sending the message'
            }
        });
    }
});

module.exports = router;
