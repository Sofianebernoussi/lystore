ALTER TABLE lystore.order_client_options
ADD COLUMN id_type bigint;

UPDATE lystore.order_client_options
SET    id_type = subquery.id_type
FROM   (SELECT id_type,
               NAME,
               price
        FROM   lystore.equipment) AS subquery
WHERE  subquery.price = order_client_options.price
       AND order_client_options.NAME = subquery.NAME ;