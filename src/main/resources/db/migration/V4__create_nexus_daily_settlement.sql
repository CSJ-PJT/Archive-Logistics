create table nexus_daily_settlement (
    id bigserial primary key,
    settlement_id varchar(100) unique not null,
    idempotency_key varchar(200) unique not null,
    source varchar(100) not null,
    settlement_date date not null,
    factory_id varchar(50) not null,
    currency varchar(10) not null default 'KRW',
    total_shipments integer not null,
    delayed_shipments integer not null,
    held_shipments integer not null,
    total_quantity bigint not null,
    total_logistics_cost bigint not null,
    manufacturing_impact_cost bigint not null,
    manufacturing_share_rate numeric(5,4) not null,
    on_time_rate numeric(5,4) not null,
    status varchar(30) not null,
    retry_count integer not null default 0,
    last_error text null,
    nexus_response jsonb null,
    next_retry_at timestamp null,
    sent_at timestamp null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uq_nexus_daily_settlement_date_factory unique (settlement_date, factory_id)
);

create index idx_nexus_daily_settlement_status_date
    on nexus_daily_settlement(status, settlement_date);

create index idx_nexus_daily_settlement_factory_date
    on nexus_daily_settlement(factory_id, settlement_date);
