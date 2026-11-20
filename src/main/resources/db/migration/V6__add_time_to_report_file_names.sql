-- Report file names get the submission time (several tests a day share a date):
-- JavaTestAgent_20.11.2026_full.md -> JavaTestAgent_20.11.2026_14-05-33_full.md.
-- The prompt (.md) names its answers file, so that reference is renamed too.
WITH stamps AS (
    SELECT id AS attempt_id,
           to_char(finished_at AT TIME ZONE time_zone, 'DD.MM.YYYY') AS day,
           to_char(finished_at AT TIME ZONE time_zone, 'DD.MM.YYYY_HH24-MI-SS') AS stamp
    FROM test_attempts
)
UPDATE test_attempt_files f
SET file_name = replace(f.file_name, '_' || s.day || '_', '_' || s.stamp || '_'),
    content = CASE
        WHEN f.format = 'MD' THEN replace(f.content,
                'JavaTestResult_' || s.day || '_' || lower(f.variant) || '.json',
                'JavaTestResult_' || s.stamp || '_' || lower(f.variant) || '.json')
        ELSE f.content
    END
FROM stamps s
WHERE s.attempt_id = f.attempt_id
  AND f.file_name = 'JavaTest' || CASE f.format WHEN 'MD' THEN 'Agent' ELSE 'Result' END
                    || '_' || s.day || '_' || lower(f.variant) || '.' || lower(f.format);
