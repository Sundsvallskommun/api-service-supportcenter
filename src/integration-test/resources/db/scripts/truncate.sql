-- Deletes rather than truncates, since InnoDB refuses to truncate a table a foreign key points at.
DELETE FROM end_of_lease_computer;
DELETE FROM end_of_lease_batch;
