CREATE TABLE projects (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  name           VARCHAR(120) NOT NULL,
  project_key    VARCHAR(64)  NOT NULL UNIQUE,         -- sent by tracker in X-BugDNA-Key
  allowed_origin VARCHAR(255) NULL,
  created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

CREATE TABLE tracking_sessions (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id    BIGINT NOT NULL,
  session_id    VARCHAR(64) NOT NULL,                  -- random id from tracker, not a user id
  user_agent    VARCHAR(255) NULL,
  first_seen_at DATETIME(3) NOT NULL,
  last_seen_at  DATETIME(3) NOT NULL,
  UNIQUE KEY uk_session (project_id, session_id),
  CONSTRAINT fk_sess_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE bugs (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id       BIGINT NOT NULL,
  fingerprint      CHAR(64) NOT NULL,                  -- SHA-256 hex
  title            VARCHAR(200) NOT NULL,
  error_type       VARCHAR(40)  NOT NULL,              -- JS_ERROR | PROMISE_REJECTION | API_ERROR | NETWORK_ERROR
  error_code       VARCHAR(80)  NULL,
  http_method      VARCHAR(10)  NULL,
  endpoint         VARCHAR(255) NULL,
  page_path        VARCHAR(255) NULL,
  status           ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED','REOPENED','IGNORED') NOT NULL DEFAULT 'OPEN',
  priority         ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  assignee         VARCHAR(80)  NULL,
  occurrence_count INT NOT NULL DEFAULT 1,
  is_regression    TINYINT(1) NOT NULL DEFAULT 0,
  regression_count INT NOT NULL DEFAULT 0,
  first_seen_at    DATETIME(3) NOT NULL,
  last_seen_at     DATETIME(3) NOT NULL,
  resolved_at      DATETIME(3) NULL,
  created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_bug_fingerprint (project_id, fingerprint),   -- the grouping guarantee
  KEY idx_bugs_status (status),
  KEY idx_bugs_updated (updated_at),
  CONSTRAINT fk_bug_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE events (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id   BIGINT NOT NULL,
  session_pk   BIGINT NOT NULL,
  event_id     VARCHAR(64) NOT NULL,                   -- client UUID, used for idempotency
  type         ENUM('PAGE_VIEW','ROUTE_CHANGE','CLICK','JS_ERROR','PROMISE_REJECTION','API_ERROR','NETWORK_ERROR','CUSTOM') NOT NULL,
  action_label VARCHAR(80)  NULL,                      -- from data-bugdna-action only
  page_path    VARCHAR(255) NULL,                      -- no query string
  http_method  VARCHAR(10)  NULL,
  http_status  SMALLINT     NULL,
  endpoint     VARCHAR(255) NULL,                      -- normalized, no query string
  error_code   VARCHAR(80)  NULL,
  message      VARCHAR(500) NULL,                      -- sanitized
  stack_top    VARCHAR(500) NULL,                      -- first frames, sanitized
  occurred_at  DATETIME(3) NOT NULL,
  received_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  bug_id       BIGINT NULL,
  UNIQUE KEY uk_event (project_id, event_id),          -- duplicate batches are ignored
  KEY idx_events_session_time (session_pk, occurred_at),
  KEY idx_events_bug (bug_id),
  CONSTRAINT fk_evt_project FOREIGN KEY (project_id) REFERENCES projects(id),
  CONSTRAINT fk_evt_session FOREIGN KEY (session_pk) REFERENCES tracking_sessions(id),
  CONSTRAINT fk_evt_bug     FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE SET NULL
);

CREATE TABLE bug_occurrences (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  bug_id           BIGINT NOT NULL,
  session_pk       BIGINT NOT NULL,
  trigger_event_pk BIGINT NOT NULL,
  occurred_at      DATETIME(3) NOT NULL,
  was_regression   TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_occ_event (trigger_event_pk),
  KEY idx_occ_bug_time (bug_id, occurred_at),
  CONSTRAINT fk_occ_bug     FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE,
  CONSTRAINT fk_occ_session FOREIGN KEY (session_pk) REFERENCES tracking_sessions(id),
  CONSTRAINT fk_occ_event   FOREIGN KEY (trigger_event_pk) REFERENCES events(id) ON DELETE CASCADE
);

CREATE TABLE comments (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  bug_id     BIGINT NOT NULL,
  author     VARCHAR(80) NOT NULL,
  body       TEXT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_comments_bug (bug_id),
  CONSTRAINT fk_comment_bug FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE
);

CREATE TABLE ai_analyses (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  bug_id          BIGINT NOT NULL,
  provider        VARCHAR(20) NOT NULL,               -- gemini | openai | mock
  model           VARCHAR(80) NOT NULL,
  status          ENUM('OK','FAILED','MOCK') NOT NULL,
  summary         TEXT NULL,
  evidence_json   JSON NULL,                          -- [{statement, eventIds[]}] — facts only
  hypotheses_json JSON NULL,                          -- [{cause, confidence, reasoning}] — guesses, labeled
  debug_steps_json JSON NULL,
  repro_steps_json JSON NULL,
  input_hash      CHAR(64) NOT NULL,                  -- cache key of the sanitized prompt input
  error_message   VARCHAR(500) NULL,
  created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_ai_bug (bug_id, created_at),
  CONSTRAINT fk_ai_bug FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE
);

-- [REFINEMENT] live activity feed; no FKs so deleted rows keep their history
CREATE TABLE activity_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  entity_type ENUM('BUG','COMMENT') NOT NULL,
  entity_id   BIGINT NOT NULL,
  bug_id      BIGINT NULL,
  action      ENUM('INSERT','UPDATE','DELETE') NOT NULL,
  summary     VARCHAR(255) NOT NULL,
  old_values  JSON NULL,
  new_values  JSON NULL,
  source      VARCHAR(20) NOT NULL,                   -- API | TRACKER | MYSQL_CLI
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_activity_created (created_at)
);

