-- Legacy @Version rows and their outbox events started at zero; shift both sides
-- together so existing event ordering remains intact under the one-based contract.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM products
        WHERE version < 0 OR version > 2147483646
    ) THEN
        RAISE EXCEPTION 'Cannot migrate product versions outside the supported legacy range 0..2147483646';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM outbox
        WHERE jsonb_typeof(payload::jsonb) IS DISTINCT FROM 'object'
           OR jsonb_typeof(payload::jsonb -> 'version') IS DISTINCT FROM 'number'
           OR (payload::jsonb ->> 'version') !~ '^(0|[1-9][0-9]*)$'
           OR CASE
               WHEN (payload::jsonb ->> 'version') ~ '^(0|[1-9][0-9]*)$'
               THEN (payload::jsonb ->> 'version')::numeric > 2147483646
               ELSE FALSE
           END
    ) THEN
        RAISE EXCEPTION 'Cannot migrate outbox rows with missing, non-integer, or out-of-range version values; repair those rows before retrying';
    END IF;
END;
$$;

UPDATE outbox
SET payload = jsonb_set(
    payload::jsonb,
    '{version}',
    to_jsonb((payload::jsonb ->> 'version')::numeric + 1),
    false
)::text;

UPDATE products
SET version = version + 1;

ALTER TABLE products
    ALTER COLUMN version TYPE INTEGER USING version::INTEGER,
    ALTER COLUMN version SET DEFAULT 1;

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS ck_products_version_positive;

ALTER TABLE products
    ADD CONSTRAINT ck_products_version_positive CHECK (version >= 1 AND version <= 2147483647);

ALTER TABLE outbox
    ALTER COLUMN event_id SET NOT NULL,
    ALTER COLUMN aggregate_id SET NOT NULL,
    ALTER COLUMN event_type SET NOT NULL,
    ALTER COLUMN payload SET NOT NULL,
    ALTER COLUMN schema_version SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN processed SET NOT NULL;

ALTER TABLE outbox
    DROP CONSTRAINT IF EXISTS outbox_schema_version_check;

ALTER TABLE outbox
    ADD CONSTRAINT ck_outbox_schema_version_positive CHECK (schema_version >= 1);

ALTER TABLE outbox
    DROP CONSTRAINT IF EXISTS outbox_payload_check;

ALTER TABLE outbox
    DROP CONSTRAINT IF EXISTS ck_outbox_payload_valid_version;

ALTER TABLE outbox
    ADD CONSTRAINT ck_outbox_payload_valid_version CHECK (
        payload <> ''
        AND jsonb_typeof(payload::jsonb) = 'object'
        AND jsonb_typeof(payload::jsonb -> 'version') = 'number'
        AND COALESCE((payload::jsonb ->> 'version') ~ '^[1-9][0-9]*$', FALSE)
        AND CASE
            WHEN (payload::jsonb ->> 'version') ~ '^[1-9][0-9]*$'
            THEN (payload::jsonb ->> 'version')::numeric <= 2147483647
            ELSE FALSE
        END
    );
