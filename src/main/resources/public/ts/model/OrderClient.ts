import {_, idiom as lang, model, moment, notify} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {Campaign, Contract, ContractType, Structure, Supplier, TechnicalSpec, Utils} from './index';
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
    priceProposalTTCTotal: number;
    grade?: Grade;
    title?: Title;
    options: OrderOptionClient[];
    technical_spec: TechnicalSpec[];
    preference: number;
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
    contract_type: ContractType;
    name_structure: string;
    id_contract: number;
    id_campaign: number;
    id_structure: string;
    id_supplier: string;
    id_project: number;
    id_operation: number;
    selected: boolean;
    comment?: string;
    price_proposal?: number;
    rank?:number;
    structure: Structure;
    priceUnitedTTC: number;
    rankOrder: string;
    isOrderRegion: Boolean;
    constructor() {

    }

    calculatePriceTTC ( roundNumber?: number, priceCalculate?: number)  {
        let price = parseFloat(Utils.calculatePriceTTC(priceCalculate , this.tax_amount).toString());
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
            return await http.delete(`/lystore/order/${this.id}/${this.id_structure}/${this.id_campaign}`);
        } catch (e) {
            notify.error('lystore.order.delete.err');
        }
    }

    downloadFile(file) {
        window.open(`/lystore/order/${this.id}/file/${file.id}`);
    }

    async updateStatusOrder(status: String, id:number = this.id){
        try {
            await http.put(`/lystore/order/${id}`, {status: status});
        } catch (e) {
            notify.error('lystore.order.update.err');
        }
    }

    static formatSqlDataToModel(data: any) {
        return {
            action: data.action,
            amount: data.amount,
            cause_status: data.cause_status,
            comment: data.comment,
            creation_date: data.creation_date,
            description: data.description,
            equipment_key: data.equipment_key,
            id: data.id,
            id_campaign: data.id_campaign,
            id_contract: data.id_contract,
            id_operation: data.id_operation,
            id_order: data.id_order,
            id_project: data.id_project,
            id_structure: data.id_structure,
            image: data.image,
            name: data.name,
            number_validation: data.number_validation,
            price: data.price,
            price_proposal: data.price_proposal,
            program: data.program,
            rank: data.rank,
            status: data.status,
            summary: data.summary,
            tax_amount: data.tax_amount
        }
            ;
    }

    async get() {
        try {
            let {data} = await http.get(`/lystore/order/${this.id}`);
            Mix.extend(this, OrderClient.formatSqlDataToModel(data));

        } catch (e) {
            notify.error('lystore.order.get.err');
        }
    }

    async getOneOrderClientProgress(id){
        try{
            const {data} = await http.get(`/lystore/orderClient/${id}/order/progress`);
            const result = {
                ...data,
                project: data.project?Mix.castAs(Project, JSON.parse(data.project.toString())):null,
                campaign: data.campaign?Mix.castAs(Campaign, JSON.parse(data.campaign)):null,
                contract_type: data.contract_type?JSON.parse(data.contract_type):null,
                contract: data.contract?JSON.parse(data.contract):null,
                structure_groups: data.structure_groups?JSON.parse(data.structure_groups):null,
                options: data.options?JSON.parse(data.options):null,
                supplier: data.supplier?JSON.parse(data.supplier):null,
                title: data.title?JSON.parse(data.title):null,
                price : data.price?parseFloat(data.price):null,
                amount : data.amount?parseInt(data.amount):null,
                price_proposal : data.price_proposal?parseFloat(data.price_proposal):null,
                rank : data.rank?parseInt(data.rank.toString()) : null,
                tax_amount : data.tax_amount?parseFloat(data.tax_amount):null,
                price_single_ttc : data.price_single_ttc?parseFloat(data.price_single_ttc):null,
                technical_specs: data.technical_specs?JSON.parse(data.technical_specs):null,
            };
            return result
        } catch (e) {
            notify.error('lystore.admin.order.get.err');
            throw e;
        }
    }

    async getOneOrderClientWaiting(id){
        try{
            const {data} = await http.get(`/lystore/orderClient/${id}/order/waiting`);
            const result = {
                ...data,
                project: data.project?Mix.castAs(Project, JSON.parse(data.project.toString())):null,
                campaign: data.campaign?Mix.castAs(Campaign, JSON.parse(data.campaign)):null,
                contract_type: data.contract_type?JSON.parse(data.contract_type):null,
                contract: data.contract?JSON.parse(data.contract):null,
                structure_groups: data.structure_groups?JSON.parse(data.structure_groups):null,
                options: data.options?JSON.parse(data.options):null,
                supplier: data.supplier?JSON.parse(data.supplier):null,
                title: data.title?JSON.parse(data.title):null,
                price : data.price?parseFloat(data.price):null,
                amount : data.amount?parseInt(data.amount):null,
                price_proposal : data.price_proposal?parseFloat(data.price_proposal):null,
                rank : data.rank?parseInt(data.rank.toString()) : null,
                tax_amount : data.tax_amount?parseFloat(data.tax_amount):null,
                price_single_ttc : data.price_single_ttc?parseFloat(data.price_single_ttc):null,
                technical_specs: data.technical_specs?JSON.parse(data.technical_specs):null,
            };
            return result
        } catch (e) {
            notify.error('lystore.admin.order.get.err');
            throw e;
        }
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
    filters: Array<string>;
    ordersOfOperation: Array<OrderClient>;

    constructor(supplier?: Supplier) {
        super([]);
        this.supplier = supplier ? supplier : new Supplier();
        this.dateGeneration = new Date();
        this.projects = new Selection<Project>([]);
        this.id_project_use = -1;
        this.filters = [];
    }

    async updateReference(tabIdsProjects: Array<object>, id_campaign:number, id_project:number, id_structure:string) {
        try {
            await  http.put(`/lystore/campaign/${id_campaign}/projects/${id_project}/preferences?structureId=${id_structure}`,
                { preferences: tabIdsProjects });
        }catch (e) {
            notify.error('lystore.project.update.err');
        }
    }
    async sync (status: string, structures: Structure[] = [], idCampaign?: number, idStructure?: string) {
        try {
            this.projects = new Selection<Project>([]);
            this.id_project_use = -1;
            if (idCampaign && idStructure ) {
                let { data } = await http.get(  `/lystore/orders/${idCampaign}/${idStructure}` );
                this.all = Mix.castArrayAs(OrderClient, data);

                this.all.map((order) => {
                    order.price = parseFloat(order.price.toString());
                    order.price_proposal = order.price_proposal? parseFloat( order.price_proposal.toString()) : null;
                    order.tax_amount = parseFloat(order.tax_amount.toString());
                    order.project = Mix.castAs(Project, JSON.parse(order.project.toString()));
                    order.project.init(idCampaign, idStructure);
                    order.project.title = Mix.castAs(Title, JSON.parse(order.title.toString()));
                    if (this.id_project_use != order.project.id) {
                        this.id_project_use = order.project.id;
                        this.projects.push(order.project);
                    }
                    order.rank = order.rank ? parseInt(order.rank.toString()) : null ;

                    order.options = order.options.toString() !== '[null]' && order.options !== null ?
                        Mix.castArrayAs(OrderOptionClient, JSON.parse(order.options.toString()))
                        : order.options = [];
                    order.options.map((order) => order.selected = true);
                    order.files = order.files !== '[null]' ? Utils.parsePostgreSQLJson(order.files) : [];
                });
                this.all = _.sortBy(this.all, (order)=> order.rank != null ? order.rank : this.all.length );
                this.projects.all = _.sortBy(this.projects.all, (project)=> project.preference != null
                    ? project.preference
                    : this.projects.all.length );
            } else {
                let { data } = await http.get(  `/lystore/orders?status=${status}` );
                this.all = Mix.castArrayAs(OrderClient, data);
                this.all.map((order: OrderClient) => {
                    order.name_structure =  structures.length > 0 ? this.initNameStructure(order.id_structure, structures) : '';
                    order.structure = structures.length > 0 ? this.initStructure(order.id_structure, structures) : new Structure();
                    order.price = parseFloat(status === 'VALID' ? order.price.toString().replace(',', '.') : order.price.toString());
                    order.structure_groups = Utils.parsePostgreSQLJson(order.structure_groups);

                    if (status !== 'VALID') {
                        order.tax_amount = parseFloat(order.tax_amount.toString());
                        order.contract = Mix.castAs(Contract,  JSON.parse(order.contract.toString()));
                        order.contract_type = Mix.castAs(ContractType,  JSON.parse(order.contract_type.toString()));
                        order.supplier = Mix.castAs(Supplier,  JSON.parse(order.supplier.toString()));
                        order.id_supplier = order.supplier.id;
                        order.campaign = Mix.castAs(Campaign,  JSON.parse(order.campaign.toString()));
                        order.project = Mix.castAs(Project, JSON.parse(order.project.toString()));
                        order.project.title = Mix.castAs(Title, JSON.parse(order.title.toString()));
                        order.rank = order.rank ? parseInt(order.rank.toString()) : null ;
                        if (this.id_project_use != order.project.id) {
                            this.id_project_use = order.project.id;
                            this.projects.push(order.project);
                        }
                        order.creation_date = moment(order.creation_date).format('L');
                        order.options.toString() !== '[null]' && order.options !== null ?
                            order.options = Mix.castArrayAs( OrderOptionClient, JSON.parse(order.options.toString()))
                            : order.options = [];
                        order.priceTTCtotal = parseFloat((order.calculatePriceTTC(2, order.price) as number).toString()) * order.amount;
                        order.priceUnitedTTC = order.price_proposal ?
                            parseFloat(( order.price_proposal).toString()):
                            parseFloat((order.calculatePriceTTC(2, order.price) as number).toString());
                        order.priceProposalTTCTotal = order.price_proposal !== null ?
                            parseFloat(( order.price_proposal).toString()) * order.amount :
                            null;
                        if( order.campaign.orderPriorityEnable()){
                            order.rankOrder = (order.rank + 1).toString();
                        } else if (order.campaign.projectPriorityEnable()){
                            order.rankOrder = (order.project.preference + 1).toString();
                        }else{
                            order.rankOrder = lang.translate("lystore.order.not.prioritized");
                        }
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
            let statusURL = status
            if (status === "IN PROGRESS") {
                statusURL = "inprogress";
            }
            let config = status === 'SENT' ? {responseType: 'arraybuffer'} : {};
            return await  http.put(`/lystore/orders/${statusURL.toLowerCase()}`, this.toJson(status), config);
        } catch (e) {
            notify.error('lystore.order.update.err');
            throw e;
        }
    }

    async updateOrderRanks(tabIdsProjects: Array<object>, structureId:string, campaignId:number){
        try {
            await  http.put(`/lystore/order/rank/move?idStructure=${structureId}&idCampaign=${campaignId}`,{ orders: tabIdsProjects });
        }catch (e) {
            notify.error('lystore.project.update.err');
            throw e;
        }
    }

    initNameStructure (idStructure: string, structures: Structure[]) {
        let structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure.uai + '-' + structure.name : '' ;
    }
    initStructure (idStructure: string, structures: Structure[]) {
        let structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure : new Structure() ;
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
        for (let i = 0; i < this.all.length; i++) {
            let order = this.all[i];
            total += order.price_proposal !== null ? order.priceProposalTTCTotal : order.priceTTCtotal;
        }
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
    async addOperation (idOperation:number, idsOrder: Array<number>) {
        try{
            await http.put(`/lystore/orders/operation/${idOperation}`, idsOrder);
        }catch (e){
            notify.error('lystore.basket.update.err');
            throw e;
        }
    }
    async addOperationInProgress (idOperation:number, idsOrder: Array<number>) {
        try{
            await http.put(`/lystore/orders/operation/in-progress/${idOperation}`, idsOrder);
        }catch (e){
            notify.error('lystore.basket.update.err');
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