# BugDNA — Database Entity-Relationship Diagram

BugDNA uses **8 relational tables** (7 core domain tables + `activity_log` for two-way live feed tracking) plus the **`bug_summary` analytical view**, managed exclusively via Flyway migrations (`V1__schema.sql`, `V2__triggers_and_view.sql`, `V3__seed.sql`) with `ddl-auto=validate`.

```mermaid
erDiagram
    PROJECTS ||--o{ TRACKING_SESSIONS : "has"
    PROJECTS ||--o{ BUGS : "owns"
    PROJECTS ||--o{ EVENTS : "collects"
    TRACKING_SESSIONS ||--o{ EVENTS : "emits"
    TRACKING_SESSIONS ||--o{ BUG_OCCURRENCES : "affected_by"
    BUGS ||--o{ BUG_OCCURRENCES : "grouped_into"
    BUGS ||--o{ EVENTS : "linked_trigger"
    BUGS ||--o{ COMMENTS : "has"
    BUGS ||--o{ AI_ANALYSES : "explained_by"
    EVENTS ||--o| BUG_OCCURRENCES : "triggers"

    PROJECTS {
        bigint id PK
        varchar name
        varchar project_key UK
        varchar allowed_origin
        datetime created_at
    }

    TRACKING_SESSIONS {
        bigint id PK
        bigint project_id FK
        varchar session_id UK
        varchar user_agent
        datetime first_seen_at
        datetime last_seen_at
    }

    BUGS {
        bigint id PK
        bigint project_id FK
        char_64 fingerprint UK
        varchar title
        varchar error_type
        varchar error_code
        varchar http_method
        varchar endpoint
        varchar page_path
        enum status
        enum priority
        varchar assignee
        int occurrence_count
        tinyint is_regression
        int regression_count
        datetime first_seen_at
        datetime last_seen_at
        datetime resolved_at
        datetime created_at
        datetime updated_at
    }

    EVENTS {
        bigint id PK
        bigint project_id FK
        bigint session_pk FK
        varchar event_id UK
        enum type
        varchar action_label
        varchar page_path
        varchar http_method
        smallint http_status
        varchar endpoint
        varchar error_code
        varchar message
        varchar stack_top
        datetime occurred_at
        datetime received_at
        bigint bug_id FK
    }

    BUG_OCCURRENCES {
        bigint id PK
        bigint bug_id FK
        bigint session_pk FK
        bigint trigger_event_pk FK_UK
        datetime occurred_at
        tinyint was_regression
    }

    COMMENTS {
        bigint id PK
        bigint bug_id FK
        varchar author
        text body
        datetime created_at
        datetime updated_at
    }

    AI_ANALYSES {
        bigint id PK
        bigint bug_id FK
        varchar provider
        varchar model
        enum status
        text summary
        json evidence_json
        json hypotheses_json
        json debug_steps_json
        json repro_steps_json
        char_64 input_hash
        varchar error_message
        datetime created_at
    }

    ACTIVITY_LOG {
        bigint id PK
        enum entity_type
        bigint entity_id
        bigint bug_id
        enum action
        varchar summary
        json old_values
        json new_values
        varchar source
        datetime created_at
    }
```

## Triggers & Analytical View (`V2__triggers_and_view.sql`)

- **6 Triggers** (`trg_bugs_ai`, `trg_bugs_au`, `trg_bugs_ad`, `trg_comments_ai`, `trg_comments_au`, `trg_comments_ad`): automatically append audit rows to `activity_log` with `source = COALESCE(@bugdna_actor, 'MYSQL_CLI')`.
- **`VIEW bug_summary`**: aggregates `bugs`, `COUNT(DISTINCT bug_occurrences.session_pk) AS sessions_affected`, `COUNT(DISTINCT comments.id) AS comment_count`, and `TIMESTAMPDIFF(MINUTE, first_seen_at, COALESCE(resolved_at, NOW(3))) AS minutes_open` for the Analytics Overview and Bug Detail metrics.

