const mysql = require('mysql2/promise');

// Parse DATABASE_URL for serverless connection
function getConnectionOptions() {
    const url = new URL(process.env.DATABASE_URL);
    return {
        host: url.hostname,
        port: url.port || 3306,
        user: url.username,
        password: url.password,
        database: url.pathname.replace(/^\//, ''),
        ssl: { rejectUnauthorized: false }
    };
}

// Create connection for each request (serverless-safe)
async function getConnection() {
    return await mysql.createConnection(getConnectionOptions());
}

// Simple pool wrapper that manages connections properly
const pool = {
    async execute(sql, params) {
        const conn = await getConnection();
        try {
            return await conn.execute(sql, params);
        } finally {
            await conn.end();
        }
    },
    async query(sql, params) {
        const conn = await getConnection();
        try {
            return await conn.query(sql, params);
        } finally {
            await conn.end();
        }
    },
    async getConnection() {
        return await getConnection();
    }
};

// Test connection helper
async function testConnection() {
    try {
        const conn = await getConnection();
        await conn.execute('SELECT 1');
        await conn.end();
        console.log('Database connected successfully');
        return true;
    } catch (error) {
        console.error('Database connection failed:', error.message);
        return false;
    }
}

module.exports = { pool, testConnection };
