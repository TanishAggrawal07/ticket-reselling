/**
 * ReTix Backend Test Suite
 * Run with: node scripts/test-suite.js
 */

const fetch = require('node-fetch');

const BASE_URL = process.env.API_URL || 'https://backend-three-phi-61.vercel.app';
const COLORS = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m'
};

class TestRunner {
  constructor() {
    this.results = { passed: 0, failed: 0, skipped: 0 };
    this.authToken = null;
    this.userId = null;
    this.testTicketId = null;
  }

  log(message, color = COLORS.reset) {
    console.log(`${color}${message}${COLORS.reset}`);
  }

  async runTest(name, testFn) {
    try {
      await testFn();
      this.results.passed++;
      this.log(`  ✓ ${name}`, COLORS.green);
    } catch (error) {
      this.results.failed++;
      this.log(`  ✗ ${name}: ${error.message}`, COLORS.red);
    }
  }

  async makeRequest(endpoint, options = {}) {
    const url = `${BASE_URL}${endpoint}`;
    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(this.authToken && { 'Authorization': `Bearer ${this.authToken}` }),
        ...options.headers
      }
    });

    const data = await response.json().catch(() => null);
    return { status: response.status, data, ok: response.ok };
  }

  // ============ TEST CASES ============

  async testHealth() {
    const { ok, data } = await this.makeRequest('/');
    if (!ok) throw new Error('Health check failed');
  }

  async testSignup() {
    const uniqueEmail = `test${Date.now()}@example.com`;
    const { ok, data } = await this.makeRequest('/api/auth/signup', {
      method: 'POST',
      body: JSON.stringify({
        email: uniqueEmail,
        password: 'testpassword123',
        name: 'Test User'
      })
    });

    if (!ok) throw new Error(data?.error?.message || 'Signup failed');
    this.authToken = data.data.token;
    this.userId = data.data.user.id;
  }

  async testLogin() {
    // Uses credentials from signup
    const { ok, data } = await this.makeRequest('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        email: 'test@example.com',
        password: 'testpassword123'
      })
    });

    // If login fails, we'll skip dependent tests
    if (!ok) {
      this.results.skipped++;
      throw new Error('Login failed - using existing token if available');
    }

    this.authToken = data.data.token;
    this.userId = data.data.user.id;
  }

  async testGetProfile() {
    const { ok, data } = await this.makeRequest('/api/users/profile');
    if (!ok) throw new Error(data?.error?.message || 'Failed to get profile');
  }

  async testUpdateProfile() {
    const { ok, data } = await this.makeRequest('/api/users/profile', {
      method: 'PUT',
      body: JSON.stringify({
        name: 'Updated Test User',
        profileImageUrl: 'https://example.com/image.jpg'
      })
    });
    if (!ok) throw new Error(data?.error?.message || 'Failed to update profile');
  }

  async testGetWalletBalance() {
    const { ok, data } = await this.makeRequest('/api/wallet/balance');
    if (!ok) throw new Error(data?.error?.message || 'Failed to get wallet balance');
  }

  async testGetTransactions() {
    const { ok, data } = await this.makeRequest('/api/wallet/transactions?limit=10&offset=0');
    if (!ok) throw new Error(data?.error?.message || 'Failed to get transactions');
  }

  async testCreateTicket() {
    const { ok, data } = await this.makeRequest('/api/tickets', {
      method: 'POST',
      body: JSON.stringify({
        title: 'Test Event Ticket',
        description: 'Test description',
        originalPrice: 100,
        price: 80,
        eventDate: '2025-12-31T18:00:00Z',
        imageUrl: 'https://example.com/ticket.jpg'
      })
    });

    if (!ok) throw new Error(data?.error?.message || 'Failed to create ticket');
    this.testTicketId = data.data.ticketId;
  }

  async testGetTickets() {
    const { ok, data } = await this.makeRequest('/api/tickets?limit=10&offset=0');
    if (!ok) throw new Error(data?.error?.message || 'Failed to get tickets');
  }

  async testGetMyListings() {
    const { ok, data } = await this.makeRequest('/api/tickets/my-listings?limit=10&offset=0');
    if (!ok) throw new Error(data?.error?.message || 'Failed to get listings');
  }

  async testGetMyPurchases() {
    const { ok, data } = await this.makeRequest('/api/tickets/my-purchases?limit=10&offset=0');
    if (!ok) throw new Error(data?.error?.message || 'Failed to get purchases');
  }

  async testBuyTicket() {
    // Try to buy own ticket (should fail)
    if (!this.testTicketId) {
      this.results.skipped++;
      throw new Error('No test ticket available');
    }

    const { ok, data } = await this.makeRequest(`/api/tickets/buy/${this.testTicketId}`, {
      method: 'POST'
    });

    // Buying own ticket should fail with OWN_TICKET
    if (ok) {
      throw new Error('Should not be able to buy own ticket');
    }

    if (data?.error?.code !== 'OWN_TICKET') {
      throw new Error(`Expected OWN_TICKET error, got: ${data?.error?.code}`);
    }
  }

  async testChatConversations() {
    const { ok, data } = await this.makeRequest('/api/chat/conversations');
    if (!ok) throw new Error(data?.error?.message || 'Failed to get conversations');
  }

  async testSendMessage() {
    // Sending message to self should fail
    const { data } = await this.makeRequest('/api/chat/send', {
      method: 'POST',
      body: JSON.stringify({
        receiverId: this.userId,
        message: 'Test message'
      })
    });

    if (data?.success) {
      throw new Error('Should not be able to send message to self');
    }
  }

  async testInvalidAuth() {
    const oldToken = this.authToken;
    this.authToken = 'invalid_token';

    const { ok } = await this.makeRequest('/api/users/profile');

    this.authToken = oldToken;

    if (ok) {
      throw new Error('Should reject invalid token');
    }
  }

  // ============ MAIN RUNNER ============

  async run() {
    this.log('\n═══════════════════════════════════════════', COLORS.cyan);
    this.log('       ReTix Backend Test Suite', COLORS.cyan);
    this.log('═══════════════════════════════════════════\n', COLORS.cyan);

    // Health check
    this.log('Health Check:', COLORS.blue);
    await this.runTest('API is reachable', () => this.testHealth());

    // Authentication
    this.log('\nAuthentication:', COLORS.blue);
    await this.runTest('User signup', () => this.testSignup());
    await this.runTest('User login', () => this.testLogin());

    if (!this.authToken) {
      this.log('\n⚠️  No valid token - skipping authenticated tests', COLORS.yellow);
      return;
    }

    // User endpoints
    this.log('\nUser Endpoints:', COLORS.blue);
    await this.runTest('Get user profile', () => this.testGetProfile());
    await this.runTest('Update user profile', () => this.testUpdateProfile());

    // Wallet endpoints
    this.log('\nWallet Endpoints:', COLORS.blue);
    await this.runTest('Get wallet balance', () => this.testGetWalletBalance());
    await this.runTest('Get transaction history', () => this.testGetTransactions());

    // Ticket endpoints
    this.log('\nTicket Endpoints:', COLORS.blue);
    await this.runTest('Create ticket', () => this.testCreateTicket());
    await this.runTest('Get available tickets', () => this.testGetTickets());
    await this.runTest('Get my listings', () => this.testGetMyListings());
    await this.runTest('Get my purchases', () => this.testGetMyPurchases());
    await this.runTest('Cannot buy own ticket', () => this.testBuyTicket());

    // Chat endpoints
    this.log('\nChat Endpoints:', COLORS.blue);
    await this.runTest('Get conversations', () => this.testChatConversations());
    await this.runTest('Cannot send self message', () => this.testSendMessage());

    // Security
    this.log('\nSecurity:', COLORS.blue);
    await this.runTest('Reject invalid auth token', () => this.testInvalidAuth());

    // Summary
    this.log('\n═══════════════════════════════════════════', COLORS.cyan);
    this.log('              Test Summary', COLORS.cyan);
    this.log('═══════════════════════════════════════════', COLORS.cyan);
    this.log(`  Passed:  ${this.results.passed}`, COLORS.green);
    this.log(`  Failed:  ${this.results.failed}`, this.results.failed > 0 ? COLORS.red : COLORS.green);
    this.log(`  Skipped: ${this.results.skipped}`, COLORS.yellow);
    this.log('═══════════════════════════════════════════\n', COLORS.cyan);

    process.exit(this.results.failed > 0 ? 1 : 0);
  }
}

// Run tests
const runner = new TestRunner();
runner.run().catch(error => {
  console.error('Test runner failed:', error);
  process.exit(1);
});
