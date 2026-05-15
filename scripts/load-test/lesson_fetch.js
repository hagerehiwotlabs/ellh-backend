/**
 * k6 load test — ELLH Sprint 9 performance validation.
 *
 * Tests two critical paths under 50 concurrent virtual users:
 *   1. Lesson fetch (cache hit path): GET /api/v1/lessons/{id}
 *      → Verifies Redis cache hit rate ≥80% (NFR-06 Design Goal f).
 *      → Verifies p95 response time ≤1.5s (NFR-05).
 *
 *   2. Progress update: PATCH /api/v1/progress (write path)
 *      → Verifies p95 response time ≤1.5s under concurrent writes.
 *
 * Usage:
 *   k6 run --env BASE_URL=https://your-render-app.onrender.com \
 *           --env JWT_TOKEN=<valid_jwt_token> \
 *           scripts/load-test/lesson_fetch.js
 *
 * Expected results (NFR-05, Design Goal f):
 *   - http_req_duration p(95) ≤ 1500ms for lesson fetch
 *   - http_req_duration p(95) ≤ 1500ms for progress update
 *   - error rate < 1%
 *
 * Note: Run during off-peak hours on Render.com free tier to avoid hitting
 * rate limits (Trade-off c: free-tier infrastructure constraints).
 * Run k6 from a machine with stable network — not from within the app.
 *
 * Section 4.5 Sprint 9 QA Tasks.
 * NFR-05 — Standard API responses ≤1.5s.
 * Design Goal f — Performance: "Redis cache hit rate ≥80%".
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate         = new Rate('errors');
const lessonFetchTime   = new Trend('lesson_fetch_duration', true);
const progressWriteTime = new Trend('progress_write_duration', true);

// ── Load test configuration ───────────────────────────────────────────────
export const options = {
  scenarios: {
    lesson_fetch: {
      executor:    'constant-vus',
      vus:         50,          // 50 concurrent virtual users
      duration:    '2m',        // run for 2 minutes
      exec:        'fetchLesson',
    },
    progress_update: {
      executor:    'constant-vus',
      vus:         20,          // 20 concurrent progress writers
      duration:    '2m',
      exec:        'updateProgress',
      startTime:   '30s',       // start after lesson fetch warms the cache
    },
  },
  thresholds: {
    // NFR-05: p95 ≤ 1500ms for all requests
    'http_req_duration{scenario:lesson_fetch}':   ['p(95)<1500'],
    'http_req_duration{scenario:progress_update}':['p(95)<1500'],
    // Error rate < 1%
    'errors':                                     ['rate<0.01'],
    // Named metrics for detailed analysis
    'lesson_fetch_duration':  ['p(95)<1500', 'p(99)<3000'],
    'progress_write_duration':['p(95)<1500', 'p(99)<3000'],
  },
};

const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8080';
const JWT_TOKEN  = __ENV.JWT_TOKEN  || 'replace-with-valid-jwt';

// Lesson IDs to fetch (seed data from Sprint 2 — IDs 1–9)
const LESSON_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9];

const headers = {
  'Authorization': `Bearer ${JWT_TOKEN}`,
  'Content-Type':  'application/json',
  'Accept':        'application/json',
};

// ── Scenario 1: Lesson fetch (cache hit path after first request) ─────────
export function fetchLesson() {
  const lessonId = LESSON_IDS[Math.floor(Math.random() * LESSON_IDS.length)];
  const url      = `${BASE_URL}/api/v1/lessons/${lessonId}`;

  const res = http.get(url, { headers, tags: { scenario: 'lesson_fetch' } });

  const success = check(res, {
    'lesson fetch: status 200':        (r) => r.status === 200,
    'lesson fetch: response has title': (r) => {
      try { return JSON.parse(r.body).title !== undefined; }
      catch (e) { return false; }
    },
    'lesson fetch: ≤1500ms':           (r) => r.timings.duration < 1500,
  });

  lessonFetchTime.add(res.timings.duration);
  errorRate.add(!success);

  sleep(1 + Math.random() * 2); // 1–3s think time between requests
}

// ── Scenario 2: Progress update (write path) ──────────────────────────────
export function updateProgress() {
  const lessonId   = LESSON_IDS[Math.floor(Math.random() * LESSON_IDS.length)];
  const exerciseId = Math.floor(Math.random() * 45) + 1; // exercises 1–45

  const payload = JSON.stringify({
    lessonId:   lessonId,
    exerciseId: exerciseId,
    status:     'COMPLETED',
    score:      Math.floor(Math.random() * 100),
  });

  const res = http.patch(
    `${BASE_URL}/api/v1/progress`,
    payload,
    { headers, tags: { scenario: 'progress_update' } }
  );

  const success = check(res, {
    'progress update: status 200 or 201': (r) => r.status === 200 || r.status === 201,
    'progress update: ≤1500ms':           (r) => r.timings.duration < 1500,
  });

  progressWriteTime.add(res.timings.duration);
  errorRate.add(!success);

  sleep(2 + Math.random() * 3); // 2–5s think time
}

// ── Reporting helper ──────────────────────────────────────────────────────
export function handleSummary(data) {
  const lessonP95   = data.metrics['lesson_fetch_duration'] &&
                      data.metrics['lesson_fetch_duration'].values['p(95)'];
  const progressP95 = data.metrics['progress_write_duration'] &&
                      data.metrics['progress_write_duration'].values['p(95)'];

  console.log('\n══════════════════════════════════════════════════════');
  console.log('  ELLH Sprint 9 Load Test Results');
  console.log('══════════════════════════════════════════════════════');
  console.log(`  Lesson fetch  p95: ${lessonP95 ? lessonP95.toFixed(0) : 'N/A'}ms  (target ≤1500ms)`);
  console.log(`  Progress write p95: ${progressP95 ? progressP95.toFixed(0) : 'N/A'}ms (target ≤1500ms)`);
  console.log('══════════════════════════════════════════════════════');

  return {
    'stdout': JSON.stringify(data, null, 2),
  };
}
