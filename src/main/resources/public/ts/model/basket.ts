import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {_, notify} from 'entcore';
import http from 'axios';
import {Equipment, EquipmentOption, Structure} from './index';

export class Basket implements Selectable {
    id?: number;
    amount: number;
    processing_date: string| Date;
    equipment: Equipment ;
    options: EquipmentOption[];
    id_campaign: number;
    id_structure: string;
    selected: boolean;
    comment?: string;
    price_proposal?: number;
    price_editable: boolean;
    display_price_editable: boolean;


    constructor (equipment: Equipment , id_campaign: number, id_structure: string ) {
        this.equipment = Mix.castAs(Equipment, equipment) ;
        this.id_campaign = id_campaign;
        this.id_structure = id_structure;
        this.amount = 1;
        this.display_price_editable = false;
    }

    toJson () {
        let options = _.filter( this.equipment.options , function(option) { return option.required || option.selected ; });
        return {
            amount: this.amount,
            processing_date : this.processing_date,
            equipment : this.equipment.id,
            options: options.length > 0 ?  _.pluck( options , 'id') : null ,
            id_campaign : this.id_campaign,
            id_structure : this.id_structure
        };
    }

    async save () {
        if (this.id) {
            this.update();
        } else {
            this.create();
        }
    }

    async create () {
        try {
            return await  http.post(`/lystore/basket`, this.toJson());
        } catch (e) {
            notify.error('lystore.basket.create.err');
        }
    }

    async update () {
        try {
            http.put(`/lystore/basket/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.basket.update.err');
            throw e;
        }
    }
    async updateAmount () {
        try {
            http.put(`/lystore/basket/${this.id}/amount`, this.toJson());
        } catch (e) {
            notify.error('lystore.basket.update.err');
            throw e;
        }
    }
    async updateComment(){
        try{
            http.put(`/lystore/basket/${this.id}/comment`, { comment: this.comment });
        }catch (e){
            notify.error('lystore.basket.update.err');
            throw e;
        }
    }

    async updatePriceProposal() {
        try {
            http.put(`/lystore/basket/${this.id}/priceProposal`, {price_proposal: this.price_proposal});
        } catch (e) {
            notify.error('lystore.basket.update.err');
            throw e;
        }
    }

    async delete () {
        try {
            return await  http.delete(`/lystore/basket/${this.id}`);
        } catch (e) {
            notify.error('lystore.basket.delete.err');
        }
    }


}

export class Baskets extends Selection<Basket> {
    constructor() {
        super([]);
    }

    async sync (idCampaign: number , idStructure: string ) {
        try {
            let { data } = await http.get(`/lystore/basket/${idCampaign}/${idStructure}`);
            this.all = Mix.castArrayAs(Basket, data);
            this.all.map((basket) => {
                basket.equipment = Mix.castAs(Equipment, JSON.parse(basket.equipment.toString())[0]);
                basket.options.toString() !== '[null]' && basket.options !== null ?
                    basket.options = Mix.castArrayAs(EquipmentOption, JSON.parse(basket.options.toString()))
                    : basket.options = [];
                basket.equipment.options = basket.options;
                basket.equipment.options.map((option) => option.selected = true);
            });
        } catch (e) {
            notify.error('lystore.basket.sync.err');
        }
    }
    async takeOrder (idCampaign: number , Structure: Structure ) {
        try {
            return await http.post(`/lystore/baskets/to/orders`, {
                id_campaign: idCampaign,
                id_structure: Structure.id,
                structure_name: Structure.name
            });
        } catch (e) {
            notify.error('lystore.order.create.err');
        }
    }
}