import http from 'axios';
import { notify } from 'entcore';
import { Mix, Selection, Selectable } from 'entcore-toolkit';
import { TAG_COLORS } from './index';

export class Tag implements Selectable {
    selected: boolean;
    id?: number;
    name: string;

    color: string;
    nb_equipments: number;

    constructor (name = '', color?: string) {
        this.name = name;
        this.color = color || TAG_COLORS[0];
    }

    toJson () {
        return {
            name: this.name,
            color: this.color
        }
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
            await http.post(`/lystore/tag`, this.toJson());
        } catch (e) {
            notify.error('lystore.tag.create.err');
        }
    }

    async update () {
        try {
            await http.put(`/lystore/tag/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.tag.update.err');
        }
    }

    async delete () {
        try {
            await http.delete(`/lystore/tag/${this.id}`);
        } catch (e) {
            notify.error('lystore.tag.delete.err');
        }
    }
}

export class Tags extends Selection<Tag> {

    colors: string[];

    constructor () {
        super([]);
        this.colors = TAG_COLORS;
    }

    async sync () {
        let tags = await http.get(`/lystore/tags`);
        this.all = Mix.castArrayAs(Tag, tags.data);
    }

    async delete (tags: Tag[]): Promise<void> {
        try {
            let filter = '';
            tags.map((tag) => filter += `id=${tag.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/lystore/tag?${filter}`);
        } catch (e) {
            notify.error('lystore.tag.delete.err');
        }
    }
}