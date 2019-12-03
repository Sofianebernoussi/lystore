ALTER  table  Lystore.operation
DROP CONSTRAINT  op_id_label,
DROP CONSTRAINT  op_date_cp,
ADD CONSTRAINT operation_unicity_label_date UNIQUE (id_label,date_cp)
;