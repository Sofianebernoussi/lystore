import { notify, moment, _ , model } from 'entcore';
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
    number_validation: string;
    priceTTCtotal: number ;
    options: OrderOptionClient[];
    technical_spec: TechnicalSpec[];
    contract: Contract;
    supplier: Supplier;
    campaign: Campaign;
    structure_groups: string[];
    order_number?: string;

    name_structure: string;
    id_contract: number;
    id_campaign: number;
    id_structure: string;
    id_supplier: string;
    selected: boolean;

    constructor() {}

    calculatePriceTTC ( roundNumber?: number)  {
        let price = parseFloat(Utils.calculatePriceTTC(this.price , this.tax_amount).toString());
        this.options.map((option) => {
            price += parseFloat(Utils.calculatePriceTTC(option.price , option.tax_amount).toString() );
        });
        return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
    }

    async delete () {
        try {
            return await http.delete(`/lystore/order/${this.id}/${this.id_structure}`);
        } catch (e) {
            notify.error('lystore.order.delete.err');
        }
    }
}
export class OrdersClient extends Selection<OrderClient> {

    supplier: Supplier;
    bc_number?: string;
    id_program?: number;
    engagement_number?: string;
    dateGeneration?: Date;

    constructor(supplier?: Supplier) {
        super([]);
        this.supplier = supplier ? supplier : new Supplier();
        this.dateGeneration = new Date();
    }

    async sync (status: string, structures: Structure[] = [], idCampaign?: number, idStructure?: string) {
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
                    order.options.map((order) => order.selected = true);
                });
            } else {
                let { data } = await http.get(  `/lystore/orders?status=${status}` );
                this.all = Mix.castArrayAs(OrderClient, data);
                this.all.map((order: OrderClient) => {
                    order.price = parseFloat(order.price.toString());
                    order.name_structure =  structures.length > 0 ? this.initNameStructure(order.id_structure, structures) : '';
                    if (status !== 'VALID') {
                        order.tax_amount = parseFloat(order.tax_amount.toString());
                        order.contract = Mix.castAs(Contract,  JSON.parse(order.contract.toString()));
                        order.supplier = Mix.castAs(Supplier,  JSON.parse(order.supplier.toString()));
                        order.id_supplier = order.supplier.id;
                        order.campaign = Mix.castAs(Campaign,  JSON.parse(order.campaign.toString()));
                        order.creation_date = moment(order.creation_date).format('L');
                        order.options.toString() !== '[null]' && order.options !== null ?
                            order.options = Mix.castArrayAs( OrderOptionClient, JSON.parse(order.options.toString()))
                            : order.options = [];
                        order.priceTTCtotal = parseFloat((order.calculatePriceTTC(2) as number).toString()) * order.amount;
                        order.structure_groups = Utils.parsePostgreSQLJson(order.structure_groups);
                    }
                });
            }
        } catch (e) {
            notify.error('lystore.order.sync.err');
        }
    }

    toJson (status: string) {
        const ids = status === 'SENT'
            ? _.pluck(this.all, 'number_validation')
            : _.pluck(this.all, 'id');

        const supplierId = status === 'SENT'
            ? _.pluck(this.all, 'supplierid')[0]
            : this.supplier.id;
        return {
            ids,
            status : status,
            bc_number: this.bc_number || null,
            engagement_number: this.engagement_number || null,
            dateGeneration: moment(this.dateGeneration).format('DD/MM/YYYY') || null,
            supplierId,
            userId : model.me.userId,
            id_program: this.id_program || null
        };
    }

    async getPreviewData (): Promise<any> {
        try {
            const params = Utils.formatGetParameters(this.toJson('SENT'));
            const { data } = await http.get(`lystore/orders/preview?${params}`);
            return data;
        } catch (e) {
            throw e;
        }
    }

    async updateStatus(status: string) {
        try {
            let config = status === 'SENT' ? {responseType: 'arraybuffer'} : {};
            return  await  http.put(`/lystore/orders/${status.toLowerCase()}`, this.toJson(status), config);
        } catch (e) {
            notify.error('lystore.order.update.err');
            throw e;
        }
    }

    initNameStructure (idStructure: string, structures: Structure[]) {
        let structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure.uai + '-' + structure.name : '' ;
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
            total += parseFloat((order.calculatePriceTTC() as number).toString()) * order.amount;
        });
        return total;
    }

    async cancel (orders: OrderClient[]) {
        try {
            let params = '';
            orders.map((order) => {
                params += `number_validation=${order.number_validation}&`;
            });
            params = params.slice(0, -1);
            await http.delete(`/lystore/orders/valid?${params}`);
        } catch (e) {
            throw e;
        }
    }

}

export class OrderOptionClient implements Selectable {
    id?: number;
    tax_amount: number;
    price: number;
    name: string;
    amount: number;
    required: boolean;
    id_order_client_equipment: number;
    selected: boolean;
}