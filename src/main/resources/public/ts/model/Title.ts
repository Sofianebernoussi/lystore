import http from 'axios';
import {Mix} from 'entcore-toolkit';
import {notify} from 'entcore';

export interface Title {
    id: number;
    name: string;
}

export class Title {
    constructor(id: number = null, name: string = '') {
        this.id = id;
        this.name = name;
    }
}

export class Titles {
    all: Title[];

    constructor() {
        this.all = [];
    }

    async sync() {
        try {
            const {data} = await http.get('/lystore/titles');
            this.all = Mix.castArrayAs(Title, data);
        } catch (e) {
            console.error(e);
            notify.error('lystore.titles.sync.err');
        }
    }
}