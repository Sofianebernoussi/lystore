ALTER TABLE lystore.order_client_equipment DROP CONSTRAINT fk_equipment_id;

ALTER TABLE lystore.order_client_equipment DROP COLUMN id_equipment;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN name character varying(255) NOT NULL;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN summary character varying(300);

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN description text;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN image character varying(100);

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN technical_spec json;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN status character varying(50) NOT NULL;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN id_contract bigint  NOT NULL;
	ALTER TABLE lystore.order_client_equipment
    ADD COLUMN equipment_key bigint;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN cause_status character varying(300);

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN "number_validation" character varying(50);

ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT fk_contract_id FOREIGN KEY (id_contract)
    REFERENCES lystore.contract (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;

ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT "Check_tax_amount_positive" CHECK (tax_amount >= 0::numeric);

ALTER TABLE lystore.order_client_options DROP COLUMN id_option;

ALTER TABLE lystore.order_client_options
    ADD COLUMN name character varying NOT NULL;

ALTER TABLE lystore.order_client_options
    ADD COLUMN amount integer NOT NULL;

ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID', 'REJECTED', 'SENT', 'DONE') );

ALTER TABLE lystore.order_client_options
    ADD COLUMN required boolean NOT NULL;
ALTER TABLE lystore.order_client_options DROP CONSTRAINT "FK_order-client-equipment_id";

CREATE  SEQUENCE lystore.seq_validation_number
  INCREMENT 1
  MINVALUE 0
  MAXVALUE 99999999999999
  START 19700101001
  CACHE 1;
ALTER TABLE lystore.seq_validation_number
  OWNER TO postgres;


CREATE OR REPLACE FUNCTION lystore.get_validation_number()
  RETURNS VARCHAR AS $$
DECLARE
nextSeqVal VARCHAR ;
valeurinitiale VARCHAR := '0001';
BEGIN
	select  pg_catalog.nextval('lystore.seq_validation_number'::regclass) into nextSeqVal;
	if (left(nextSeqVal ,8 ) != replace (  CURRENT_DATE || '', '-' , ''))
	then
		select replace (  CURRENT_DATE || '', '-' , '') || valeurinitiale into nextSeqVal;
		PERFORM pg_catalog.setval('lystore.seq_validation_number'::regclass, nextSeqVal::BIGINT, true);
	end if;
	return nextSeqVal;
 END;
 $$ LANGUAGE plpgsql VOLATILE

ALTER TABLE lystore.order_client_equipment
	ADD COLUMN file character varying (250);