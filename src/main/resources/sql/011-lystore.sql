
CREATE OR REPLACE FUNCTION delete_empty_project()
RETURNS trigger AS  $$
BEGIN
DELETE FROM Lystore.project as prj
	WHERE prj.id NOT IN (SELECT pp.id FROM Lystore.project as pp
 	  INNER JOIN Lystore.order_client_equipment oce
	  ON oce.id_project = pp.id
	GROUP BY pp.id)
;
RETURN NULL;
END;
 $$  LANGUAGE plpgsql;

CREATE TRIGGER check_empty_project AFTER DELETE
ON Lystore.order_client_equipment
FOR EACH ROW
EXECUTE PROCEDURE delete_empty_project();


ALTER TABLE lystore.project
  ADD COLUMN preference bigint;

ALTER TABLE lystore.campaign
  ADD COLUMN priority_enabled boolean DEFAULT true ;

UPDATE lystore.project
  SET preference=id
WHERE preference IS NULL;