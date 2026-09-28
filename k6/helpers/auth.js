import http from 'k6/http';
import { check } from 'k6';
import { CONFIG } from '../config.js';

export function authenticateUser() {
    const loginUrl = `${CONFIG.BASE_URL}/api/auth/login`;
    const registerUrl = `${CONFIG.BASE_URL}/api/auth/register`;

    const loginPayload = JSON.stringify({
        email: CONFIG.TEST_EMAIL,
        password: CONFIG.TEST_PASSWORD
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
    };

    // Try logging in first
    let res = http.post(loginUrl, loginPayload, params);

    // If login fails (e.g., 401 Unauthorized because user doesn't exist), register the user
    if (res.status !== 200) {
        const registerPayload = JSON.stringify({
            name: CONFIG.REGISTER_NAME,
            email: CONFIG.REGISTER_EMAIL,
            password: CONFIG.REGISTER_PASSWORD
        });

        res = http.post(registerUrl, registerPayload, params);
        
        check(res, {
            'registration successful': (r) => r.status === 201 || r.status === 200,
        });

        // Try logging in again after registration
        res = http.post(loginUrl, loginPayload, params);
    }

    check(res, {
        'login successful': (r) => r.status === 200,
        'has token': (r) => r.json('token') !== undefined,
    });

    return res.json('token');
}
