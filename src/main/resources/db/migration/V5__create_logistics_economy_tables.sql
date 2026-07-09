create table if not exists logistics_revenue_event (
    id bigserial primary key,
    event_id varchar(100) unique not null,
    idempotency_key varchar(200) unique not null,
    simulation_run_id varchar(100),
    settlement_cycle_id varchar(100),
    source_service varchar(100) not null,
    billed_to_service varchar(100) not null,
    revenue_type varchar(80) not null,
    base_amount bigint not null,
    revenue_amount bigint not null,
    currency varchar(10) not null,
    reason text not null,
    created_at timestamp not null
);

create table if not exists logistics_cost_event (
    id bigserial primary key,
    event_id varchar(100) unique not null,
    idempotency_key varchar(200) unique not null,
    simulation_run_id varchar(100),
    settlement_cycle_id varchar(100),
    source_service varchar(100) not null,
    paid_to_service varchar(100) not null,
    cost_type varchar(80) not null,
    cost_amount bigint not null,
    currency varchar(10) not null,
    reason text not null,
    created_at timestamp not null
);

create table if not exists logistics_daily_settlement (
    id bigserial primary key,
    settlement_id varchar(100) unique not null,
    settlement_cycle_id varchar(100) not null,
    settled_at date not null,
    billed_to_service varchar(100) not null,
    factory_id varchar(50) not null,
    route_count bigint not null,
    total_delivery_fee bigint not null,
    total_surcharge bigint not null,
    total_cost bigint not null,
    ledger_fee_paid bigint not null,
    net_profit bigint not null,
    status varchar(20) not null,
    created_at timestamp not null,
    completed_at timestamp
);

create table if not exists logistics_profit_snapshot (
    snapshot_id varchar(100) primary key,
    settlement_date date not null,
    revenue_amount bigint not null,
    cost_amount bigint not null,
    profit_amount bigint not null,
    cash_balance bigint not null,
    bankruptcy_risk varchar(20) not null,
    created_at timestamp not null
);

create index if not exists idx_logistics_revenue_event_settlement_cycle
    on logistics_revenue_event(settlement_cycle_id);

create index if not exists idx_logistics_revenue_event_type
    on logistics_revenue_event(revenue_type, created_at);

create index if not exists idx_logistics_cost_event_settlement_cycle
    on logistics_cost_event(settlement_cycle_id);

create index if not exists idx_logistics_cost_event_type
    on logistics_cost_event(cost_type, created_at);

create index if not exists idx_logistics_daily_settlement_date_factory
    on logistics_daily_settlement(settled_at, factory_id);

create index if not exists idx_logistics_profit_snapshot_date
    on logistics_profit_snapshot(settlement_date, created_at);
