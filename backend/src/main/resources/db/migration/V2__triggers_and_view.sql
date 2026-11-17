-- ===== bugs =====
CREATE TRIGGER trg_bugs_ai AFTER INSERT ON bugs FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, new_values, source)
  VALUES ('BUG', NEW.id, NEW.id, 'INSERT',
          CONCAT('Bug #', NEW.id, ' created: ', LEFT(NEW.title, 150)),
          JSON_OBJECT('status', NEW.status, 'priority', NEW.priority, 'title', NEW.title),
          COALESCE(@bugdna_actor, 'MYSQL_CLI'));

-- logs only meaningful changes (status/priority/assignee/title/regression), not every occurrence bump
CREATE TRIGGER trg_bugs_au AFTER UPDATE ON bugs FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, old_values, new_values, source)
  SELECT 'BUG', NEW.id, NEW.id, 'UPDATE',
         LEFT(CONCAT('Bug #', NEW.id, ': ', CONCAT_WS('; ',
           IF(OLD.status   <=> NEW.status,   NULL, CONCAT('status ', OLD.status, ' -> ', NEW.status)),
           IF(OLD.priority <=> NEW.priority, NULL, CONCAT('priority ', OLD.priority, ' -> ', NEW.priority)),
           IF(OLD.assignee <=> NEW.assignee, NULL, CONCAT('assignee ', IFNULL(OLD.assignee,'none'), ' -> ', IFNULL(NEW.assignee,'none'))),
           IF(OLD.title    <=> NEW.title,    NULL, 'title edited'),
           IF(OLD.is_regression <=> NEW.is_regression OR NEW.is_regression = 0, NULL, 'REGRESSION detected'))), 255),
         JSON_OBJECT('status', OLD.status, 'priority', OLD.priority, 'assignee', OLD.assignee, 'title', OLD.title),
         JSON_OBJECT('status', NEW.status, 'priority', NEW.priority, 'assignee', NEW.assignee, 'title', NEW.title),
         COALESCE(@bugdna_actor, 'MYSQL_CLI')
  FROM DUAL
  WHERE NOT (OLD.status <=> NEW.status AND OLD.priority <=> NEW.priority
             AND OLD.assignee <=> NEW.assignee AND OLD.title <=> NEW.title
             AND OLD.is_regression <=> NEW.is_regression);

CREATE TRIGGER trg_bugs_ad AFTER DELETE ON bugs FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, old_values, source)
  VALUES ('BUG', OLD.id, OLD.id, 'DELETE',
          CONCAT('Bug #', OLD.id, ' deleted: ', LEFT(OLD.title, 150)),
          JSON_OBJECT('status', OLD.status, 'title', OLD.title),
          COALESCE(@bugdna_actor, 'MYSQL_CLI'));

-- ===== comments =====
CREATE TRIGGER trg_comments_ai AFTER INSERT ON comments FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, new_values, source)
  VALUES ('COMMENT', NEW.id, NEW.bug_id, 'INSERT',
          CONCAT(NEW.author, ' commented on bug #', NEW.bug_id, ': ', LEFT(NEW.body, 120)),
          JSON_OBJECT('author', NEW.author, 'body', LEFT(NEW.body, 500)),
          COALESCE(@bugdna_actor, 'MYSQL_CLI'));

CREATE TRIGGER trg_comments_au AFTER UPDATE ON comments FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, old_values, new_values, source)
  SELECT 'COMMENT', NEW.id, NEW.bug_id, 'UPDATE',
         CONCAT('Comment #', NEW.id, ' on bug #', NEW.bug_id, ' edited'),
         JSON_OBJECT('body', LEFT(OLD.body, 500)), JSON_OBJECT('body', LEFT(NEW.body, 500)),
         COALESCE(@bugdna_actor, 'MYSQL_CLI')
  FROM DUAL WHERE NOT (OLD.body <=> NEW.body AND OLD.author <=> NEW.author);

CREATE TRIGGER trg_comments_ad AFTER DELETE ON comments FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, old_values, source)
  VALUES ('COMMENT', OLD.id, OLD.bug_id, 'DELETE',
          CONCAT('Comment #', OLD.id, ' on bug #', OLD.bug_id, ' deleted'),
          JSON_OBJECT('author', OLD.author, 'body', LEFT(OLD.body, 500)),
          COALESCE(@bugdna_actor, 'MYSQL_CLI'));

-- ===== analytics view =====
CREATE OR REPLACE VIEW bug_summary AS
SELECT b.id, b.project_id, b.title, b.error_type, b.status, b.priority, b.assignee,
       b.occurrence_count, b.is_regression, b.regression_count,
       b.first_seen_at, b.last_seen_at, b.resolved_at,
       COUNT(DISTINCT o.session_pk) AS sessions_affected,
       COUNT(DISTINCT c.id)         AS comment_count,
       TIMESTAMPDIFF(MINUTE, b.first_seen_at, COALESCE(b.resolved_at, NOW(3))) AS minutes_open
FROM bugs b
LEFT JOIN bug_occurrences o ON o.bug_id = b.id
LEFT JOIN comments c        ON c.bug_id = b.id
GROUP BY b.id;

