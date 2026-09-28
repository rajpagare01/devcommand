export const CONFIG = {
    BASE_URL: __ENV.BASE_URL || 'http://localhost:8080',
    TEST_EMAIL: __ENV.K6_TEST_EMAIL || 'k6-test@example.com',
    TEST_PASSWORD: __ENV.K6_TEST_PASSWORD || 'password123',
    REGISTER_EMAIL: __ENV.K6_REGISTER_EMAIL || 'k6-test@example.com',
    REGISTER_PASSWORD: __ENV.K6_REGISTER_PASSWORD || 'password123',
    REGISTER_NAME: __ENV.K6_REGISTER_NAME || 'K6 Performance Test'
};

export const THRESHOLDS = {
    smoke: {
        http_req_failed: ['rate<0.01'], // less than 1% errors
        http_req_duration: ['p(95)<500'] // 95% of requests under 500ms
    },
    baseline: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000']
    },
    load: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<2000']
    }
};
