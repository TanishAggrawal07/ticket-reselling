/**
 * Add ₹10,000 to all users' wallet balances
 * Run with: node scripts/add-wallet-credit.js
 */

require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });
const mysql = require('mysql2/promise');
const { v4: uuidv4 } = require('uuid');
const path = require('path');

const CREDIT_AMOUNT = 10000;
const DESCRIPTION = 'Added funds to wallet';

async function addWalletCredit() {
    const connection = await mysql.createConnection(process.env.DATABASE_URL);

    try {
        // Get all users
        const [users] = await connection.execute('SELECT id, name, wallet_balance FROM users');

        if (users.length === 0) {
            console.log('No users found.');
            return;
        }

        console.log(`Adding ₹${CREDIT_AMOUNT} to ${users.length} user(s)...\n`);

        for (const user of users) {
            // Update wallet balance
            await connection.execute(
                'UPDATE users SET wallet_balance = wallet_balance + ? WHERE id = ?',
                [CREDIT_AMOUNT, user.id]
            );

            // Create transaction record
            await connection.execute(
                `INSERT INTO transactions (id, user_id, type, amount, description)
                 VALUES (?, ?, 'credit', ?, ?)`,
                [uuidv4(), user.id, CREDIT_AMOUNT, DESCRIPTION]
            );

            console.log(`✅ ${user.name} (${user.id}) — ₹${user.wallet_balance} → ₹${user.wallet_balance + CREDIT_AMOUNT}`);
        }

        console.log(`\n✨ Done! ₹${CREDIT_AMOUNT} credited to all ${users.length} account(s).`);
    } catch (error) {
        console.error('❌ Error:', error);
        process.exit(1);
    } finally {
        await connection.end();
    }
}

if (require.main === module) {
    addWalletCredit();
}

module.exports = { addWalletCredit };