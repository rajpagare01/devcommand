import { authenticateUser } from '../helpers/auth.js';
import { httpGet } from '../helpers/http.js';
import { THRESHOLDS } from '../config.js';
import { sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const dsaListDuration = new Trend('dsa_list_duration');
const dsaListFailure = new Rate('dsa_list_failure_rate');

const dsaPaginatedDuration = new Trend('dsa_paginated_duration');
const dsaPaginatedFailure = new Rate('dsa_paginated_failure_rate');

export const options = {
    vus: 5,
    duration: '30s',
    thresholds: THRESHOLDS.smoke
};

export function setup() {
    return { token: authenticateUser() };
}

export default function (data) {
    let res;

    res = httpGet('/api/dsa', data.token, 200, 'GET /api/dsa');
    dsaListDuration.add(res.timings.duration);
    dsaListFailure.add(res.status !== 200);

    res = httpGet('/api/dsa?page=0&size=20', data.token, 200, 'GET /api/dsa paginated');
    dsaPaginatedDuration.add(res.timings.duration);
    dsaPaginatedFailure.add(res.status !== 200);

    sleep(1);
}
