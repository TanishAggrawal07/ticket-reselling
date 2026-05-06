const express = require('express');
const { signToken, verifyToken } = require('../../lib/jwt');

const router = express.Router();

/**
 * POST /api/auth/refresh
 * Refresh JWT token before it expires
 */
router.post('/', async (req, res) => {
    try {
        const { token } = req.body;

        if (!token) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'MISSING_TOKEN',
                    message: 'Token is required'
                }
            });
        }

        // Verify current token (even if expired, we'll refresh it)
        const decoded = verifyToken(token);

        if (!decoded) {
            return res.status(401).json({
                success: false,
                error: {
                    code: 'INVALID_TOKEN',
                    message: 'Cannot refresh invalid token'
                }
            });
        }

        // Generate new token
        const newToken = signToken({
            userId: decoded.userId,
            email: decoded.email,
            name: decoded.name
        });

        res.json({
            success: true,
            data: {
                token: newToken
            }
        });

    } catch (error) {
        console.error('Token refresh error:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'An error occurred during token refresh'
            }
        });
    }
});

module.exports = router;
