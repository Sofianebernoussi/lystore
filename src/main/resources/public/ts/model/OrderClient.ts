import {_, model, moment, notify} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {Campaign, Contract, Structure, Supplier, TechnicalSpec, Utils} from './index';
import http from 'axios';
import {Project} from "./project";
import {Title} from "./title";
import {Grade} from "./grade";

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
    grade?: Grade;
    title?: Title;
    options: OrderOptionClient[];
    technical_spec: TechnicalSpec[];
    contract: Contract;
    supplier: Supplier;
    campaign: Campaign;
    structure_groups: string[];
    order_number?: string;
    label_program?: string;
    contract_name?: string;
    supplier_name?: string;
    project: Project;
    files: any;

    name_structure: string;
    id_contract: number;
    id_campaign: number;
    id_structure: string;
    id_supplier: string;
    id_project: number;
    selected: boolean;
    comment?: string;
    price_proposal?: number;

    constructor() {
    }

    calculatePriceTTC ( roundNumber?: number)  {
        let price = parseFloat(Utils.calculatePriceTTC(this.price , this.tax_amount).toString());
        if (this.options !== undefined) {
            this.options.map((option) => {
                price += parseFloat(Utils.calculatePriceTTC(option.price , option.tax_amount).toString() );
            });
        }
        return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
    }

    async updateComment(){
        try{
            http.put(`/lystore/order/${this.id}/comment`, { comment: this.comment });
        }catch (e){
            notify.error('lystore.basket.update.err');
            throw e;
        }
    }


    async delete () {
        try {
            return await http.delete(`/lystore/order/${this.id}/${this.id_structure}`);
        } catch (e) {
            notify.error('lystore.order.delete.err');
        }
    }


    downloadFile(file) {
        window.open(`/lystore/order/${this.id}/file/${file.id}`);
    }
}
export class OrdersClient extends Selection<OrderClient> {

    supplier: Supplier;
    bc_number?: string;
    id_program?: number;
    engagement_number?: string;
    projects: Selection<Project>;

    dateGeneration?: Date;
    id_project_use?: number;

    constructor(supplier?: Supplier) {
        super([]);
        this.supplier = supplier ? supplier : new Supplier();
        this.dateGeneration = new Date();
        this.projects = new Selection<Project>([]);
        this.id_project_use = -1;

    }


    async sync (status: string, structures: Structure[] = [], idCampaign?: number, idStructure?: string) {
        try {
            this.projects = new Selection<Project>([]);

            if (idCampaign && idStructure ) {
                let { data } = await http.get(  `/lystore/orders/${idCampaign}/${idStructure}` );
                this.all = Mix.castArrayAs(OrderClient, data);
                this.all.map((order) => {
                    order.price = parseFloat(order.price.toString());
                    order.tax_amount = parseFloat(order.tax_amount.toString());
                    order.project = Mix.castAs(Project, JSON.parse(order.project.toString()));
                    order.project.title = Mix.castAs(Title, JSON.parse(order.title.toString()));
                    order.project.grade = Mix.castAs(Grade, JSON.parse(order.grade.toString()));

                    if (this.id_project_use != order.project.id) {
                        this.id_project_use = order.project.id;
                        this.projects.push(order.project);
                    }


                    order.options = order.options.toString() !== '[null]' && order.options !== null ?
                        Mix.castArrayAs(OrderOptionClient, JSON.parse(order.options.toString()))
                        : order.options = [];
                    order.options.map((order) => order.selected = true);
                    order.files = order.files !== '[null]' ? Utils.parsePostgreSQLJson(order.files) : [];
                });
            } else {
                let { data } = await http.get(  `/lystore/orders?status=${status}` );
                this.all = Mix.castArrayAs(OrderClient, data);
                this.all.map((order: OrderClient) => {
                    order.name_structure =  structures.length > 0 ? this.initNameStructure(order.id_structure, structures) : '';
                    order.price = parseFloat(status === 'VALID' ? order.price.toString().replace(',', '.') : order.price.toString());
                    order.structure_groups = Utils.parsePostgreSQLJson(order.structure_groups);
                    if (status !== 'VALID') {
                        order.tax_amount = parseFloat(order.tax_amount.toString());
                        order.contract = Mix.castAs(Contract,  JSON.parse(order.contract.toString()));
                        order.supplier = Mix.castAs(Supplier,  JSON.parse(order.supplier.toString()));
                        order.id_supplier = order.supplier.id;
                        order.campaign = Mix.castAs(Campaign,  JSON.parse(order.campaign.toString()));
                        order.project = Mix.castAs(Project, JSON.parse(order.project.toString()));
                        order.project.title = Mix.castAs(Title, JSON.parse(order.title.toString()));
                        order.project.grade = Mix.castAs(Grade, JSON.parse(order.grade.toString()));

                        if (this.id_project_use != order.project.id) {
                            this.id_project_use = order.project.id;
                            this.projects.push(order.project);
                        }
                        order.creation_date = moment(order.creation_date).format('L');
                        order.options.toString() !== '[null]' && order.options !== null ?
                            order.options = Mix.castArrayAs( OrderOptionClient, JSON.parse(order.options.toString()))
                            : order.options = [];
                        order.priceTTCtotal = parseFloat((order.calculatePriceTTC(2) as number).toString()) * order.amount;
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