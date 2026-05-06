/**
 * Database Seeder for ReTix
 * Run with: node scripts/seed-database.js
 *
 * This script creates:
 * - 3 sample users with wallet balances
 * - 9 sample tickets (some available, some sold)
 * - Sample transactions for wallet history
 * - Sample conversations and messages
 */

require('dotenv').config();
const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');

const SALT_ROUNDS = 10;

// Sample data
const users = [
    {
        id: uuidv4(),
        name: 'Arjun Sharma',
        email: 'arjun@example.com',
        password: 'password123',
        profileImageUrl: '',
        walletBalance: 25000
    },
    {
        id: uuidv4(),
        name: 'Priya Patel',
        email: 'priya@example.com',
        password: 'password123',
        profileImageUrl: '',
        walletBalance: 35000
    },
    {
        id: uuidv4(),
        name: 'Rahul Verma',
        email: 'rahul@example.com',
        password: 'password123',
        profileImageUrl: '',
        walletBalance: 18000
    },
    {
        id: uuidv4(),
        name: 'Test User',
        email: 'test@example.com',
        password: 'test123',
        profileImageUrl: '',
        walletBalance: 60000
    }
];

const tickets = [
 * This script creates:
 * - 4 sample users with wallet balances (₹10,000 base credit each)
 * - 9 sample tickets (some available, some sold)
        eventDate: 'Sat, Jan 18 • 7:00 PM',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg',
        status: 'available'
    },
    {
        title: 'Ed Sheeran: +-=÷x Tour',
        description: 'VIP tickets for Ed Sheeran live concert',
        price: 7200,
        eventDate: 'Sun, Feb 2 • 6:30 PM',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg',
        status: 'available'
    },
    {
        title: 'Arijit Singh Live Concert',
        description: 'Experience the magic of Arijit Singh live',
        price: 2800,
        eventDate: 'Fri, Jan 24 • 8:00 PM',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg',
        status: 'available'
    },
    {
        title: 'IPL 2025: RCB vs CSK',
        description: 'Premium stand tickets for the biggest cricket rivalry',
        price: 2500,
        eventDate: 'Sat, Mar 15 • 3:30 PM',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg',
        status: 'available'
    },
    {
        title: 'Sunburn Goa 2025',
        description: '3-day pass for Asia\'s biggest music festival',
        price: 3200,
        eventDate: 'Dec 27-29 • 4:00 PM',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg',
        status: 'sold',
        buyerIndex: 1 // priya bought from arjun
    },
    {
        title: 'Zakir Khan: Haq Se Single',
        description: 'Comedy show by the famous stand-up comedian',
        price: 1200,
        eventDate: 'Sat, Feb 8 • 7:30 PM',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg',
        status: 'sold',
        buyerIndex: 2 // rahul bought from priya
    },
    {
        title: 'Hamlet – Shakespeare Drama',
        description: 'Classic theatre performance by renowned artists',
        price: 900,
        eventDate: 'Sun, Feb 16 • 6:00 PM',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg',
        status: 'available'
    },
    {
        title: 'Google DevFest 2025',
        description: 'Tech conference with industry leaders',
        price: 400,
        eventDate: 'Sat, Mar 1 • 10:00 AM',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg',
        status: 'available'
    },
    {
        title: 'Mumbai Street Food Festival',
        description: 'All-you-can-eat food festival',
        price: 250,
        eventDate: 'Sat, Mar 22 • 12:00 PM',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg',
        status: 'available'
    }
];

async function seedDatabase() {
    const connection = await mysql.createConnection(process.env.DATABASE_URL + '?connectionLimit=2');

    try {
        console.log('🌱 Starting database seed...\n');

        // Clear existing data (optional - comment out if you want to keep existing data)
        console.log('🧹 Clearing existing data...');
        await connection.execute('SET FOREIGN_KEY_CHECKS = 0');
        await connection.execute('TRUNCATE TABLE messages');
        await connection.execute('TRUNCATE TABLE conversations');
        await connection.execute('TRUNCATE TABLE transactions');
        await connection.execute('TRUNCATE TABLE tickets');
        await connection.execute('TRUNCATE TABLE users');
        await connection.execute('SET FOREIGN_KEY_CHECKS = 1');
        console.log('✅ Cleared existing data\n');

        // Insert users
        console.log('👤 Creating users...');
        for (const user of users) {
            const passwordHash = await bcrypt.hash(user.password, SALT_ROUNDS);
            await connection.execute(
                `INSERT INTO users (id, email, password_hash, name, profile_image_url, wallet_balance)
                 VALUES (?, ?, ?, ?, ?, ?)`,
                [user.id, user.email, passwordHash, user.name, user.profileImageUrl, user.walletBalance]
            );
            console.log(`  ✅ ${user.name} (${user.email}) - Wallet: ₹${user.walletBalance}`);
        }
        console.log('');

        // Insert tickets
        console.log('🎫 Creating tickets...');
        const ticketIds = [];
        for (let i = 0; i < tickets.length; i++) {
            const ticket = tickets[i];
            const ticketId = uuidv4();
            ticketIds.push(ticketId);

            const sellerId = users[i % users.length].id;
            const buyerId = ticket.status === 'sold' && ticket.buyerIndex !== undefined
                ? users[ticket.buyerIndex].id
                : null;

            const originalPrice = Math.round(ticket.price * 1.2); // 20% above selling price

            await connection.execute(
                `INSERT INTO tickets (id, title, description, original_price, price, event_date, image_url, seller_id, status, buyer_id)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
                [ticketId, ticket.title, ticket.description, originalPrice, ticket.price, ticket.eventDate,
                 ticket.imageUrl, sellerId, ticket.status, buyerId]
            );

            console.log(`  ✅ ${ticket.title} - ₹${ticket.price} (${ticket.status})`);
        }
        console.log('');

        // Create transactions for sold tickets
        console.log('💰 Creating transaction history...');

        // Transaction for sold ticket #5 (Sunburn)
        await connection.execute(
            `INSERT INTO transactions (id, user_id, type, amount, description, ticket_id, status)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [uuidv4(), users[0].id, 'credit', 3200, 'Sold: Sunburn Goa 2025', ticketIds[4], 'completed']
        );
        await connection.execute(
            `INSERT INTO transactions (id, user_id, type, amount, description, ticket_id, status)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [uuidv4(), users[1].id, 'debit', 3200, 'Purchased: Sunburn Goa 2025', ticketIds[4], 'completed']
        );
        console.log('  ✅ Sunburn transaction');

        // Transaction for sold ticket #6 (Zakir Khan)
        await connection.execute(
            `INSERT INTO transactions (id, user_id, type, amount, description, ticket_id, status)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [uuidv4(), users[1].id, 'credit', 1200, 'Sold: Zakir Khan: Haq Se Single', ticketIds[5], 'completed']
        );
        await connection.execute(
            `INSERT INTO transactions (id, user_id, type, amount, description, ticket_id, status)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [uuidv4(), users[2].id, 'debit', 1200, 'Purchased: Zakir Khan: Haq Se Single', ticketIds[5], 'completed']
        );
        console.log('  ✅ Zakir Khan transaction');

        // Add some additional transactions for wallet history
        await connection.execute(
            `INSERT INTO transactions (id, user_id, type, amount, description, ticket_id, status)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [uuidv4(), users[0].id, 'credit', 5000, 'Added funds to wallet', null, 'completed']
        );
        await connection.execute(
            `INSERT INTO transactions (id, user_id, type, amount, description, ticket_id, status)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [uuidv4(), users[1].id, 'credit', 10000, 'Added funds to wallet', null, 'completed']
        );
        console.log('  ✅ Wallet top-up transactions');
        console.log('');

        // Create sample conversation
        console.log('💬 Creating sample conversations...');
        const chatId = users[0].id < users[1].id
            ? `${users[0].id}_${users[1].id}`
            : `${users[1].id}_${users[0].id}`;

        await connection.execute(
            `INSERT INTO conversations (id, user1_id, user2_id)
             VALUES (?, ?, ?)`,
            [chatId, users[0].id, users[1].id]
        );

        // Sample messages
        const messages = [
            { sender: users[0].id, receiver: users[1].id, msg: 'Hi! Is the Coldplay ticket still available?' },
            { sender: users[1].id, receiver: users[0].id, msg: 'Yes, it is! Are you interested?' },
            { sender: users[0].id, receiver: users[1].id, msg: 'Definitely! Can you share more details about the seats?' },
            { sender: users[1].id, receiver: users[0].id, msg: 'They are front row, Section A. You will have an amazing view!' },
            { sender: users[0].id, receiver: users[1].id, msg: 'Perfect! I will buy it right now.' }
        ];

        for (const msg of messages) {
            await connection.execute(
                `INSERT INTO messages (id, conversation_id, sender_id, receiver_id, message)
                 VALUES (?, ?, ?, ?, ?)`,
                [uuidv4(), chatId, msg.sender, msg.receiver, msg.msg]
            );
        }
        console.log('  ✅ Sample conversation created');
        console.log('');

        console.log('✨ Database seed completed successfully!');
        console.log('\n📋 Test Accounts:');
        console.log('  arjun@example.com / password123');
        console.log('  priya@example.com / password123');
        console.log('  rahul@example.com / password123');
        console.log('  test@example.com / test123');

    } catch (error) {
        console.error('❌ Error seeding database:', error);
        process.exit(1);
    } finally {
        await connection.end();
    }
}

// Run if called directly
if (require.main === module) {
    seedDatabase();
}

module.exports = { seedDatabase };
