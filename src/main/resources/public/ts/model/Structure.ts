import { model } from 'entcore';
import { Selectable, Mix, Selection } from 'entcore-toolkit';
import http from 'axios';

export class Structure implements Selectable {
    id: string;
    name: string;
    uai: string;
    city: string;

    selected: boolean;

    constructor (name?: string, uai?: string, city?: string) {
        this.name = name;
        this.uai = uai;
        this.city = city;
        this.selected = false;
    }

    toJson () {
        return {
            id: this.id,
            name: this.name,
            uai: this.uai,
            city: this.city
        };
    }

}

export class Structures  extends Selection<Structure> {


    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let {data} = await http.get(`/lystore/structures`);
        this.all = Mix.castArrayAs(Structure, data);
    }

}