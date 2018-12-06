DROP TABLE lystore.equipment_type;

CREATE TABLE lystore.equipment_type
(
  id bigserial NOT NULL,
  name character varying(255) NOT NULL,
  CONSTRAINT equipment_type_pkey PRIMARY KEY (id)
);

INSERT INTO Lystore.equipment_type
	VALUES (1,'EQUIPEMENT');
INSERT INTO Lystore.equipment_type
	VALUES (2,'PRESTATION');


ALTER TABLE lystore.EQUIPMENT
  ADD COLUMN id_type bigint NOT NULL DEFAULT 1;
ALTER TABLE lystore.EQUIPMENT
  ADD COLUMN option_enabled boolean NOT NULL DEFAULT false;

ALTER TABLE lystore.EQUIPMENT
  ADD CONSTRAINT fk_equipment_type_id FOREIGN KEY (id_type)
	REFERENCES lystore.equipment (id) MATCH SIMPLE
	ON UPDATE NO ACTION ON DELETE CASCADE;


ALTER TABLE lystore.campaign
ADD COLUMN purse_enabled boolean;

UPDATE lystore.campaign SET purse_enabled = true;

ALTER TABLE lystore.campaign
ALTER COLUMN purse_enabled SET DEFAULT FALSE;