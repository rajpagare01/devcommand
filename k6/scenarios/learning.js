import { authenticateUser } from '../helpers/auth.js';
import { httpGet } from '../helpers/http.js';
import { THRESHOLDS } from '../config.js';
import { sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const learningListDuration = new Trend('learning_list_duration');
const learningListFailure = new Rate('learning_list_failure_rate');

const learningCompletedDuration = new Trend('learning_completed_duration');
const learningCompletedFailure = new Rate('learning_completed_failure_rate');

const learningInProgressDuration = new Trend('learning_in_progress_duration');
const learningInProgressFailure = new Rate('learning_in_progress_failure_rate');

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

    res = httpGet('/api/learning', data.token, 200, 'GET /api/learning');
    learningListDuration.add(res.timings.duration);
    learningListFailure.add(res.status !== 200);

    res = httpGet('/api/learning/completed', data.token, 200, 'GET /api/learning/completed');
    learningCompletedDuration.add(res.timings.duration);
    learningCompletedFailure.add(res.status !== 200);

    res = httpGet('/api/learning/in-progress', data.token, 200, 'GET /api/learning/in-progress');
    learningInProgressDuration.add(res.timings.duration);
    learningInProgressFailure.add(res.status !== 200);

    sleep(1);
}
