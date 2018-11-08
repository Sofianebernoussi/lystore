import http from 'axios';
import {Mix} from 'entcore-toolkit';
import {notify} from 'entcore';

export interface Grade {
    id: number;
    name: string;
}

export class Grade {
    constructor(id: number = null, name: string = '') {
        this.id = id;
        this.name = name;
    }
}

export class Grades {
    all: Grade[];

    constructor() {
        this.all = [];
    }

    async sync() {
        try {
            const {data} = await http.get('/lystore/grades');
            this.all = Mix.castArrayAs(Grade, data);
        } catch (e) {
            console.error(e);
            notify.error('lystore.grades.sync.err');
        }
    }

}

