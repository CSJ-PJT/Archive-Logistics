create table if not exists archiveos_runtime_delivery (
    id bigserial primary key,
    event_id varchar(200) not null unique,
    idempotency_key varchar(240) not null,
    correlation_id varchar(200),
    causation_id varchar(200),
    order_id varchar(200),
    simulation_run_id varchar(200),
    entity_type varchar(100) not null,
    entity_id varchar(200) not null,
    event_type varchar(160) not null,
    source_system varchar(100) not null,
    payload_json jsonb not null,
    status varchar(40) not null,
    retry_count integer not null default 0,
    next_retry_at timestamp null,
    last_error_code varchar(80) null,
    last_error_message text null,
    created_at timestamp not null,
    published_at timestamp null,
    updated_at timestamp not null
);

create index if not exists idx_archiveos_runtime_delivery_status_retry
    on archiveos_runtime_delivery(status, next_retry_at, created_at);
create index if not exists idx_archiveos_runtime_delivery_correlation
    on archiveos_runtime_delivery(correlation_id, created_at);
