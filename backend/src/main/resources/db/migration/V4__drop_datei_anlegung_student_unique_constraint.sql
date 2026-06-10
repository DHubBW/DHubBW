-- V4: Drop unique constraint on datei_anlegung.student_id.
-- @OneToOne on the student field caused Hibernate to generate a UNIQUE
-- constraint on that column, preventing a student from uploading more
-- than one file. The annotation was changed to @ManyToOne (MR !124),
-- but the physical constraint survives on the production database until
-- explicitly dropped here.
DO $$
DECLARE
    v_constraint TEXT;
BEGIN
    SELECT c.conname
    INTO v_constraint
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_attribute a
        ON a.attrelid = t.oid
        AND a.attnum = ANY(c.conkey)
    WHERE t.relname  = 'datei_anlegung'
      AND a.attname  = 'student_id'
      AND c.contype  = 'u'
    LIMIT 1;

    IF v_constraint IS NOT NULL THEN
        EXECUTE 'ALTER TABLE datei_anlegung DROP CONSTRAINT '
            || quote_ident(v_constraint);
    END IF;
END $$;
