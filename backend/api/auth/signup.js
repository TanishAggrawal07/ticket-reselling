const express = require('express');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const { pool } = require('../../lib/db');
const { signToken } = require('../../lib/jwt');

const router = express.Router();
const SALT_ROUNDS = 10;

/**
 * POST /api/auth/signup
 * Register a new user
 */
router.post('/', async (req, res) => {
    try {
        const { email, password, name } = req.body;

        // Validate input
        if (!email || !password || !name) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'MISSING_FIELDS',
                    message: 'Email, password, and name are required'
                }
            });
        }

        // Validate email format
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'INVALID_EMAIL',
                    message: 'Please provide a valid email address'
                }
            });
        }

        // Validate password length
        if (password.length < 6) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'WEAK_PASSWORD',
                    message: 'Password must be at least 6 characters long'
                }
            });
        }

        // Check if email already exists
        const [existingUsers] = await pool.execute(
            'SELECT id FROM users WHERE email = ?',
            [email.toLowerCase()]
        );

        if (existingUsers.length > 0) {
            return res.status(409).json({
                success: false,
                error: {
                    code: 'EMAIL_EXISTS',
                    message: 'An account with this email already exists'
                }
            });
        }

        // Hash password
        const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);

        // Generate user ID
        const userId = uuidv4();

        // Insert user
        await pool.execute(
            `INSERT INTO users (id, email, password_hash, name, wallet_balance)
             VALUES (?, ?, ?, ?, ?)`,
            [userId, email.toLowerCase(), passwordHash, name, 0]
        );

        // Generate JWT
        const token = signToken({
            userId: userId,
            email: email.toLowerCase(),
            name: name
        });

        // Return success with token
        res.status(201).json({
            success: true,
            data: {
                token: token,
                user: {
                    id: userId,
                    email: email.toLowerCase(),
                    name: name,
                    walletBalance: 0,
                    profileImageUrl: ''
                }
            }
        });

    } catch (error) {
        console.error('Signup error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred during registration'
            }
        });
    }
});

module.exports = router;
