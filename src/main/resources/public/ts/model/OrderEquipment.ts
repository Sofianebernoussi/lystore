import { notify } from 'entcore';
import { Selectable, Selection, Mix } from 'entcore-toolkit';
import { TechnicalSpec } from './index';
import http from 'axios';

export class OrderEquipment implements Selectable {
id?: number;
amount: number;
name: string;
price: number;
tax_amount: number;
summary: string;
description: string;
image: string;
id_campaign: number;
id_structure: string;
creation_date: Date;
status: string;
id_contract: number;
options: OrderOptionEquipment[];
technical_spec: TechnicalSpec[];
name_supplier: string;
selected: boolean;

    constructor() {}
    async delete (idStructure: number) {
        try {
          return await http.delete(`/lystore/order/${this.id}/${idStructure}`);
        } catch (e) {
            notify.error('lystore.order.delete.err');
        }
    }
}
 export class OrdersEquipments extends Selection<OrderEquipment> {
    constructor() {
        super([]);
    }

    async sync (idCampaign: number, idStructure: string) {
        try {
            let { data } = await http.get(`/lystore/orders/${idCampaign}/${idStructure}`);
            this.all = Mix.castArrayAs(OrderEquipment, data);
            this.all.map((order) => {
                order.price = parseFloat(order.price.toString());
                order.tax_amount = parseFloat(order.tax_amount.toString());
                order.options.toString() !== '[null]' && order.options !== null ?
                    order.options = Mix.castArrayAs( OrderOptionEquipment, JSON.parse(order.options.toString()))
                    : order.options = [];
            });
        } catch (e) {
            notify.error('lystore.order.sync.err');
        }
    }
 }

export class OrderOptionEquipment implements Selectable {
    id?: number;
    tax_amount: number;
    price: number;
    name: string;
    amount: number;
    required: boolean;
    id_order_client_equipment: number;
    selected: boolean;

}