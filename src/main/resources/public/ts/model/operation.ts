import { Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';
import {notify, _} from "entcore";



export class Operation implements Selectable {
    id?:number;
    id_label:number;
    label : label;
    status:boolean = false;
    Operations : any;
    bc_numbers:Array<any> ;
    programs : Array<any> ;
    contracts: Array<any> ;
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
        return {
            id_label : this.id_label,
            status : (this.status)
        };
    }

}

export class Operations extends Selection<Operation>{

    constructor() {
        super([]);
    }

    async sync() {
        let { data } = await http.get('/lystore/operations');
        this.all = Mix.castArrayAs(Operation, data);
        this.all.map( operation => {
            operation.label.toString() !== 'null' && operation.label !== null ?
                operation.label = Mix.castAs(label, JSON.parse(operation.label.toString()))
                : operation.label = new label();
            })
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