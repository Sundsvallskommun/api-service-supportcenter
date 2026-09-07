create table end_of_lease_batch (
    id                varchar(36) not null,
    -- Set by the sender, not by us. Unique per sender so that a batch that is sent again after a timeout is recognized
    -- as the one already stored instead of queueing every computer in it a second time. Scoped by municipality because
    -- the id belongs to the sender's own numbering, so one sender must not be handed another sender's batch.
    external_batch_id varchar(36) not null,
    -- The municipality of the sender. The municipality of a computer is read from POB and kept on the computer row.
    municipality_id   varchar(4)  not null,
    created           datetime(6) not null,
    primary key (id),
    constraint uq_end_of_lease_batch_municipality_id_external_batch_id unique (municipality_id, external_batch_id)
) engine = InnoDB default charset = utf8mb4 collate = utf8mb4_unicode_ci;

create table end_of_lease_computer (
    id                    varchar(36)   not null,
    batch_id              varchar(36)   not null,
    serial_number         varchar(64)   not null,
    asset_tag             varchar(64)   not null,
    end_of_lease_date     date          not null,
    status                varchar(32)   not null,
    -- Read from the POB configuration item (Virtual.CIKommun) and decides which SysMan instance the computer belongs
    -- to. Null until a computer has been picked up and looked up.
    asset_municipality_id varchar(4),
    attempts              int           not null default 0,
    error_message         varchar(2048),
    sent_at               datetime(6),
    created               datetime(6)   not null,
    modified              datetime(6),
    primary key (id),
    -- Declared here rather than left to the foreign key, which would otherwise create an index of its own under the
    -- name of the constraint.
    key ix_end_of_lease_computer_batch_id (batch_id),
    constraint fk_end_of_lease_computer_batch foreign key (batch_id) references end_of_lease_batch (id)
) engine = InnoDB default charset = utf8mb4 collate = utf8mb4_unicode_ci;

-- Leads on status so that the query for the next computer to send reads the pending rows only, whatever the number of
-- rows that have already been sent, and finds them in age order without sorting.
create index ix_end_of_lease_computer_status_created on end_of_lease_computer (status, created);

-- Looking a single computer up by either of the two identifiers it is known by is otherwise a full table scan, which
-- costs more the longer the table is kept. Not unique, since the same computer can appear in more than one batch if a
-- lease is extended.
create index ix_end_of_lease_computer_serial_number on end_of_lease_computer (serial_number);
create index ix_end_of_lease_computer_asset_tag on end_of_lease_computer (asset_tag);
