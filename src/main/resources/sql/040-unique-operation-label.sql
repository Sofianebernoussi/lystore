ALTER  table  Lystore.operation
ADD CONSTRAINT  op_id_label UNIQUE (id_label),
ADD CONSTRAINT  op_date_cp UNIQUE (date_cp)
;