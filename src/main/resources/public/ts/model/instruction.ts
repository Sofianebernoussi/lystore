import { Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';

export class Instruction implements Selectable {
    id?:number;
    object:string;
    exercice_number:number;
    cp_number:string;
    service_number:string;
    subject_to_cp:boolean;
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