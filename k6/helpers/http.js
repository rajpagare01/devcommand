import http from 'k6/http';
import { check } from 'k6';
import { CONFIG } from '../config.js';

export function getHeaders(token) {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}

export function httpGet(url, token, expectedStatus = 200, checkName = 'GET status is correct') {
    const params = { headers: getHeaders(token) };
    const res = http.get(`${CONFIG.BASE_URL}${url}`, params);
    check(res, {
        [checkName]: (r) => r.status === expectedStatus
    });
    return res;
}

export function httpPost(url, payload, token, expectedStatus = 201, checkName = 'POST status is correct') {
    const params = { headers: getHeaders(token) };
    const res = http.post(`${CONFIG.BASE_URL}${url}`, JSON.stringify(payload), params);
    check(res, {
        [checkName]: (r) => r.status === expectedStatus
    });
    return res;
}

export function httpPut(url, payload, token, expectedStatus = 200, checkName = 'PUT status is correct') {
    const params = { headers: getHeaders(token) };
    const res = http.put(`${CONFIG.BASE_URL}${url}`, JSON.stringify(payload), params);
    check(res, {
        [checkName]: (r) => r.status === expectedStatus
    });
    return res;
}

export function httpPatch(url, payload, token, expectedStatus = 200, checkName = 'PATCH status is correct') {
    const params = { headers: getHeaders(token) };
    const res = http.patch(`${CONFIG.BASE_URL}${url}`, JSON.stringify(payload), params);
    check(res, {
        [checkName]: (r) => r.status === expectedStatus
    });
    return res;
}

export function httpDelete(url, token, expectedStatus = 200, checkName = 'DELETE status is correct') {
    const params = { headers: getHeaders(token) };
    const res = http.del(`${CONFIG.BASE_URL}${url}`, null, params);
    check(res, {
        [checkName]: (r) => r.status === expectedStatus || r.status === 204
    });
    return res;
}
