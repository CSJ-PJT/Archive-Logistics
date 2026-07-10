create table if not exists logistics_workforce_allocation (
    id bigserial primary key,
    allocation_id varchar(100) unique not null,
    source_service varchar(100) not null,
    workday_id varchar(100),
    work_date date not null,
    dispatchers integer not null,
    drivers integer not null,
    delay_responders integer not null,
    synthetic_daily_labor_cost bigint not null,
    simulation_run_id varchar(100),
    settlement_cycle_id varchar(100),
    correlation_id varchar(100),
    causation_id varchar(100),
    hop_count integer not null,
    max_hop integer not null,
    reason varchar(500),
    created_at timestamp not null
);

create table if not exists logistics_workday_productivity (
    id bigserial primary key,
    workday_id varchar(100) unique not null,
    allocation_id varchar(100),
    work_date date unique not null,
    workforce_enabled boolean not null,
    baseline_capacity boolean not null,
    dispatchers integer not null,
    drivers integer not null,
    delay_responders integer not null,
    workload_events bigint not null,
    capacity_events bigint not null,
    processed_events bigint not null,
    backlog_events bigint not null,
    shortage_events bigint not null,
    delayed_response_load bigint not null,
    productivity_rate numeric(7,4) not null,
    utilization_rate numeric(7,4) not null,
    synthetic_labor_cost bigint not null,
    status varchar(50) not null,
    bottleneck_type varchar(50) not null,
    created_at timestamp not null
);

create index if not exists idx_workforce_allocation_work_date
    on logistics_workforce_allocation(work_date, created_at);

create index if not exists idx_workday_productivity_work_date
    on logistics_workday_productivity(work_date);

create index if not exists idx_workday_productivity_status
    on logistics_workday_productivity(status, work_date);
