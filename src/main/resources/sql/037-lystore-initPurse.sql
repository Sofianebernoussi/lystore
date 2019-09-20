ALTER table lystore.purse
ADD COLUMN initial_amount NUMERIC ;

UPDATE  lystore.purse
set initial_amount = amount;
