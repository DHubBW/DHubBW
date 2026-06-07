-- V3: Migrate file data from BYTEA to PostgreSQL Large Objects.
-- Large Objects are stored in pg_largeobject and referenced by OID.
-- This allows streaming and avoids loading entire files into heap memory.

-- Step 1: Add OID column for Large Object reference
ALTER TABLE datei ADD COLUMN data_oid OID;

-- Step 2: Migrate existing BYTEA data into Large Objects
DO $$
DECLARE
    rec RECORD;
    loid OID;
BEGIN
    FOR rec IN SELECT datei_id, data FROM datei WHERE data IS NOT NULL
    LOOP
        loid := lo_from_bytea(0, rec.data);
        UPDATE datei SET data_oid = loid WHERE datei_id = rec.datei_id;
    END LOOP;
END
$$;

-- Step 3: Clear BYTEA data to free space (column kept for entity compat)
UPDATE datei SET data = NULL WHERE data_oid IS NOT NULL;
