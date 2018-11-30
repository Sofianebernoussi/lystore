ALTER TABLE lystore.order_client_equipment
  DROP CONSTRAINT "status_values";

ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') );