import http from 'axios';
import {notify} from 'entcore';
import {Mix} from 'entcore-toolkit';

export interface Project {
    id?: number;
    description: string;
    id_title: number;
    id_grade: number;
    building?: string;
    stair?: string;
    room?: string;
    site?: string;
}

export class Project {
    toJson() {
        return {
            description: this.description,
            id_title: this.id_title,
            id_grade: this.id_grade,
            building: this.building || null,
            stair: this.stair || null,
            room: this.room || null,
            site: this.site || null
        };
    }
}

export class Projects {
    all: Project[];

    constructor() {
        this.all = [];
    }

    async sync() {
        try {
            const {data} = await http.get('/lystore/titles');
            this.all = Mix.castArrayAs(Project, data);
        } catch (e) {
            console.error(e);
            notify.error('lystore.titles.sync.err');
        }
    }
}