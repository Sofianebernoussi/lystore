import http from "axios";
import {notify} from "entcore";
import {Project} from "./project";
import {Campaign, Contract, ContractType, Structure, TechnicalSpec} from "./index";
import {OrderClient} from "./OrderClient";

export class OrderRegion {

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

    private toJson() {
        return {
            id: this.id,
            amount: this.amount,
            name: this.name,
            price: this.price,
            summary: this.summary,
            description: this.description,
        }
    }

    createFromOrderClient(order: OrderClient) {
        this.order_client = order;
        this.id_orderClient = order.id;
        this.amount = order.amount;
        this.name = order.name;
        this.summary = order.summary
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
        if (order.rank)
            this.rank = order.rank;
        this.structure = order.structure;
    }

    async adminUpdate() {
        try {
            http.put(`/lystore/region/order/`, this.toJson());
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }
}