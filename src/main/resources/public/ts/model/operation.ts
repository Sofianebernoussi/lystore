import { Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';
import {notify} from "entcore";

export class Operation implements Selectable {
    id?:number;
    id_label:number;
    status:boolean;

    nbr_sub: number;
    amount: number;
    selected:boolean;
    constructor(){

    }
    async save () {
        if (this.id) {
            await this.update();
        } else {
            await this.create();
        }
    }

    async create () {
        try {
            await http.post(`/lystore/operation`, this.toJson());
        } catch (e) {
            notify.error('lystore.operation.create.err');
        }
    }

    async update () {
        try {
            await http.put(`/lystore/operation/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.operation.update.err');
            throw e;
        }
    }

    toJson(){
        return this;
    }

}

export class Operations extends Selection<Operation>{

    constructor() {
        super([]);
    }

}

export class label implements Selectable{
    id: number;
    title: string;
    selected: boolean;
}
export class labels extends Selection<label>{

    constructor() {
        super([]);
    }

    async sync() {
        let { data } = await http.get('/lystore/labels');
        this.all = Mix.castArrayAs(label, data);
    }
}