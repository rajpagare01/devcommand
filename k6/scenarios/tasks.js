import { authenticateUser } from '../helpers/auth.js';
import { httpGet } from '../helpers/http.js';
import { THRESHOLDS } from '../config.js';
import { sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const tasksListDuration = new Trend('tasks_list_duration');
const tasksListFailure = new Rate('tasks_list_failure_rate');

const tasksTodayDuration = new Trend('tasks_today_duration');
const tasksTodayFailure = new Rate('tasks_today_failure_rate');

const tasksUpcomingDuration = new Trend('tasks_upcoming_duration');
const tasksUpcomingFailure = new Rate('tasks_upcoming_failure_rate');

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

    res = httpGet('/api/tasks', data.token, 200, 'GET /api/tasks');
    tasksListDuration.add(res.timings.duration);
    tasksListFailure.add(res.status !== 200);

    res = httpGet('/api/tasks/today', data.token, 200, 'GET /api/tasks/today');
    tasksTodayDuration.add(res.timings.duration);
    tasksTodayFailure.add(res.status !== 200);

    res = httpGet('/api/tasks/upcoming', data.token, 200, 'GET /api/tasks/upcoming');
    tasksUpcomingDuration.add(res.timings.duration);
    tasksUpcomingFailure.add(res.status !== 200);

    sleep(1);
}
