create table nexus_logistics_event (
    id bigserial primary key,
    event_id varchar(100) unique not null,
    idempotency_key varchar(200) unique not null,
    source varchar(100) not null,
    event_type varchar(100) not null,
    schema_version integer not null default 1,
    payload jsonb not null,
    status varchar(30) not null,
    received_at timestamp not null,
    processed_at timestamp null,
    failure_reason text null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table route_plan (
    id bigserial primary key,
    route_plan_id varchar(100) unique not null,
    source_event_id varchar(100) unique not null,
    shipment_id varchar(100) not null,
    factory_id varchar(50) not null,
    origin_code varchar(50) not null,
    destination_code varchar(50) not null,
    vendor_id varchar(100) not null,
    distance_km numeric(10,2) not null,
    estimated_minutes integer not null,
    priority varchar(30) not null,
    risk_score numeric(5,4) not null,
    delayed boolean not null,
    deviated boolean not null,
    requires_cold_chain boolean not null,
    route_status varchar(30) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table route_cost (
    id bigserial primary key,
    route_plan_id varchar(100) unique not null,
    fuel_cost bigint not null,
    toll_cost bigint not null,
    urgent_surcharge bigint not null,
    delay_penalty bigint not null,
    cold_chain_penalty bigint not null,
    total_cost bigint not null,
    currency varchar(10) not null default 'KRW',
    requires_approval boolean not null,
    reason text not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table logistics_outbox_event (
    id bigserial primary key,
    event_id varchar(100) unique not null,
    idempotency_key varchar(200) unique not null,
    source varchar(100) not null,
    event_type varchar(100) not null,
    aggregate_type varchar(100) not null,
    aggregate_id varchar(100) not null,
    payload jsonb not null,
    status varchar(30) not null,
    retry_count integer not null default 0,
    last_error text null,
    next_retry_at timestamp null,
    created_at timestamp not null,
    published_at timestamp null,
    updated_at timestamp not null
);

create table ledger_publish_attempt (
    id bigserial primary key,
    batch_id varchar(100) not null,
    event_count integer not null,
    success_count integer not null,
    failure_count integer not null,
    ledger_enabled boolean not null,
    ledger_endpoint varchar(500) null,
    contract_mode varchar(100) not null,
    result_status varchar(30) not null,
    error_message text null,
    started_at timestamp not null,
    completed_at timestamp not null
);

create table audit_log (
    id bigserial primary key,
    trace_id varchar(100) null,
    actor varchar(100) not null,
    action varchar(100) not null,
    target_type varchar(100) not null,
    target_id varchar(100) not null,
    before_status varchar(50) null,
    after_status varchar(50) null,
    detail jsonb null,
    created_at timestamp not null
);
