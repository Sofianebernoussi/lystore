CREATE TABLE lystore.label_operation
(
    id bigserial,
    label character varying(50),
    PRIMARY KEY (id),
    CONSTRAINT uq_label UNIQUE (label)
);


CREATE TABLE lystore.operation
(
    id bigserial,
    id_label bigint,
    status character varying(50),
    id_instruction bigint,
    PRIMARY KEY (id),
    CONSTRAINT fk_id_label FOREIGN KEY (id_label)
        REFERENCES lystore.label_operation (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN id_operation bigint;
ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT fk_operation_id FOREIGN KEY (id_operation)
    REFERENCES lystore.operation (id) MATCH SIMPLE;

-- Add the data in entcore

-- INSERT INTO lystore.label_operation(
--                label)
--                VALUES
--                ('1E 1R PROVISIONS MARCHE'),
-- ('BAC PRO MELEC (PR)'),
-- ('BAC PRO SN (PR)'),
-- ('BTS CHIMIE'),
-- ('CAP ESTHETIQUE COSMETIQUE(PR)'),
-- ('CAP SIGNALETIQUE DECOR PR'),
-- ('COMPLT ET RENOUVT MOB ET GEN'),
-- ('CONSTRUCTION RENOVATION'),
-- ('EQUIPEMENT AUDIOVISUEL DES LYC'),
-- ('EQUIPEMENTS FINANCES RELIQUATS'),
-- ('HANDICAPES PR'),
-- ('LYCEES 100% NUMERIQUE'),
-- ('MACHINES D''ENTRETIEN'),
-- ('MACHINES OUTILS'),
-- ('MAINTENANCE DES MO (PR)'),
-- ('MESURES DE RENTREE MAC'),
-- ('MESURES DE RENTREE SUB'),
-- ('MGIEN'),
-- ('PRIORITES CUISINES'),
-- ('PRIORITES EXAO'),
-- ('PRIORITES PROVISIONS MARCHES'),
-- ('REPROGRAPHIE'),
-- ('SATISFACTION DES PRIORITES'),
-- ('SECURITE CONFORMITE'),
-- ('VEHICULES');
