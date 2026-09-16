-- A dependency failure deliberately costs a computer nothing, and nothing else then stops the two runs from picking
-- the same row up again on the very next pass. Both runs read a page from the front of the queue in age order, so a
-- few hundred rows that keep failing on something outside themselves fill every page and nothing behind them is ever
-- read again. retry_after holds such a row back for longer than the interval between runs, and the queue moves past it.
alter table end_of_lease_computer
    add column retry_after datetime(6) after error_message;

-- Rows are read detached, written back with a merge of the whole entity, and two instances that both believe they hold
-- the ShedLock lock would silently overwrite each other. With a version the second write fails instead of being lost.
-- Existing rows start at zero.
alter table end_of_lease_computer
    add column version bigint not null default 0 after modified;
