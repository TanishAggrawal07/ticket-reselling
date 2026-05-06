const request = require('supertest');
const app = require('../api/index');
const { getAuthToken, authRequest } = require('./setup');

describe('Users API', () => {
    let token;

    beforeAll(async () => {
        token = await getAuthToken();
    });

    describe('GET /api/users/profile', () => {
        it('should return user profile', async () => {
            const res = await authRequest(token)
                .get('/api/users/profile');

            expect(res.status).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data).toHaveProperty('name');
            expect(res.body.data).toHaveProperty('email');
        });

        it('should reject unauthenticated requests', async () => {
            const res = await request(app)
                .get('/api/users/profile');

            expect(res.status).toBe(401);
        });
    });

    describe('GET /api/users/stats', () => {
        it('should return user stats', async () => {
            const res = await authRequest(token)
                .get('/api/users/stats');

            expect(res.status).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data).toHaveProperty('activeListings');
            expect(res.body.data).toHaveProperty('soldCount');
            expect(res.body.data).toHaveProperty('totalEarnings');
        });

        it('should reject unauthenticated requests', async () => {
            const res = await request(app)
                .get('/api/users/stats');

            expect(res.status).toBe(401);
        });
    });
});