import { authenticateUser } from '../helpers/auth.js';
import { THRESHOLDS } from '../config.js';
import { check } from 'k6';
import http from 'k6/http';
import { CONFIG } from '../config.js';
import { Trend, Rate } from 'k6/metrics';

const authLoginDuration = new Trend('auth_login_duration');
const authLoginFailure = new Rate('auth_login_failure_rate');

export const options = {
    vus: 2,
    duration: '10s', // smoke test by default
    thresholds: THRESHOLDS.smoke
};

export default function () {
    const loginUrl = `${CONFIG.BASE_URL}/api/auth/login`;
    const payload = JSON.stringify({
        email: CONFIG.TEST_EMAIL,
        password: CONFIG.TEST_PASSWORD
    });
    
    const params = {
        headers: { 'Content-Type': 'application/json' },
    };

    const res = http.post(loginUrl, payload, params);
    
    check(res, {
        'login successful': (r) => r.status === 200,
        'has token': (r) => r.json('token') !== undefined
    });

    authLoginDuration.add(res.timings.duration);
    authLoginFailure.add(res.status !== 200);
}
