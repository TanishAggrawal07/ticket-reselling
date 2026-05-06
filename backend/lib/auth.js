const { verifyToken, extractTokenFromHeader } = require('./jwt');

/**
 * Express middleware to verify JWT authentication
 * Adds req.user with decoded token payload if valid
 */
function authMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    const token = extractTokenFromHeader(authHeader);

    if (!token) {
        return res.status(401).json({
            success: false,
            error: {
                code: 'UNAUTHORIZED',
                message: 'Authentication required. Please provide a valid token.'
            }
        });
    }

    const decoded = verifyToken(token);
    if (!decoded) {
        return res.status(401).json({
            success: false,
            error: {
                code: 'INVALID_TOKEN',
                message: 'Invalid or expired token. Please login again.'
            }
        });
    }

    req.user = decoded;
    next();
}

/**
 * Optional auth middleware - doesn't reject if no token
 * Adds req.user if token is valid, otherwise req.user is null
 */
function optionalAuthMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    const token = extractTokenFromHeader(authHeader);

    if (token) {
        const decoded = verifyToken(token);
        if (decoded) {
            req.user = decoded;
        }
    }

    next();
}

module.exports = {
    authMiddleware,
    optionalAuthMiddleware
};
