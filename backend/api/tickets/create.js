const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * POST /api/tickets
 * Create a new ticket listing
 */
router.post('/', authMiddleware, async (req, res) => {
    try {
        const { title, description, originalPrice, price, eventDate, imageUrl, ticketFileUrl } = req.body;
        const sellerId = req.user.userId;
        const sellerName = req.user.name;

        // Validate required fields
        if (!title || !price) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'MISSING_FIELDS',
                    message: 'Title and price are required'
                }
            });
        }

        if (price <= 0) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'INVALID_PRICE',
                    message: 'Price must be greater than 0'
                }
            });
        }

        const ticketId = uuidv4();

        await pool.execute(
            `INSERT INTO tickets (id, title, description, original_price, price, event_date, image_url, ticket_file_url, seller_id, status)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [
                ticketId,
                title,
                description || '',
                originalPrice || price,
                price,
                eventDate || '',
                imageUrl || '',
                ticketFileUrl || '',
                sellerId,
                'available'
            ]
        );

        res.status(201).json({
            success: true,
            data: {
                ticket: {
                    id: ticketId,
                    title,
                    description: description || '',
                    originalPrice: originalPrice || price,
                    price,
                    eventDate: eventDate || '',
                    imageUrl: imageUrl || '',
                    ticketFileUrl: ticketFileUrl || '',
                    sellerId,
                    sellerName,
                    status: 'available',
                    createdAt: new Date().toISOString()
                }
            }
        });

    } catch (error) {
        console.error('Create ticket error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while creating the ticket'
            }
        });
    }
});

module.exports = router;
