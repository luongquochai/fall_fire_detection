CREATE MATERIALIZED VIEW IF NOT EXISTS fall_fire.events_by_message AS
    SELECT *
    FROM fall_fire.events
    WHERE message_id IS NOT NULL
    PRIMARY KEY (message_id, user_id, device_id, event_date); 