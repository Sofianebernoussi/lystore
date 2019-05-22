import { Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';

export class Instruction implements Selectable {
    id?:number;
    object:string;
    exercice_number:number|ExerciceNumber;
    cp_number:string;
    service_number:string;
    submitted_to_cp:boolean;
    date_cp: object;
    comment:string;
    amount:number;
    operations:Array<object>;

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

export class ExerciceNumber {
    id: number;
    title: string;
}
export class ExerciceNumbers {
   async sync() {

   }
}