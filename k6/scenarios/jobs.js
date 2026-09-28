import { authenticateUser } from '../helpers/auth.js';
import { httpGet } from '../helpers/http.js';
import { THRESHOLDS } from '../config.js';
import { sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const jobsListDuration = new Trend('jobs_list_duration');
const jobsListFailure = new Rate('jobs_list_failure_rate');

export const options = {
    vus: 5,
    duration: '30s',
    thresholds: THRESHOLDS.smoke
};

export function setup() {
    return { token: authenticateUser() };
}

export default function (data) {
    let res = httpGet('/api/jobs', data.token, 200, 'GET /api/jobs');
    jobsListDuration.add(res.timings.duration);
    jobsListFailure.add(res.status !== 200);

    // Note: To test /api/jobs/{id} realistically without creating records in test,
    // we would need an existing ID. For safety, we only test list endpoints in pure load tests.
    sleep(1);
}
