import { Tag, Utils } from './index';
import { notify } from 'entcore';
import { Selectable, Selection, Mix } from 'entcore-toolkit';
import http from 'axios';

export class Equipment implements Selectable {
    id?: number;
    name: string;
    summary: string;
    description: string;
    price: number;
    id_tax: number;
    id_contract: number;
    status: string;
    image: string;
    technical_specs: TechnicalSpec[];
    tags: Tag[];

    selected: boolean;
    options : EquipmentOption[];

    constructor (name?: string, price?: number) {
        if (name) this.name = name;
        if (price) this.price = price;
        this.technical_specs = [];
        this.tags = [];
        this.options= [];
    }

    toJson () {
        return {
            name: this.name,
            summary: this.summary || null,
            description: this.description || null,
            price: parseFloat(this.price.toString()),
            id_tax: this.id_tax,
            status: this.status,
            image: this.image || null,
            id_contract: this.id_contract,
            technical_specs: this.technical_specs.map((spec: TechnicalSpec) => spec.toJson()),
            tags: this.tags.map((tag: Tag) => tag.id),
            options : this.options.map((option: EquipmentOption) => option.toJson())
        }
    }

    async save () {
        if (this.id) {
            await this.update()
        } else {
            await this.create();
        }
    }

    async create () {
        try {
            await http.post(`/lystore/equipment`, this.toJson());
        } catch (e) {
            notify.error('lystore.equipment.create.err');
        }
    }

    async update () {
        try {
           await http.put(`/lystore/equipment/${this.id}`, this.toJson())
        } catch (e) {
            notify.error('lystore.equipment.update.err');
            throw e;
        }
    }

    async delete () {
        try {
            await http.delete(`/lystore/equipment/${this.id}`);
        } catch (e) {
            notify.error('lystore.equipment.delete.err');
        }
    }
}

export class TechnicalSpec {
    name: string;
    value: string;

    toJson () {
        return {
            name: this.name,
            value: this.value
        }
    }
}

export class Equipments extends Selection<Equipment> {

    constructor () {
        super([]);
    }

    async delete (equipments: Equipment[]): Promise<void> {
        try {
            let filter = '';
            equipments.map((equipment) => filter += `id=${equipment.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/lystore/equipment?${filter}`);
        } catch (e) {
            notify.error('lystore.equipment.delete.err');
        }
    }

    async sync () {
        try {
            let { data } = await http.get(`/lystore/equipments`);
            this.all = Mix.castArrayAs(Equipment, data);
            this.all.map((equipment) => {
                equipment.tags = JSON.parse(equipment.tags.toString());
                equipment.options = JSON.parse(equipment.options.toString()) ;
                equipment.options !== [null] && equipment.options[0] !== null ? equipment.options = Mix.castArrayAs(EquipmentOption, equipment.options) : equipment.options = [];
            });
            this.all.map((equipment) =>
                equipment.technical_specs = Mix.castArrayAs(TechnicalSpec, Utils.parsePostgreSQLJson(equipment.technical_specs.toString())));
        } catch (e) {
            notify.error('lystore.equipment.sync.err');
        }
    }
}

export class EquipmentOption   {

    id?: number;
    name: string;
    price: number;
    amount: number;
    required: boolean;
    id_tax: number;

    constructor () {
        this.name ="";
        this.amount = 1;
        this.required = false;

    }

    toJson () {
        return {
            name: this.name,
            price: parseFloat(this.price.toString()),
            amount: parseInt(this.price.toString()),
            required : this.required,
            id_tax: this.id_tax
        }
    }

}