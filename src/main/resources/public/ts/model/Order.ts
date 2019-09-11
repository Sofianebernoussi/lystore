import {
    Campaign,
    Contract,
    ContractType, Equipment, OrderClient, OrderOptionClient, OrderRegion,
    Project, Structure,
    Structures,
    Title, Utils
} from './index';
import {Mix, Selectable} from "entcore-toolkit";
import {_} from "entcore";

export interface OrderImp extends Selectable{
    amount: number;
    campaign: Campaign;
    comment: string;
    contract: Contract;
    contract_type: ContractType;
    creation_date: Date;
    equipment_key:number;
    id_structure: string;
    price: number;
    price_proposal: number;
    price_single_ttc: number;
    project: Project;
    rank: number;
    rankOrder: Number;
    selected:boolean;
    structure: Structure;
    tax_amount: number;
    title:Title;
}

export class Order implements OrderImp{
    amount;//waiting//
    campaign;//waiting//
    comment;//waiting//
    contract;//waiting//
    contract_type;//waiting//
    creation_date;//
    equipment: Equipment;
    equipment_key;//
    id_structure;
    options;
    price;//
    price_proposal;
    price_single_ttc;//
    project;//waiting//
    program;//waiting
    rank;//
    rankOrder;//waiting
    selected;
    structure;//waiting//
    tax_amount;
    title;//
    inheritedClass:Order|OrderClient|OrderRegion;//


    order_client_equipment_parent?:any;//

    //this.typeOrder = order.constructor.name;
    //this.name_structure = order.id_structure? OrderUtils.initNameStructure( order.id_structure, structures) : "";
    //this.structure_groups = order.structure_groups? JSON.parse(order.structure_groups) : null;
    //this.supplier = order.supplier? JSON.parse(order.supplier) : null;
    //this.creation_date = moment(order.creation_date).format('L');
    //this.technical_spec = order.technical_spec? Utils.parsePostgreSQLJson(this.technical_spec) : null;
    constructor(order: Order, structures:Structures){
        if(order.order_client_equipment_parent){
            this.order_client_equipment_parent = order.order_client_equipment_parent;
        }
        this.inheritedClass = order;
        this.comment = order.comment;
        this.equipment_key = order.equipment_key;
        this.structure = order.id_structure? OrderUtils.initStructure( order.id_structure, structures) : new Structure();
        this.project = order.project? Mix.castAs(Project, JSON.parse(order.project.toString())) : null;
        this.campaign = order.campaign? Mix.castAs(Campaign, JSON.parse(order.campaign.toString())) : null;
        this.contract_type = order.contract_type? JSON.parse(order.contract_type) : null;
        this.contract = order.contract? JSON.parse(order.contract) : null;
        this.title = order.title?JSON.parse(order.title) : null;
        this.price  = order.price? parseFloat(order.price) : null;
        this.amount  = order.amount? parseInt(order.amount) : null;
        this.price_proposal = order.price_proposal? parseFloat(order.price_proposal) : null;
        this.rank  = order.rank? parseInt(order.rank.toString())+1  : null;
        this.tax_amount  = order.tax_amount? parseFloat(order.tax_amount) : null;
        this.price_single_ttc  = order.price_single_ttc? parseFloat(order.price_single_ttc) : null;
        if(order.options){
            this.options = order.options.toString() !== '[null]' && order.options !== null ?
                Mix.castArrayAs(OrderOptionClient, JSON.parse(order.options.toString()))  :
                [];
        }
        if(this.campaign){
            if(this.campaign.orderPriorityEnable()) order.rankOrder = order.rank + 1;
        }
    }
}

export class OrderUtils {
    static initStructure(idStructure:string, structures:Structures):Structure{
        const structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure : new Structure() ;
    }

    static initNameStructure (idStructure: string, structures: Structures):string {
        let structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure.uai + '-' + structure.name : '' ;
    }

    static calculatePriceTTC( roundNumber?: number, order?:Order|OrderClient|OrderRegion):number|any {
        let price = parseFloat(Utils.calculatePriceTTC(order.price , order.tax_amount).toString());
        if (order.options !== undefined) {
            order.options.map((option) => {
                price += parseFloat(Utils.calculatePriceTTC(option.price , option.tax_amount).toString() );
            });
        }
        return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
    }
    static initParentOrder( order:Order):Object{
        if(!order)return;
        if(order.equipment) {
            return {
                amount: order.amount || 0,
                comment: order.comment || "",
                equipment: {
                    contract_type_name: order.equipment.contract_type_name || "",
                    name: order.equipment.name || "",
                },
                price_single_ttc: OrderUtils.findGoodPrice(order) || 0,
                rank: order.rank || 0,
            };
        } else {
            return  {
                amount : order.amount || 0,
                comment : order.comment || "",
                equipment : {
                    contract_type_name: "",
                    name: "",
                },
                price_single_ttc : OrderUtils.findGoodPrice(order) || 0,
                rank : order.rank || 0,
            };
        }
    }
    static findGoodPrice(order:Order):Number{
        if(order.price_single_ttc) return order.price_single_ttc;
        if(order.price_proposal) return order.price_proposal;
        if(order.price) return OrderUtils.calculatePriceTTC(2,order);
    }
}