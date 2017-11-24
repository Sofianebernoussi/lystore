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