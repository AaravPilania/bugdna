-- Flyway runs on the app connection pool; mark seed operations so activity_log shows 'SEED'
SET @bugdna_actor = 'SEED';

-- 1. Project
INSERT INTO projects (id, name, project_key, allowed_origin, created_at)
VALUES (1, 'Demo Store', 'demo-store-dev-key', 'http://localhost:5174', NOW(3));

-- 2. Tracking Sessions
INSERT INTO tracking_sessions (id, project_id, session_id, user_agent, first_seen_at, last_seen_at)
VALUES 
(1, 1, 'sess-dev-alpha-001', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/130.0.0.0 Safari/537.36', DATE_SUB(NOW(3), INTERVAL 2 HOUR), NOW(3)),
(2, 1, 'sess-dev-beta-002', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15', DATE_SUB(NOW(3), INTERVAL 1 HOUR), NOW(3));

-- 3. Bugs (Explicit IDs 101-106, canonical SHA-256 fingerprints)
-- Bug 101: TypeError on /product/:id
INSERT INTO bugs (id, project_id, fingerprint, title, error_type, error_code, http_method, endpoint, page_path, status, priority, assignee, occurrence_count, is_regression, regression_count, first_seen_at, last_seen_at, created_at, updated_at)
VALUES (
  101, 1,
  SHA2('v1|JS_ERROR|TypeError|cannot read properties of undefined (reading \'price\')|productpage.js:renderprice', 256),
  'TypeError: Cannot read properties of undefined (reading \'price\')',
  'JS_ERROR', NULL, NULL, NULL, '/product/1',
  'OPEN', 'HIGH', NULL, 3, 0, 0,
  DATE_SUB(NOW(3), INTERVAL 110 MINUTE), DATE_SUB(NOW(3), INTERVAL 15 MINUTE),
  DATE_SUB(NOW(3), INTERVAL 110 MINUTE), DATE_SUB(NOW(3), INTERVAL 15 MINUTE)
);

-- Bug 102: 404 Product Not Found
INSERT INTO bugs (id, project_id, fingerprint, title, error_type, error_code, http_method, endpoint, page_path, status, priority, assignee, occurrence_count, is_regression, regression_count, first_seen_at, last_seen_at, created_at, updated_at)
VALUES (
  102, 1,
  SHA2('v1|API_ERROR|GET|/demo-api/products/<id>|404|PRODUCT_NOT_FOUND', 256),
  'GET /demo-api/products/999 → 404 PRODUCT_NOT_FOUND',
  'API_ERROR', 'PRODUCT_NOT_FOUND', 'GET', '/demo-api/products/<id>', '/product/999',
  'OPEN', 'MEDIUM', NULL, 2, 0, 0,
  DATE_SUB(NOW(3), INTERVAL 90 MINUTE), DATE_SUB(NOW(3), INTERVAL 40 MINUTE),
  DATE_SUB(NOW(3), INTERVAL 90 MINUTE), DATE_SUB(NOW(3), INTERVAL 40 MINUTE)
);

-- Bug 103: Network Error, RESOLVED
INSERT INTO bugs (id, project_id, fingerprint, title, error_type, error_code, http_method, endpoint, page_path, status, priority, assignee, occurrence_count, is_regression, regression_count, first_seen_at, last_seen_at, resolved_at, created_at, updated_at)
VALUES (
  103, 1,
  SHA2('v1|NETWORK_ERROR|POST|/demo-api/analytics/heartbeat', 256),
  'Failed to fetch POST /demo-api/analytics/heartbeat',
  'NETWORK_ERROR', NULL, 'POST', '/demo-api/analytics/heartbeat', '/cart',
  'RESOLVED', 'LOW', 'Aarav', 1, 0, 0,
  DATE_SUB(NOW(3), INTERVAL 80 MINUTE), DATE_SUB(NOW(3), INTERVAL 50 MINUTE), DATE_SUB(NOW(3), INTERVAL 30 MINUTE),
  DATE_SUB(NOW(3), INTERVAL 80 MINUTE), DATE_SUB(NOW(3), INTERVAL 30 MINUTE)
);

-- Bug 104: Used in teacher demo (API_ERROR, status OPEN)
INSERT INTO bugs (id, project_id, fingerprint, title, error_type, error_code, http_method, endpoint, page_path, status, priority, assignee, occurrence_count, is_regression, regression_count, first_seen_at, last_seen_at, created_at, updated_at)
VALUES (
  104, 1,
  SHA2('v1|API_ERROR|POST|/demo-api/cart/coupon|400|INVALID_COUPON_FORMAT', 256),
  'POST /demo-api/cart/coupon → 400 INVALID_COUPON_FORMAT',
  'API_ERROR', 'INVALID_COUPON_FORMAT', 'POST', '/demo-api/cart/coupon', '/cart',
  'OPEN', 'MEDIUM', NULL, 4, 0, 0,
  DATE_SUB(NOW(3), INTERVAL 75 MINUTE), DATE_SUB(NOW(3), INTERVAL 10 MINUTE),
  DATE_SUB(NOW(3), INTERVAL 75 MINUTE), DATE_SUB(NOW(3), INTERVAL 10 MINUTE)
);

-- Bug 105: Promise Rejection, IN_PROGRESS with Assignee
INSERT INTO bugs (id, project_id, fingerprint, title, error_type, error_code, http_method, endpoint, page_path, status, priority, assignee, occurrence_count, is_regression, regression_count, first_seen_at, last_seen_at, created_at, updated_at)
VALUES (
  105, 1,
  SHA2('v1|PROMISE_REJECTION|Error|sync failed with status <n>|wishlist.js:syncwishlist', 256),
  'UnhandledRejection: WishlistSyncError: Sync failed with status 503',
  'PROMISE_REJECTION', NULL, NULL, NULL, '/wishlist',
  'IN_PROGRESS', 'HIGH', 'Member 1', 2, 0, 0,
  DATE_SUB(NOW(3), INTERVAL 60 MINUTE), DATE_SUB(NOW(3), INTERVAL 20 MINUTE),
  DATE_SUB(NOW(3), INTERVAL 60 MINUTE), DATE_SUB(NOW(3), INTERVAL 20 MINUTE)
);

-- Bug 106: Flagged Regression (REOPENED, is_regression = 1)
INSERT INTO bugs (id, project_id, fingerprint, title, error_type, error_code, http_method, endpoint, page_path, status, priority, assignee, occurrence_count, is_regression, regression_count, first_seen_at, last_seen_at, created_at, updated_at)
VALUES (
  106, 1,
  SHA2('v1|JS_ERROR|ReferenceError|inventorystock is not defined|cart.js:validatecheckout', 256),
  'ReferenceError: inventoryStock is not defined',
  'JS_ERROR', NULL, NULL, NULL, '/cart',
  'REOPENED', 'CRITICAL', 'Member 2', 5, 1, 1,
  DATE_SUB(NOW(3), INTERVAL 120 MINUTE), DATE_SUB(NOW(3), INTERVAL 5 MINUTE),
  DATE_SUB(NOW(3), INTERVAL 120 MINUTE), DATE_SUB(NOW(3), INTERVAL 5 MINUTE)
);

-- 4. Events (Timelines for Bugs 101, 102, 104)
INSERT INTO events (id, project_id, session_pk, event_id, type, action_label, page_path, http_method, http_status, endpoint, error_code, message, stack_top, occurred_at, bug_id)
VALUES
-- Session 1 -> Bug 101 timeline
(1, 1, 1, 'evt-seed-1001', 'PAGE_VIEW', NULL, '/', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 112 MINUTE), NULL),
(2, 1, 1, 'evt-seed-1002', 'CLICK', 'NAV_PRODUCTS', '/', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 111 MINUTE), NULL),
(3, 1, 1, 'evt-seed-1003', 'ROUTE_CHANGE', NULL, '/product/1', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 111 MINUTE), NULL),
(4, 1, 1, 'evt-seed-1004', 'JS_ERROR', NULL, '/product/1', NULL, NULL, NULL, NULL, 'Cannot read properties of undefined (reading ''price'')', 'productPage.js:renderPrice', DATE_SUB(NOW(3), INTERVAL 110 MINUTE), 101),

-- Session 1 -> Bug 104 timeline
(5, 1, 1, 'evt-seed-1005', 'PAGE_VIEW', NULL, '/cart', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 78 MINUTE), NULL),
(6, 1, 1, 'evt-seed-1006', 'CLICK', 'APPLY_COUPON', '/cart', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 76 MINUTE), NULL),
(7, 1, 1, 'evt-seed-1007', 'API_ERROR', NULL, '/cart', 'POST', 400, '/demo-api/cart/coupon', 'INVALID_COUPON_FORMAT', 'Invalid coupon format', NULL, DATE_SUB(NOW(3), INTERVAL 75 MINUTE), 104),

-- Session 2 -> Bug 102 timeline
(8, 1, 2, 'evt-seed-1008', 'PAGE_VIEW', NULL, '/', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 95 MINUTE), NULL),
(9, 1, 2, 'evt-seed-1009', 'ROUTE_CHANGE', NULL, '/product/999', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 92 MINUTE), NULL),
(10, 1, 2, 'evt-seed-1010', 'API_ERROR', NULL, '/product/999', 'GET', 404, '/demo-api/products/999', 'PRODUCT_NOT_FOUND', 'Product with ID 999 not found', NULL, DATE_SUB(NOW(3), INTERVAL 90 MINUTE), 102),

-- Session 2 -> Bug 105 timeline
(11, 1, 2, 'evt-seed-1011', 'ROUTE_CHANGE', NULL, '/wishlist', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 62 MINUTE), NULL),
(12, 1, 2, 'evt-seed-1012', 'CLICK', 'SYNC_WISHLIST', '/wishlist', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 61 MINUTE), NULL),
(13, 1, 2, 'evt-seed-1013', 'PROMISE_REJECTION', NULL, '/wishlist', NULL, NULL, NULL, NULL, 'Sync failed with status 503', 'wishlist.js:syncWishlist', DATE_SUB(NOW(3), INTERVAL 60 MINUTE), 105),

-- Additional occurrence events
(14, 1, 1, 'evt-seed-1014', 'CLICK', 'APPLY_COUPON', '/cart', NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(3), INTERVAL 11 MINUTE), NULL),
(15, 1, 1, 'evt-seed-1015', 'API_ERROR', NULL, '/cart', 'POST', 400, '/demo-api/cart/coupon', 'INVALID_COUPON_FORMAT', 'Invalid coupon format', NULL, DATE_SUB(NOW(3), INTERVAL 10 MINUTE), 104);

-- 5. Bug Occurrences
INSERT INTO bug_occurrences (id, bug_id, session_pk, trigger_event_pk, occurred_at, was_regression)
VALUES
(1, 101, 1, 4, DATE_SUB(NOW(3), INTERVAL 110 MINUTE), 0),
(2, 104, 1, 7, DATE_SUB(NOW(3), INTERVAL 75 MINUTE), 0),
(3, 102, 2, 10, DATE_SUB(NOW(3), INTERVAL 90 MINUTE), 0),
(4, 105, 2, 13, DATE_SUB(NOW(3), INTERVAL 60 MINUTE), 0),
(5, 104, 1, 15, DATE_SUB(NOW(3), INTERVAL 10 MINUTE), 0);

-- 6. Comments
INSERT INTO comments (id, bug_id, author, body, created_at, updated_at)
VALUES
(1, 101, 'Aarav', 'Product price rendering logic needs optional chaining: item?.price.', DATE_SUB(NOW(3), INTERVAL 80 MINUTE), DATE_SUB(NOW(3), INTERVAL 80 MINUTE)),
(2, 103, 'Aarav', 'Resolved: Analytics heartbeat endpoint had rate limiting enabled by mistake.', DATE_SUB(NOW(3), INTERVAL 30 MINUTE), DATE_SUB(NOW(3), INTERVAL 30 MINUTE)),
(3, 104, 'Member 1', 'Coupon input regex rejects lowercase characters. Need to normalize uppercase before dispatching.', DATE_SUB(NOW(3), INTERVAL 45 MINUTE), DATE_SUB(NOW(3), INTERVAL 45 MINUTE)),
(4, 105, 'Member 1', 'Investigating third-party wishlist sync service downtime.', DATE_SUB(NOW(3), INTERVAL 15 MINUTE), DATE_SUB(NOW(3), INTERVAL 15 MINUTE)),
(5, 106, 'Member 2', 'Regression occurred after merging the cart checkout refactor branch.', DATE_SUB(NOW(3), INTERVAL 4 MINUTE), DATE_SUB(NOW(3), INTERVAL 4 MINUTE));

-- 7. Ensure next inserted bug starts at >= 200 (live demo creates bug 200+)
ALTER TABLE bugs AUTO_INCREMENT = 200;

-- Reset actor to default API for pooled connections
SET @bugdna_actor = 'API';

