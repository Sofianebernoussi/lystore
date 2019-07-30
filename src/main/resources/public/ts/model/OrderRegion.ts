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
            equipment_key: this.equipment_key,
            comment: (this.comment) ? this.comment : "",
            ...(this.rank && {rank: this.rank}),
            technical_specs: (this.technical_spec === null || this.technical_spec.length === 0) ?
                []:
                Utils.jsonParse(this.technical_spec.toString())
                    .map((spec: TechnicalSpec) => {
                        return {
                            name: spec.name,
                            value: spec.value
                        }
                    }),
            id_operation: this.id_operation,
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
        if (order.rank )
            this.rank = order.rank;
        this.structure = order.structure;
        this.id_operation = order.id_operation;
    }

    async set() {
        try {
            return await http.put(`/lystore/region/order/`, this.toJson());
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
}