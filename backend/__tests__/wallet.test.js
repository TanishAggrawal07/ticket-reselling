const request = require('supertest');
const app = require('../api/index');
const { getAuthToken, authRequest } = require('./setup');

describe('Wallet API', () => {
    let token;

    beforeAll(async () => {
        token = await getAuthToken();
    });

    describe('GET /api/wallet/balance', () => {
        it('should return balance and pendingBalance', async () => {
            const res = await authRequest(token)
                .get('/api/wallet/balance');

            expect(res.status).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data).toHaveProperty('balance');
            expect(res.body.data).toHaveProperty('pendingBalance');
            expect(typeof res.body.data.balance).toBe('number');
            expect(typeof res.body.data.pendingBalance).toBe('number');
        });

        it('should reject unauthenticated requests', async () => {
            const res = await request(app)
                .get('/api/wallet/balance');

            expect(res.status).toBe(401);
        });
    });

    describe('GET /api/wallet/transactions', () => {
        it('should return transaction list', async () => {
            const res = await authRequest(token)
                .get('/api/wallet/transactions');

            expect(res.status).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data).toHaveProperty('transactions');
            expect(Array.isArray(res.body.data.transactions)).toBe(true);
            expect(res.body.data).toHaveProperty('count');
        });

        it('should reject unauthenticated requests', async () => {
            const res = await request(app)
                .get('/api/wallet/transactions');

            expect(res.status).toBe(401);
        });
    });

    describe('GET /api/wallet/monthly-earnings', () => {
        it('should return monthly earnings data', async () => {
            const res = await authRequest(token)
                .get('/api/wallet/monthly-earnings');

            expect(res.status).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data).toHaveProperty('monthlyEarnings');
            expect(Array.isArray(res.body.data.monthlyEarnings)).toBe(true);
        });

        it('should return 6 months of data', async () => {
            const res = await authRequest(token)
                .get('/api/wallet/monthly-earnings');

            expect(res.body.data.monthlyEarnings.length).toBeLessThanOrEqual(6);
            // Each entry should have label and total
            if (res.body.data.monthlyEarnings.length > 0) {
                const entry = res.body.data.monthlyEarnings[0];
                expect(entry).toHaveProperty('label');
                expect(entry).toHaveProperty('total');
            }
        });

        it('should reject unauthenticated requests', async () => {
            const res = await request(app)
                .get('/api/wallet/monthly-earnings');

            expect(res.status).toBe(401);
        });
    });
});