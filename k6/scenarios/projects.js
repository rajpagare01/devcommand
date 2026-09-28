import { authenticateUser } from '../helpers/auth.js';
import { httpGet } from '../helpers/http.js';
import { THRESHOLDS } from '../config.js';
import { sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const projectsListDuration = new Trend('projects_list_duration');
const projectsListFailure = new Rate('projects_list_failure_rate');

const projectsActiveDuration = new Trend('projects_active_duration');
const projectsActiveFailure = new Rate('projects_active_failure_rate');

const projectsCompletedDuration = new Trend('projects_completed_duration');
const projectsCompletedFailure = new Rate('projects_completed_failure_rate');

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

    res = httpGet('/api/projects', data.token, 200, 'GET /api/projects');
    projectsListDuration.add(res.timings.duration);
    projectsListFailure.add(res.status !== 200);

    res = httpGet('/api/projects/active', data.token, 200, 'GET /api/projects/active');
    projectsActiveDuration.add(res.timings.duration);
    projectsActiveFailure.add(res.status !== 200);

    res = httpGet('/api/projects/completed', data.token, 200, 'GET /api/projects/completed');
    projectsCompletedDuration.add(res.timings.duration);
    projectsCompletedFailure.add(res.status !== 200);

    sleep(1);
}
