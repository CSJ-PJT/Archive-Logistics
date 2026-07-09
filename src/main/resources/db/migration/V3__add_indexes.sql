create index idx_nexus_logistics_event_event_type_received_at
    on nexus_logistics_event(event_type, received_at);

create index idx_nexus_logistics_event_status_received_at
    on nexus_logistics_event(status, received_at);

create index idx_route_plan_factory_created_at
    on route_plan(factory_id, created_at);

create index idx_route_plan_route_status_created_at
    on route_plan(route_status, created_at);

create index idx_logistics_outbox_event_status_created_at
    on logistics_outbox_event(status, created_at);

create index idx_logistics_outbox_event_next_retry_at
    on logistics_outbox_event(next_retry_at);

create index idx_ledger_publish_attempt_started_at
    on ledger_publish_attempt(started_at);

create index idx_audit_log_target_type_target_id
    on audit_log(target_type, target_id);

create index idx_audit_log_created_at
    on audit_log(created_at);
