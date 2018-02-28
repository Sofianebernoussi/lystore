/**
 * Created by rahnir on 22/01/2018.
 * return inputs that contains all filters
 * @table a table of filters "string"
 */
import { ng, _ } from 'entcore';
export const ordersClientFilter = ng.filter('ordersClientFilter', () => {

    return (inputs, table) => {
        let conditionFilter = (order, table) => {
            let result = [];
            let i = 0;
            do {
                result[i] = (table[i] !== '') ?
                    ((order.name_structure ? order.name_structure.toLowerCase().includes(table[i].toLowerCase()) : false )
                        || ( order.contract ? order.contract.name.toLowerCase().includes(table[i].toLowerCase()) : false )
                        || (order.supplier ? order.supplier.name.toString().includes(table[i].toLowerCase()) : false )
                    || (order.campaign ? order.campaign.name.toString().includes(table[i].toLowerCase()) : false) )
                    : true ;
                i++;
            }while (i < table.length && result[i > 1 ? i - 1 : 0] === true);

            return _.reduce(result, (oldVal, newVal ) => {return oldVal && newVal; }, true);
        };

        return table.length > 0
            ? _.without(inputs.map((input) => {
                if (input === undefined || conditionFilter(input, table)) return input ;
            }), undefined )
            :  inputs ;

    };
});