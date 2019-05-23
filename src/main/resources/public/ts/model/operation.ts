import { Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';

export class Operation implements Selectable {
    id?:number;
    id_label:number;
    status:boolean;

    nbr_sub: number;
    amount: number;
    selected:boolean;
    constructor(){

    }

}

export class Operations extends Selection<Operation>{

    constructor() {
        super([]);
    }

}