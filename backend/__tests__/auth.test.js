const request = require('supertest');
const app = require('../api/index');

describe('Auth API', () => {
    describe('POST /api/auth/login', () => {
        it('should login with valid credentials', async () => {
            const res = await request(app)
                .post('/api/auth/login')
                .send({ email: 'test@example.com', password: 'test123' });

            expect(res.status).toBe(200);
            expect(res.body.success).toBe(true);
            expect(res.body.data).toHaveProperty('token');
            expect(res.body.data).toHaveProperty('userId');
        });

        it('should reject invalid credentials', async () => {
            const res = await request(app)
                .post('/api/auth/login')
                .send({ email: 'test@example.com', password: 'wrongpassword' });

            expect(res.status).toBe(401);
        });

        it('should reject missing fields', async () => {
            const res = await request(app)
                .post('/api/auth/login')
                .send({});

            expect(res.status).toBe(400);
        });
    });

    describe('POST /api/auth/signup', () => {
        it('should reject duplicate email', async () => {
            const res = await request(app)
                .post('/api/auth/signup')
                .send({ email: 'test@example.com', password: 'newpass123', name: 'Duplicate' });

            expect(res.status).toBe(409);
        });

        it('should reject missing fields', async () => {
            const res = await request(app)
                .post('/api/auth/signup')
                .send({ email: 'new@example.com' });

            expect(res.status).toBe(400);
        });
    });
});