/**
 * API Test Script
 * Run with: node scripts/test-api.js
 * Tests all API endpoints to verify deployment
 */

const API_BASE_URL = process.env.API_URL || 'https://backend-three-phi-61.vercel.app/api';

async function testEndpoint(name, method, endpoint, body = null, authToken = null) {
    const url = `${API_BASE_URL}${endpoint}`;
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        }
    };

    if (authToken) {
        options.headers['Authorization'] = `Bearer ${authToken}`;
    }

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        console.log(`\n🧪 Testing: ${name}`);
        console.log(`   ${method} ${url}`);

        const response = await fetch(url, options);
        const data = await response.json();

        if (response.ok && data.success) {
            console.log(`   ✅ PASSED (${response.status})`);
            return { success: true, data };
        } else {
            console.log(`   ❌ FAILED (${response.status})`);
            console.log(`   Error: ${JSON.stringify(data.error || data)}`);
            return { success: false, error: data.error };
        }
    } catch (error) {
        console.log(`   ❌ ERROR: ${error.message}`);
        return { success: false, error: error.message };
    }
}

async function runTests() {
    console.log('═══════════════════════════════════════════════');
    console.log('🚀 ReTix API Test Suite');
    console.log(`🌐 API URL: ${API_BASE_URL}`);
    console.log('═══════════════════════════════════════════════');

    let authToken = null;
    let testUserId = null;

    // 1. Health Check
    await testEndpoint('Health Check', 'GET', '/');

    // 2. Get Public Tickets
    await testEndpoint('Get Public Tickets', 'GET', '/tickets');

    // 3. Signup
    const signupResult = await testEndpoint('User Signup', 'POST', '/auth/signup', {
        email: `test_${Date.now()}@example.com`,
        password: 'testpassword123',
        name: 'Test User'
    });

    if (signupResult.success) {
        authToken = signupResult.data.data.token;
        testUserId = signupResult.data.data.user.id;
        console.log(`   Token received: ${authToken.substring(0, 20)}...`);
    }

    // 4. Login (if signup failed, try with a known user)
    if (!authToken) {
        const loginResult = await testEndpoint('User Login', 'POST', '/auth/login', {
            email: 'test@example.com',
            password: 'test123'
        });

        if (loginResult.success) {
            authToken = loginResult.data.data.token;
            testUserId = loginResult.data.data.user.id;
        }
    }

    // Skip auth-required tests if we don't have a token
    if (!authToken) {
        console.log('\n⚠️ Skipping authenticated endpoints (no valid token)');
        console.log('═══════════════════════════════════════════════');
        console.log('❌ Tests completed with failures');
        return;
    }

    console.log(`\n🔐 Using auth token for user: ${testUserId}`);

    // 5. Get Profile
    await testEndpoint('Get Profile', 'GET', '/users/profile', null, authToken);

    // 6. Get User Stats
    await testEndpoint('Get User Stats', 'GET', '/users/stats', null, authToken);

    // 7. Get Wallet Balance
    await testEndpoint('Get Wallet Balance', 'GET', '/wallet/balance', null, authToken);

    // 8. Get Wallet Transactions
    await testEndpoint('Get Wallet Transactions', 'GET', '/wallet/transactions', null, authToken);

    // 9. Get My Listings
    await testEndpoint('Get My Listings', 'GET', '/tickets/my-listings', null, authToken);

    // 10. Get My Purchases
    await testEndpoint('Get My Purchases', 'GET', '/tickets/my-purchases', null, authToken);

    // 11. Get Chat Conversations
    await testEndpoint('Get Conversations', 'GET', '/chat/conversations', null, authToken);

    // Summary
    console.log('\n═══════════════════════════════════════════════');
    console.log('✅ Tests completed');
    console.log('═══════════════════════════════════════════════');
}

// Check if fetch is available (Node 18+)
if (typeof fetch === 'undefined') {
    console.log('❌ This script requires Node.js 18+ with native fetch support');
    console.log('   Please upgrade Node.js or use a tool like node-fetch');
    process.exit(1);
}

runTests().catch(console.error);
