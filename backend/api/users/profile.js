const express = require('express');
const { pool } = require('../../lib/db');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

/**
 * GET /api/users/profile
 * Get current user profile
 */
router.get('/', authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;

        const [users] = await pool.execute(
            'SELECT id, email, name, profile_image_url, wallet_balance, created_at FROM users WHERE id = ?',
            [userId]
        );

        if (users.length === 0) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'USER_NOT_FOUND',
                    message: 'User not found'
                }
            });
        }

        const user = users[0];

        res.json({
            success: true,
            data: {
                id: user.id,
                email: user.email,
                name: user.name,
                profileImageUrl: user.profile_image_url || '',
                walletBalance: parseInt(user.wallet_balance) || 0,
                createdAt: user.created_at
            }
        });

    } catch (error) {
        console.error('Get profile error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while fetching profile'
            }
        });
    }
});

/**
 * PUT /api/users/profile
 * Update user profile
 */
router.put('/', authMiddleware, async (req, res) => {
    try {
        const userId = req.user.userId;
        const { name, profileImageUrl } = req.body;

        // Build update query dynamically
        const updates = [];
        const values = [];

        if (name !== undefined) {
            updates.push('name = ?');
            values.push(name);
        }

        if (profileImageUrl !== undefined) {
            updates.push('profile_image_url = ?');
            values.push(profileImageUrl);
        }

        if (updates.length === 0) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'NO_UPDATES',
                    message: 'No fields to update'
                }
            });
        }

        // Add userId to values
        values.push(userId);

        await pool.execute(
            `UPDATE users SET ${updates.join(', ')} WHERE id = ?`,
            values
        );

        // Fetch updated user
        const [users] = await pool.execute(
            'SELECT id, email, name, profile_image_url, wallet_balance, created_at FROM users WHERE id = ?',
            [userId]
        );

        const user = users[0];

        res.json({
            success: true,
            data: {
                id: user.id,
                email: user.email,
                name: user.name,
                profileImageUrl: user.profile_image_url || '',
                walletBalance: parseInt(user.wallet_balance) || 0,
                createdAt: user.created_at
            }
        });

    } catch (error) {
        console.error('Update profile error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred while updating profile'
            }
        });
    }
});

module.exports = router;
