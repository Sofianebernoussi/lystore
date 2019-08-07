import http from "axios";
import {moment, notify} from "entcore";
import {Project} from "./project";
import {Campaign, Contract, ContractType, OrderOptionClient, Structure, TechnicalSpec, Utils} from "./index";
import {OrderClient} from "./OrderClient";
import {Mix, Selectable, Selection} from "entcore-toolkit";
import {Equipment} from "./Equipment";

export class OrderRegion implements Selectable {
    selected: boolean;

    id?: number;
    amount: number;
    name: string;
    price: number;
    summary: string;
    description: string;
    image: string;
    creation_date: Date;
    status: string;
    number_validation: string;
    technical_spec: TechnicalSpec[];
    contract: Contract;
    campaign: Campaign;
    structure_groups: string[];
    contract_name?: string;
    project: Project;
    files: any;
    contract_type: ContractType;
    order_client: OrderClient;
    name_structure: string;
    id_contract: number;
    id_campaign: number;
    id_structure: string;
    id_project: number;
    id_orderClient: number;
    comment?: string;
    rank?: number;
    structure: Structure;
    id_operation: number;
    equipment_key: number;
    title_id ?: number;
    equipment?: Equipment;

    toJson() {
        return {
            amount: this.amount,
            name: this.name,
            price: this.price,
            summary: this.summary,
            description: (this.description) ? this.description : "",
            ...(this.id_orderClient && {id_order_client_equipment: this.id_orderClient}),
            image: this.image,
            creation_date: moment().format('YYYY-MM-DD'),
            status: this.status,
            ...(this.number_validation && {number_validation: this.number_validation}),
            ...(this.title_id && {title_id: this.title_id}),

            id_contract: this.id_contract,
            files: this.files,
            name_structure: this.name_structure,
            id_campaign: this.id_campaign,
            id_structure: this.id_structure,
            id_project: this.id_project,
            equipment_key: this.equipment.id ,
            comment: (this.comment) ? this.comment : "",
            ...(this.rank && {rank: this.rank}),
            technical_specs: (Utils.parsePostgreSQLJson(this.technical_spec) === null || Utils.parsePostgreSQLJson(this.technical_spec).length === 0) ?
                []:
                Utils.parsePostgreSQLJson(this.technical_spec).map(spec => {
                    return {
                        name: spec.name,
                        value: spec.value
                    }
                }),
            id_operation: this.id_operation,
            rank: this.rank -1,
        }
    }

    createFromOrderClient(order: OrderClient) {
        this.order_client = order;
        this.id_orderClient = order.id;
        this.amount = order.amount;
        this.name = order.name;
        this.summary = order.summary;
        this.description = order.description;
        this.image = order.image;
        this.creation_date = order.creation_date;
        this.status = order.status;
        this.number_validation = order.number_validation;
        this.technical_spec = order.technical_spec;
        this.contract = order.contract;
        this.campaign = order.campaign;
        this.structure_groups = order.structure_groups;
        this.contract_name = order.contract_name;
        this.project = order.project;
        this.files = order.files;
        this.contract_type = order.contract_type;
        this.name_structure = order.name_structure;
        this.id_contract = order.id_contract;
        this.id_campaign = order.id_campaign;
        this.id_structure = order.id_structure;
        this.id_project = order.id_project;
        this.comment = order.comment;
        this.price = order.price_proposal;
        this.rank = order.rank;
        this.structure = order.structure;
        this.id_operation = order.id_operation;
        this.equipment = order.equipment;
    }

    async set() {
        try {
            return await http.post(`/lystore/region/order`, this.toJson());
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    async update(id){
        try {
            return await http.put(`/lystore/region/order/${id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    initDataFromEquipment() {
        if (this.equipment) {
            this.summary = this.equipment.name;
            this.image = this.equipment.image;

        }
    }

    async delete(id){
        try{
            return await http.delete(`/lystore/region/${id}/order`);
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }



    async getOneOrderRegion(id){
        try{
            const {data} =  await http.get(`/lystore/orderRegion/${id}/order`);
            let result = {
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
                rank : data.rank?parseInt(data.rank.toString()) : null,
                price_single_ttc : data.price_single_ttc?parseFloat(data.price_single_ttc):null,
                technical_spec: data.technical_spec?Utils.parsePostgreSQLJson(this.technical_spec):null,
                order_client_equipment_parent: data.order_client_equipment_parent?
                    Mix.castAs(OrderClient, JSON.parse(data.order_client_equipment_parent.toString())):
                    null,
                isOrderRegion: true,
            };
            if(result.order_client_equipment_parent) {
                result.order_client_equipment_parent.options = data.options_client_parent.toString() !== '[null]' && data.options_client_parent !== null?
                    Mix.castArrayAs(OrderOptionClient, JSON.parse(data.options_client_parent.toString())) :
                    [];
                result.order_client_equipment_parent.equipment = data.equipment_order_client_parent ?
                    Mix.castAs(Equipment, JSON.parse(data.equipment_order_client_parent)) :
                    null;

                result.order_client_equipment_parent.price_united = result.order_client_equipment_parent.price_proposal ?
                    result.order_client_equipment_parent.price_proposal :
                    result.order_client_equipment_parent
                        .calculatePriceTTC(2, result.order_client_equipment_parent.price);
                if (result.order_client_equipment_parent.price_proposal) {
                    result.order_client_equipment_parent.price_total = result.order_client_equipment_parent.price_proposal *
                        result.order_client_equipment_parent.amount;
                } else {
                    result.order_client_equipment_parent.price_total =  result.order_client_equipment_parent
                            .calculatePriceTTC(2, result.order_client_equipment_parent.price) *
                        result.order_client_equipment_parent.amount;
                }
            }
            return  Mix.castAs(OrderRegion, result);
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }
}

export class OrdersRegion extends Selection<OrderRegion> {
    constructor() {
        super([]);
    }

    async create() {
        let orders = [];
        this.all.map(order => {
            order.initDataFromEquipment();
            orders.push(order.toJson());
        });
        try {
            return await http.post(`/lystore/region/orders/`, {orders: orders});
        } catch (e) {
            notify.error('lystore.order.create.err');
            throw e;
        }
    }
    async updateOperation(idOperation:number, idsRegions: Array<number>){
        try {
            await http.put(`/lystore/order/region/${idOperation}/operation`, idsRegions);
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }
}