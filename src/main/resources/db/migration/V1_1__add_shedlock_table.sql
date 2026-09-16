-- Held by the instance that is running the end of lease job, so that one instance runs it at a time and a computer is
-- not reported to SysMan once per instance. Written by ShedLock, never by us.
create table shedlock (
    name       varchar(64)  not null,
    lock_until timestamp(3) not null,
    locked_at  timestamp(3) not null default current_timestamp(3),
    locked_by  varchar(255) not null,
    primary key (name)
) engine = InnoDB default charset = utf8mb4 collate = utf8mb4_unicode_ci;
