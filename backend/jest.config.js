module.exports = {
    testEnvironment: 'node',
    testMatch: ['**/__tests__/**/*.test.js'],
    collectCoverageFrom: [
        'api/**/*.js',
        'lib/**/*.js',
        '!api/index.js'
    ],
    testTimeout: 30000,
    verbose: true
};