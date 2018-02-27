import { notify, moment, _ } from 'entcore';
import { Selectable, Selection, Mix } from 'entcore-toolkit';
import { TechnicalSpec, Contract, Supplier, Campaign, Structure, Utils } from './index';
import http from 'axios';

export class OrderClient implements Selectable {
id?: number;
amount: number;
name: string;
price: number;
tax_amount: number;
summary: string;
description: string;
image: string;
creation_date: Date;
status: string;

options: OrderOptionClient[];
technical_spec: TechnicalSpec[];
contract: Contract;
supplier: Supplier;
campaign: Campaign;

name_structure: string;
id_contract: number;
id_campaign: number;
id_structure: string;
selected: boolean;

constructor() {}

    calculatePriceTTC = ( roundNumber?: number) => {
        let price = parseFloat(Utils.calculatePriceTTC(this.price , this.tax_amount).toString());
        this.options.map((option) => {
                 price += parseFloat(Utils.calculatePriceTTC(option.price , option.tax_amount).toString() );
        });
        return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
    }
}
 export class OrdersClient extends Selection<OrderClient> {
    constructor() {
        super([]);
    }

    async sync (structures: Structure[] = [], idCampaign?: number, idStructure?: string, ) {
        try {
            if (idCampaign && idStructure ) {
                let { data } = await http.get(  `/lystore/orders/${idCampaign}/${idStructure}` );
                this.all = Mix.castArrayAs(OrderClient, data);
                this.all.map((order) => {
                    order.price = parseFloat(order.price.toString());
                    order.tax_amount = parseFloat(order.tax_amount.toString());
                    order.options.toString() !== '[null]' && order.options !== null ?
                        order.options = Mix.castArrayAs( OrderOptionClient, JSON.parse(order.options.toString()))
                        : order.options = [];
                });
            } else {
                let { data } = await http.get(  `/lystore/orders` );
                this.all = Mix.castArrayAs(OrderClient, data);
                this.all.map((order) => {
                    order.price = parseFloat(order.price.toString());
                    order.tax_amount = parseFloat(order.tax_amount.toString());
                    order.contract = Mix.castAs(Contract,  JSON.parse(order.contract.toString()));
                    order.supplier = Mix.castAs(Supplier,  JSON.parse(order.supplier.toString()));
                    order.campaign = Mix.castAs(Campaign,  JSON.parse(order.campaign.toString()));
                    order.options.toString() !== '[null]' && order.options !== null ?
                        order.options = Mix.castArrayAs( OrderOptionClient, JSON.parse(order.options.toString()))
                        : order.options = [];
                    order.creation_date = moment(order.creation_date, 'YYYY-MM-DD').format('l');
                    order.name_structure =  structures.length > 0 ? this.initNameStructure(order.id_structure, structures) : '';
                });
            }
        } catch (e) {
            notify.error('lystore.order.sync.err');
        }
    }
     initNameStructure  (idStructure: string, structures: Structure[])  {
         let structure = _.findWhere(structures, { id : idStructure});
         return  structure ? structure.name : '' ;
     }
      calculTotalAmount () {
        let total = 0;
        this.all.map((order) => {
            total += order.amount;
        });
        return total;
     }
      calculTotalPriceTTC () {
         let total = 0;
         this.all.map((order) => {
             total += parseFloat( order.calculatePriceTTC().toString()) * order.amount;
         });
         return total;
    }
 }

export class OrderOptionClient implements Selectable {
    id?: number;
    tax_amount: number;
    price: number;
    name_opt: string;
    amount_opt: number;
    required_opt: boolean;
    id_order_client_equipment: number;
    selected: boolean;
}