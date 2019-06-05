import { Mix, Selectable, Selection} from 'entcore-toolkit';
import { moment, notify} from 'entcore';
import http from 'axios';
import {label, Operation} from "./operation";

export class Instruction implements Selectable {
    id?:number;
    object:string;
    id_exercise:number;
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

}

export class Instructions extends Selection<Instruction>{

    constructor() {
        super([]);
    }

    async sync () {

    }
}

export class ExerciseNumber {
    id: number;
    title: string;
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