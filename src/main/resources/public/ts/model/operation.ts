import { Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';
import {notify, _} from "entcore";
import {Equipment} from "./Equipment";
import {Utils} from "./Utils";



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
    id_instruction: number;
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
            throw e;
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


    async getOrders() {
        try {
            const {data} = await http.get(`/lystore/operations/${this.id}/orders`);
            return data;
        } catch (e) {
            notify.error("lystore.operation.orders.sync.err");
            throw e;
        }
    }

    toJson(){
        return {
            id_label : this.id_label,
            status : this.status,
        };
    }

}

export class Operations extends Selection<Operation>{

    filters: Array<string>;

    constructor() {
        super([]);
        this.filters = [];
    }

    async sync() {
        try{
            const queriesFilter = Utils.formatGetParameters({q: this.filters});
            let { data } = await http.get(`/lystore/operations/?${queriesFilter}`);
            this.all = Mix.castArrayAs(Operation, data);
            this.all.map( operation => {
                operation.label.toString() !== 'null' && operation.label !== null ?
                    operation.label = Mix.castAs(label, JSON.parse(operation.label.toString()))
                    : operation.label = new label();
            })
        } catch(e){
            notify.error('lystore.operation.sync.err');
            throw e;
        }
    }

    async delete (){
        let operationsIds = this.selected.map(operation => operation.id);
        try{
            await http.delete('/lystore/operations', { data: operationsIds });
        } catch(err){
            notify.error('lystore.operation.delete.err');
        }
    }
    async updateOperations(id_instruction: number, operationIds: Array<number>){
        try {
            await http.put(`/lystore/operations/instructionAttribute/${id_instruction}`, operationIds);
        } catch (e) {
            notify.error('lystore.operation.update.err');
            throw e;
        }
    }
    async updateRemoveOperations(operationIds: Array<number>){
        try {
            await http.put('/lystore/operations/instructionRemove', operationIds);
        } catch (e) {
            notify.error('lystore.operation.update.err');
            throw e;
        }
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