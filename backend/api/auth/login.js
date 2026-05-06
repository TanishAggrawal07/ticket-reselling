const express = require('express');
const bcrypt = require('bcryptjs');
const { pool } = require('../../lib/db');
const { signToken } = require('../../lib/jwt');

const router = express.Router();

/**
 * POST /api/auth/login
 * Authenticate user and return JWT
 */
router.post('/', async (req, res) => {
    try {
        const { email, password } = req.body;

        // Validate input
        if (!email || !password) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'MISSING_FIELDS',
                    message: 'Email and password are required'
                }
            });
        }

        // Find user by email
        const [users] = await pool.execute(
            'SELECT id, email, password_hash, name, profile_image_url, wallet_balance FROM users WHERE email = ?',
            [email.toLowerCase()]
        );

        if (users.length === 0) {
            return res.status(401).json({
                success: false,
                error: {
                    code: 'INVALID_CREDENTIALS',
                    message: 'Invalid email or password'
                }
            });
        }

        const user = users[0];

        // Verify password
        const isPasswordValid = await bcrypt.compare(password, user.password_hash);

        if (!isPasswordValid) {
            return res.status(401).json({
                success: false,
                error: {
                    code: 'INVALID_CREDENTIALS',
                    message: 'Invalid email or password'
                }
            });
        }

        // Generate JWT
        const token = signToken({
            userId: user.id,
            email: user.email,
            name: user.name
        });

        // Return success with token
        res.json({
            success: true,
            data: {
                token: token,
                user: {
                    id: user.id,
                    email: user.email,
                    name: user.name,
                    walletBalance: parseInt(user.wallet_balance) || 0,
                    profileImageUrl: user.profile_image_url || ''
                }
            }
        });

    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred during login'
            }
        });
    }
});

module.exports = router;
