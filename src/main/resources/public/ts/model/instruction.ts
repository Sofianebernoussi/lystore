import { Mix, Selectable, Selection} from 'entcore-toolkit';
import {moment, notify} from 'entcore';
import http from 'axios';
import {label, Operation} from "./operation";
import {Utils} from "./Utils";

export class Instruction implements Selectable {
    id?:number;
    object:string;
    id_exercise:number;
    exercise:Exercise;
    cp_number:string;
    service_number:string;
    submitted_to_cp:boolean = false;
    date_cp: object;
    comment:string;
    amount:number;
    operations:Array<Operation> = [];
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
            await http.post(`/lystore/instruction`, this.toJson());
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
            cp_number: this.service_number,
            submitted_to_cp: this.submitted_to_cp? true : false,
            date_cp: moment(this.date_cp).format('YYYY-MM-DD'),
            comment: this.comment,
        };
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
            const queriesFilter = Utils.formatGetParameters({q: this.filters});
            let {data} = await http.get(`/lystore/instructions/?${queriesFilter}`);
            this.all = Mix.castArrayAs(Instruction, data);
            this.all.forEach(instructionGet => {
                instructionGet.exercise = Mix.castAs(Exercise, JSON.parse(instructionGet.exercise.toString()));
                instructionGet.operations.toString() !== "[null]" ?
                    instructionGet.operations = JSON.parse(instructionGet.operations.toString()):
                    instructionGet.operations = [];
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