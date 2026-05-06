const request = require('supertest');
const app = require('../api/index');
const { getAuthToken, authRequest } = require('./setup');

describe('Tickets API', () => {
    let token;

    beforeAll(async () => {
        token = await getAuthToken();
    });

    describe('GET /api/tickets', () => {
        it('should return available tickets', async () => {
            const res = await authRequest(token)
                .get('/api/tickets');

            expect(res.status).toBe(200);
            expect(res.body.success).toBe(true);
            expect(Array.isArray(res.body.data.tickets)).toBe(true);
        });
    });

    describe('GET /api/tickets/my-listings', () => {
        it('should return user listings', async () => {
            const res = await authRequest(token)
                .get('/api/tickets/my-listings');

            expect(res.status).toBe(200);
            expect(res.body.success).toBe(true);
            expect(Array.isArray(res.body.data.tickets)).toBe(true);
        });
    });

    describe('GET /api/tickets/my-purchases', () => {
        it('should return user purchases', async () => {
            const res = await authRequest(token)
                .get('/api/tickets/my-purchases');

            expect(res.status).toBe(200);
            expect(res.body.success).toBe(true);
            expect(Array.isArray(res.body.data.tickets)).toBe(true);
        });
    });

    describe('POST /api/tickets/buy/:id', () => {
        it('should reject unauthenticated requests', async () => {
            const res = await request(app)
                .post('/api/tickets/buy/fake-id');

            expect(res.status).toBe(401);
        });

        it('should return error for non-existent ticket', async () => {
            const res = await authRequest(token)
                .post('/api/tickets/buy/non-existent-id');

            expect(res.status).toBe(404);
        });
    });

    describe('POST /api/tickets/:id/confirm-entry', () => {
        it('should reject unauthenticated requests', async () => {
            const res = await request(app)
                .post('/api/tickets/fake-id/confirm-entry');

            expect(res.status).toBe(401);
        });

        it('should return error for non-existent ticket', async () => {
            const res = await authRequest(token)
                .post('/api/tickets/non-existent-id/confirm-entry');

            expect(res.status).toBe(404);
        });
    });
});