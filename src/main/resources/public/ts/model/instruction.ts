import { Mix, Selectable, Selection} from 'entcore-toolkit';
import { notify} from 'entcore';
import http from 'axios';
import {label, Operation} from "./operation";
import {Utils} from "./Utils";

export class Instruction implements Selectable {
    id?:number;
    object:string;
    id_exercise:number;
    exercise_years:ExerciseNumber;
    cp_number:string;
    service_number:string;
    submitted_to_cp:boolean;
    date_cp: object;
    comment:string;
    amount:number;
    operations:Array<Operation> = [];
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
            submitted_to_cp: this.submitted_to_cp,
            date_cp: this.date_cp,
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

export class ExerciseNumber {
    id: number;
    year_exercise: string;
}
export class ExerciseNumbers{
    all:Array<ExerciseNumber>;
    constructor() {
        this.sync();
    }
   async sync() {
        try{
            let {data} = await http.get('/lystore/exercises');
            this.all = Mix.castArrayAs(ExerciseNumber, data);
        } catch (e) {
            notify.error('lystore.exercise.err');
        }
   }
}