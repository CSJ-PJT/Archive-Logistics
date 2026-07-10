alter table route_plan
    add column if not exists order_id varchar(100);

alter table route_plan
    add column if not exists customer_id varchar(100);

alter table route_plan
    add column if not exists customer_type varchar(100);

alter table route_plan
    add column if not exists product_type varchar(100);

alter table route_plan
    add column if not exists order_amount bigint;

alter table route_plan
    add column if not exists total_amount bigint;

alter table route_plan
    add column if not exists market_priority varchar(30);

alter table route_plan
    add column if not exists risk_level integer;

alter table route_plan
    add column if not exists express_order boolean;

alter table route_plan
    add column if not exists vip_customer boolean;

alter table route_plan
    add column if not exists correlation_id varchar(100);

alter table route_plan
    add column if not exists causation_id varchar(100);

alter table route_plan
    add column if not exists simulation_run_id varchar(100);

alter table route_plan
    add column if not exists settlement_cycle_id varchar(100);

alter table route_plan
    add column if not exists hop_count integer;

alter table route_plan
    add column if not exists max_hop integer;
