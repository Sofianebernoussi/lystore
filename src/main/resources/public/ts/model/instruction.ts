import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {moment, notify} from 'entcore';
import http from 'axios';
import {label, Operation} from "./operation";
import {Utils} from "./Utils";
import {OrderClient} from "./OrderClient";

// import {window} from "../controllers/administration/orders";

export class Instruction implements Selectable {
    id?:number;
    object:string;
    id_exercise:number;
    exercise:Exercise;
    cp_number:string;
    service_number:string;
    submitted_to_cp:boolean = false;
    date_cp: Date;
    comment:string;
    amount:number;
    operations:Array<Operation> ;
    selected:boolean = false;

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
            let {data : {id} } = await http.post(`/lystore/instruction`, this.toJson());
            this.id = id;
        } catch (e) {
            notify.error('lystore.instruction.create.err');
            throw e;
        }
    }

    async update () {
        try {
            await http.put(`/lystore/instruction/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.instruction.update.err');
            throw e;
        }
    }

    toJson(){
        return {
            id_exercise: this.id_exercise,
            object: this.object,
            service_number: this.service_number,
            cp_number: this.cp_number,
            submitted_to_cp: this.submitted_to_cp? true : false,
            date_cp: Utils.formatDatePost(this.date_cp),
            comment: this.comment,
        };
    }

    async getOperations(idOperation) {
        try {
            const {data} = await http.get(`/lystore/instruction/${idOperation}/operations`);
            this.operations = Mix.castArrayAs(Operation, data);
            this.operations.forEach(operation => {
                operation.label.toString() !== 'null' && operation.label !== null ?
                    operation.label = Mix.castAs(label, JSON.parse(operation.label.toString()))
                    : operation.label = new label();
                    operation.status = operation.status? true : false;
                });
        } catch (e) {
            notify.error("lystore.instruction.get.err");
            throw e;
        }
    }

    s2ab(s) {
        var buf = new ArrayBuffer(s.length);
        var view = new Uint8Array(buf);
        for (var i = 0; i != s.length; ++i) view[i] = s.charCodeAt(i) & 0xFF;
        return buf;
    }

    async exportRME() {
        window.location.href = `/lystore/instructions/${this.id}/export`;

        // try {
        //     let {data, status, headers} = await http.get(`/lystore/instructions/${this.id}/export`);
        //     if (status === 200) {
        //         var blob = new Blob([data]);
        //         var objectUrl = URL.createObjectURL(blob);
        //         var a = document.createElement("a");
        //         a.href = objectUrl;
        //         a.download = headers().filename;
        //         a.click();
        //         setTimeout(function () {
        //             document.body.removeChild(a);
        //             window.URL.revokeObjectURL(a.href);
        //         }, 100);
        //     }
        // } catch (e) {
        //     console.log(e)
        //     notify.error('lystore.instruction.exportRME.err');
        // }
    }

}

export class Instructions extends Selection<Instruction>{
    filters: Array<string>;
    constructor() {
        super([]);
        this.filters = [];
    }

    async sync () {
        try{
            this.all = [];
            const queriesFilter = Utils.formatGetParameters({q: this.filters});
            let {data} = await http.get(`/lystore/instructions/?${queriesFilter}`);
            this.all = Mix.castArrayAs(Instruction, data);
            this.all.forEach(instructionGet => {
                instructionGet.exercise = Mix.castAs(Exercise, JSON.parse(instructionGet.exercise.toString()));
                instructionGet.date_cp = moment(instructionGet.date_cp);
            })
        } catch (e) {
            notify.error('lystore.instruction.get.err');
        }
    }
    async delete (){
        let instructionIds = this.selected.map(instruction => instruction.id);
        try{
            await http.delete('/lystore/instructions', { data: instructionIds });
        } catch(err){
            notify.error('lystore.instruction.delete.err');
        }
    }
}

export class Exercise {
    id: number;
    year_exercise: string;
}
export class Exercises{
    all:Array<Exercise>;
    constructor() {
        this.sync();
    }
    async sync() {
        try{
            let {data} = await http.get('/lystore/exercises');
            this.all = Mix.castArrayAs(Exercise, data);
        } catch (e) {
            notify.error('lystore.exercise.err');
        }
    }
}