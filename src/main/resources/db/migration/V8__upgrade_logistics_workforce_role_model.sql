alter table logistics_workforce_allocation
    add column if not exists target_service varchar(100);

alter table logistics_workforce_allocation
    add column if not exists role_type varchar(80);

alter table logistics_workforce_allocation
    add column if not exists allocated_headcount integer;

alter table logistics_workforce_allocation
    add column if not exists capacity_per_person_per_day integer;

alter table logistics_workforce_allocation
    add column if not exists productivity_score numeric(7,4);

alter table logistics_workforce_allocation
    add column if not exists wage_per_day bigint;

alter table logistics_workforce_allocation
    add column if not exists effective_capacity bigint;

alter table logistics_workforce_allocation
    add column if not exists used_capacity bigint;

alter table logistics_workforce_allocation
    add column if not exists remaining_capacity bigint;

alter table logistics_workforce_allocation
    add column if not exists status varchar(50);

alter table logistics_workforce_allocation
    add column if not exists updated_at timestamp;

alter table logistics_workforce_allocation
    alter column dispatchers drop not null,
    alter column drivers drop not null,
    alter column delay_responders drop not null,
    alter column synthetic_daily_labor_cost drop not null;

create unique index if not exists uq_workforce_workday_role
    on logistics_workforce_allocation(workday_id, role_type)
    where workday_id is not null and role_type is not null;

create table if not exists logistics_workday_result (
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
    total_capacity bigint not null,
    used_capacity bigint not null,
    remaining_capacity bigint not null,
    shipments_requested bigint not null,
    shipments_dispatched bigint not null,
    shipments_delayed bigint not null,
    route_plans_created bigint not null,
    delivery_completed bigint not null,
    processed_events bigint not null,
    backlog_count bigint not null,
    shortage_events bigint not null,
    delayed_response_load bigint not null,
    productivity_rate numeric(7,4) not null,
    utilization_rate numeric(7,4) not null,
    payroll_cost bigint not null,
    status varchar(50) not null,
    bottleneck_role varchar(50) not null,
    created_at timestamp not null
);

create index if not exists idx_workday_result_status
    on logistics_workday_result(status, work_date);
