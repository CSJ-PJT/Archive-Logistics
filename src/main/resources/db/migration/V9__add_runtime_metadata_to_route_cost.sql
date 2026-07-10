alter table route_cost
    add column if not exists order_id varchar(100);

alter table route_cost
    add column if not exists correlation_id varchar(100);

alter table route_cost
    add column if not exists simulation_run_id varchar(100);

alter table route_cost
    add column if not exists settlement_cycle_id varchar(100);

create index if not exists idx_route_cost_correlation_id
    on route_cost(correlation_id);
