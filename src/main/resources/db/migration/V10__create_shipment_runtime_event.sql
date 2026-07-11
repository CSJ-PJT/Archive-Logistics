create table if not exists shipment_runtime_event (
    id bigserial primary key,
    event_id varchar(160) unique not null,
    idempotency_key varchar(220) unique not null,
    route_plan_id varchar(100) not null,
    shipment_id varchar(100) not null,
    source_service varchar(100) not null,
    target_service varchar(100) not null,
    event_type varchar(100) not null,
    status varchar(40) not null,
    severity varchar(20) not null,
    correlation_id varchar(100),
    causation_id varchar(100),
    simulation_run_id varchar(100),
    settlement_cycle_id varchar(100),
    workday_id varchar(100),
    hop_count integer not null,
    max_hop integer not null,
    metadata jsonb,
    occurred_at timestamp not null,
    created_at timestamp not null,
    unique (route_plan_id, event_type)
);

create index if not exists idx_shipment_runtime_event_occurred_at
    on shipment_runtime_event(occurred_at desc);
create index if not exists idx_shipment_runtime_event_correlation_id
    on shipment_runtime_event(correlation_id, occurred_at desc);
create index if not exists idx_shipment_runtime_event_shipment_id
    on shipment_runtime_event(shipment_id, occurred_at desc);
