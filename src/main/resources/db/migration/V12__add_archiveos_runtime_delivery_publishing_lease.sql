alter table archiveos_runtime_delivery add column if not exists publishing_started_at timestamp null;
create index if not exists idx_archiveos_runtime_delivery_stale_publishing
    on archiveos_runtime_delivery(status, publishing_started_at, updated_at)
    where published_at is null;
