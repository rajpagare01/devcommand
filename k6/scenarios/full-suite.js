import { authenticateUser } from '../helpers/auth.js';
import analyticsScenario from './analytics.js';
import dsaScenario from './dsa.js';
import jobsScenario from './jobs.js';
import learningScenario from './learning.js';
import projectsScenario from './projects.js';
import tasksScenario from './tasks.js';
import { THRESHOLDS } from '../config.js';
import { sleep } from 'k6';

export const options = {
    vus: 25,
    duration: '30s',
    thresholds: THRESHOLDS.baseline
};

export function setup() {
    return { token: authenticateUser() };
}

export default function (data) {
    analyticsScenario(data);
    dsaScenario(data);
    jobsScenario(data);
    learningScenario(data);
    projectsScenario(data);
    tasksScenario(data);
    
    sleep(1);
}
