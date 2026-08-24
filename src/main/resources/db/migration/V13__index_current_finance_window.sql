create index if not exists idx_logistics_revenue_event_created_at
    on logistics_revenue_event(created_at desc);

create index if not exists idx_logistics_cost_event_created_at
    on logistics_cost_event(created_at desc);
