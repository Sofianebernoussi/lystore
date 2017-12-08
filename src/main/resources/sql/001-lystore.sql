CREATE SCHEMA lystore;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE lystore.scripts (
  filename character varying(255) NOT NULL,
  passed timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT scripts_pkey PRIMARY KEY (filename)
);

CREATE TABLE lystore.agent (
  id bigserial NOT NULL,
  email character varying(255),
  department character varying(255),
  name character varying(100),
  phone character varying(45),
  CONSTRAINT agent_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.supplier (
  id bigserial NOT NULL,
  name character varying(100),
  address character varying(255),
  email character varying(255),
  phone character varying(45),
  CONSTRAINT supplier_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.contract_type (
  id bigserial NOT NULL,
  code character varying(50),
  name character varying(255),
  CONSTRAINT contract_type_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.program (
  id bigserial NOT NULL,
  name character varying(255),
  CONSTRAINT program_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.contract (
  id bigserial NOT NULL,
  name character varying(255),
  annual_min numeric,
  annual_max numeric,
  start_date date,
  nb_renewal numeric,
  id_contract_type bigint,
  max_brink numeric,
  id_supplier bigint,
  id_agent bigint,
  id_program bigint,
  reference character varying(50),
  end_date date,
  renewal_end date,
  CONSTRAINT contract_pk PRIMARY KEY (id),
  CONSTRAINT fk_agent_id FOREIGN KEY (id_agent)
  REFERENCES lystore.agent (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_contract_type_id FOREIGN KEY (id_contract_type)
  REFERENCES lystore.contract_type (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_program_id FOREIGN KEY (id_program)
  REFERENCES lystore.program (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_supplier_id FOREIGN KEY (id_supplier)
  REFERENCES lystore.supplier (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE lystore.tag (
  id bigserial NOT NULL,
  name character varying(255),
  color character varying(7),
  CONSTRAINT tag_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.rel_equipment_tag (
  id_equipment bigint,
  id_equipment_tag bigint,
  CONSTRAINT fk_tag_id FOREIGN KEY (id_equipment_tag)
  REFERENCES lystore.tag (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE
);