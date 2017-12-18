import { Tag } from './index';
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
    tags: Tag[];

    selected: boolean;

    constructor (name?: string, price?: number) {
        if (name) this.name = name;
        if (price) this.price = price;
        this.tags = [];
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
            tags: this.tags.map((tag: Tag) => tag.id)
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
            this.all.map((equipment) => equipment.tags = JSON.parse(equipment.tags.toString()));
        } catch (e) {
            notify.error('lystore.equipment.sync.err');
        }
    }
}