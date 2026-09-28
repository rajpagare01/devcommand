import { authenticateUser } from '../helpers/auth.js';
import { httpGet } from '../helpers/http.js';
import { THRESHOLDS } from '../config.js';
import { sleep } from 'k6';

export const options = {
    scenarios: {
        mixed_workload: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 }, // ramp up
                { duration: '1m', target: 20 },  // steady load
                { duration: '30s', target: 0 },  // ramp down
            ],
        },
    },
    thresholds: THRESHOLDS.load
};

export function setup() {
    return { token: authenticateUser() };
}

export default function (data) {
    const r = Math.random();
    
    // 30% Dashboard / Analytics
    if (r < 0.30) {
        httpGet('/api/analytics/overview', data.token, 200, 'GET overview');
        httpGet('/api/analytics/tasks', data.token, 200, 'GET tasks analytics');
    } 
    // 20% Tasks
    else if (r < 0.50) {
        httpGet('/api/tasks', data.token, 200, 'GET tasks');
        httpGet('/api/tasks/today', data.token, 200, 'GET tasks/today');
    } 
    // 15% DSA
    else if (r < 0.65) {
        httpGet('/api/dsa', data.token, 200, 'GET dsa');
    } 
    // 15% Jobs
    else if (r < 0.80) {
        httpGet('/api/jobs', data.token, 200, 'GET jobs');
    } 
    // 10% Learning
    else if (r < 0.90) {
        httpGet('/api/learning', data.token, 200, 'GET learning');
    } 
    // 10% Projects
    else {
        httpGet('/api/projects', data.token, 200, 'GET projects');
    }

    sleep(1);
}
