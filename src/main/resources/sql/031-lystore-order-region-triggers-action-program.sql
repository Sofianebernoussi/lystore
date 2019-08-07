ALTER TABLE lystore."order-region-equipment"
ADD COLUMN program character varying,
ADD COLUMN action character varying;

CREATE OR REPLACE FUNCTION lystore.fill_program_action_region() RETURNS TRIGGER AS $order-region-equipment$
DECLARE
	actionValue bigint;
	programValue character varying;
BEGIN
	SELECT lystore.getAction(NEW.id) INTO actionValue;

	SELECT name
	FROM lystore.program
	INNER JOIN lystore.program_action ON (program.id = program_action.id_program)
	WHERE program_action.action = actionValue
	INTO programValue;

    NEW.program := programValue;
    NEW.action := actionValue;
    RETURN NEW;
END;
$order-region-equipment$ LANGUAGE plpgsql;

CREATE TRIGGER fill_program_action_region BEFORE INSERT ON lystore."order-region-equipment"
FOR EACH ROW EXECUTE PROCEDURE lystore.fill_program_action_region();
