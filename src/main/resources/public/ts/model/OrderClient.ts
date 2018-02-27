import { notify } from 'entcore';
import { Selectable, Selection, Mix } from 'entcore-toolkit';
import { TechnicalSpec, Contract, Supplier, Campaign } from './index';
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

id_contract: number;
id_campaign: number;
id_structure: string;
selected: boolean;

constructor() {}
}
 export class OrdersClient extends Selection<OrderClient> {
    constructor() {
        super([]);
    }

    async sync (idCampaign?: number, idStructure?: string) {
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
                });
            }
        } catch (e) {
            notify.error('lystore.order.sync.err');
        }
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