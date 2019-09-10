import {
    Campaign,
    Contract,
    ContractType, OrderClient, OrderOptionClient, OrderRegion,
    Project, Structure,
    Structures,
    Supplier,
    TechnicalSpec, Utils
} from './index';
import {Mix, Selectable} from "entcore-toolkit";
import {_, idiom as lang, moment} from "entcore";
import forEach = require("core-js/fn/array/for-each");

"use strict";
export interface OrderImp extends Selectable{
    selected;

    typeOrder:string;
    amount: number;
    campaign: Campaign;
    comment: string;
    contract: Contract;
    contract_type: ContractType;
    creation_date: Date;
    description: string;
    files: string;
    id_campaign: number;
    id_contract: number;
    id_operation: number;
    id_project: number;
    id_structure: string;
    image: string;
    label_program?: string;
    name: string;
    name_structure: string;
    number_validation: string;
    options: OrderOptionClient[];
    price: number;
    price_single_ttc: number;
    project: Project;
    program: string;
    rank: number;
    rankOrder: Number;
    status: string;
    structure: Structures;
    structure_groups:any;
    summary: string;
    supplier: Supplier;
    tax_amount: number;
    technical_spec: TechnicalSpec[];
}

export class Order implements OrderImp{
    typeOrder;
    amount;//waiting
    campaign;//waiting
    comment;//waiting
    contract;//waiting
    contract_type;//waiting
    creation_date;
    description;
    files;
    id_campaign;
    id_contract;
    id_operation;
    id_project;
    id_structure;
    image;
    label_program?;
    name;//waiting
    name_structure;
    number_validation;
    options;
    price;
    price_single_ttc;
    project;//waiting
    program;//waiting
    rank;
    rankOrder;//waiting
    selected;
    status;
    structure;//waiting
    structure_groups;
    summary;
    supplier;
    tax_amount;
    technical_spec;
    inheritedClass:Order|OrderClient|OrderRegion;


    order_client_equipment_parent?:Order|OrderClient|OrderRegion;

    constructor(order: Order|OrderClient|OrderRegion, structures:Structures){

        this.inheritedClass = order;
        this.typeOrder = order.constructor.name;
        this.structure = order.id_structure? this.initStructure( order.id_structure, structures) : new Structure();
        this.name_structure = order.id_structure? this.initNameStructure( order.id_structure, structures) : "";
        if(order.order_client_equipment_parent)this.order_client_equipment_parent = order.order_client_equipment_parent;

        this.project = order.project? Mix.castAs(Project, JSON.parse(order.project.toString())) : null;
        this.campaign = order.campaign? Mix.castAs(Campaign, JSON.parse(order.campaign.toString())) : null;
        this.contract_type = order.contract_type? JSON.parse(order.contract_type) : null;
        this.contract = order.contract? JSON.parse(order.contract) : null;
        this.structure_groups = order.structure_groups? JSON.parse(order.structure_groups) : null;
        this.options = order.options.toString() !== '[null]' && order.options !== null ?
            Mix.castArrayAs(OrderOptionClient, JSON.parse(order.options.toString()))  :
            [];
        this.supplier = order.supplier? JSON.parse(order.supplier) : null;
        //this.title = order.title?JSON.parse(order.title) : null;
        this.price  = order.price? parseFloat(order.price) : null;
        this.amount  = order.amount? parseInt(order.amount) : null;
        //this.price_proposal  = order.price_proposal?parseFloat(order.price_proposal) =null;
        this.rank  = order.rank? parseInt(order.rank.toString())+1  : null;
        this.tax_amount  = order.tax_amount? parseFloat(order.tax_amount) : null;
        this.price_single_ttc  = order.price_single_ttc? parseFloat(order.price_single_ttc) : null;
        this.technical_spec = order.technical_spec? Utils.parsePostgreSQLJson(this.technical_spec) : null;
        this.creation_date = moment(order.creation_date).format('L');

        // if (order.campaign.orderPriorityEnable()) {
        //     order.rankOrder = order.rank + 1;
        // } else if (order.campaign.projectPriorityEnable()) {
        //     order.rankOrder = order.project.preference + 1;
        // } else {
        //     order.rankOrder = lang.translate("lystore.order.not.prioritized");
        // }
    }

    initStructure(idStructure:string, structures:Structures):Structure{
        const structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure : new Structure() ;
    }

    initNameStructure (idStructure: string, structures: Structures):string {
        let structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure.uai + '-' + structure.name : '' ;
    }


    calculatePriceTTC ( roundNumber?: number, priceCalculate?: number):number|string {
        let price = parseFloat(Utils.calculatePriceTTC(priceCalculate , this.tax_amount).toString());
        if (this.options !== undefined) {
            this.options.map((option) => {
                price += parseFloat(Utils.calculatePriceTTC(option.price , option.tax_amount).toString() );
            });
        }
        return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
    }
}