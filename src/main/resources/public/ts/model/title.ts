import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from "axios";

export interface Title {
    id: number;
    name: string;
}

export class Title implements Selectable {
    selected: boolean;

    constructor(id?: number, name?: string) {
        if (id) {
            this.id = id;
        }
        if (name) {
            this.name = name;
        }
    }
}

export class Titles extends Selection<Title> {


    constructor() {
        super([]);
    }

    async sync(): Promise<void> {
        {
            let titles = await http.get(`/lystore/titles`);
            this.all = Mix.castArrayAs(Title, titles.data);

        }
    }
}