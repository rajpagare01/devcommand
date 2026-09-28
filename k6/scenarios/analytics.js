import { authenticateUser } from '../helpers/auth.js';
import { httpGet } from '../helpers/http.js';
import { THRESHOLDS } from '../config.js';
import { Trend, Rate } from 'k6/metrics';

// Custom metrics for endpoint-specific measurement
const overviewDuration = new Trend('analytics_overview_duration');
const overviewFailure = new Rate('analytics_overview_failure_rate');

const dsaDuration = new Trend('analytics_dsa_duration');
const dsaFailure = new Rate('analytics_dsa_failure_rate');

const tasksDuration = new Trend('analytics_tasks_duration');
const tasksFailure = new Rate('analytics_tasks_failure_rate');

const jobsDuration = new Trend('analytics_jobs_duration');
const jobsFailure = new Rate('analytics_jobs_failure_rate');

const learningDuration = new Trend('analytics_learning_duration');
const learningFailure = new Rate('analytics_learning_failure_rate');

const projectsDuration = new Trend('analytics_projects_duration');
const projectsFailure = new Rate('analytics_projects_failure_rate');

export const options = {
    vus: 10,
    duration: '30s', // baseline default
    thresholds: THRESHOLDS.baseline
};

export function setup() {
    // Authenticate once and share token among VUs
    return { token: authenticateUser() };
}

export default function (data) {
    let res;

    // Overview
    res = httpGet('/api/analytics/overview', data.token, 200, 'overview analytics OK');
    overviewDuration.add(res.timings.duration);
    overviewFailure.add(res.status !== 200);

    // DSA
    res = httpGet('/api/analytics/dsa', data.token, 200, 'dsa analytics OK');
    dsaDuration.add(res.timings.duration);
    dsaFailure.add(res.status !== 200);

    // Tasks
    res = httpGet('/api/analytics/tasks', data.token, 200, 'tasks analytics OK');
    tasksDuration.add(res.timings.duration);
    tasksFailure.add(res.status !== 200);

    // Jobs
    res = httpGet('/api/analytics/jobs', data.token, 200, 'jobs analytics OK');
    jobsDuration.add(res.timings.duration);
    jobsFailure.add(res.status !== 200);

    // Learning
    res = httpGet('/api/analytics/learning', data.token, 200, 'learning analytics OK');
    learningDuration.add(res.timings.duration);
    learningFailure.add(res.status !== 200);

    // Projects
    res = httpGet('/api/analytics/projects', data.token, 200, 'projects analytics OK');
    projectsDuration.add(res.timings.duration);
    projectsFailure.add(res.status !== 200);
}
