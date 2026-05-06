/**
 * Test setup utilities for ReTix backend tests.
 * Provides helper functions for creating authenticated requests
 * and managing test data.
 */

const request = require('supertest');
const app = require('../api/index');

const TEST_USER = {
    email: 'test@example.com',
    password: 'test123'
};

/**
 * Get a valid JWT token for testing
 */
async function getAuthToken() {
    const res = await request(app)
        .post('/api/auth/login')
        .send({ email: TEST_USER.email, password: TEST_USER.password });

    if (res.status === 200 && res.body.success) {
        return res.body.data.token;
    }
    throw new Error(`Failed to get auth token: ${res.status} ${JSON.stringify(res.body)}`);
}

/**
 * Create an authenticated supertest request
 */
function authRequest(token) {
    return request(app).set('Authorization', `Bearer ${token}`);
}

module.exports = {
    app,
    getAuthToken,
    authRequest,
    TEST_USER
};