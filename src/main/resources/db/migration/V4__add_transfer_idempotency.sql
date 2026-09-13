ALTER TABLE bank_transactions
ADD COLUMN transfer_request_id VARCHAR(64);

CREATE UNIQUE INDEX ux_bank_transactions_transfer_request_id_account_type
ON bank_transactions (transfer_request_id, account_number, type)
WHERE transfer_request_id IS NOT NULL;
