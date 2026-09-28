import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { CONFIG } from '../config.js';
import { authenticateUser } from '../helpers/auth.js';

export const options = {
    vus: 25,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.01'],
    },
};

const overviewDuration = new Trend('analytics_overview_duration');
const overviewFailure = new Rate('analytics_overview_failure_rate');

export function setup() {
    return { token: authenticateUser() };
}

export default function (data) {
    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'Content-Type': 'application/json',
        },
    };

    const overviewRes = http.get(`${CONFIG.BASE_URL}/api/analytics/overview`, params);
    overviewDuration.add(overviewRes.timings.duration);
    overviewFailure.add(overviewRes.status !== 200);
    check(overviewRes, { 'overview analytics OK': (r) => r.status === 200 });
}
