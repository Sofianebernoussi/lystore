import {Eventer, Mix, Selectable, Selection} from "entcore-toolkit";
import {Grade, Grades, Title, Titles} from './index';
import http from "axios";
import {notify} from "entcore";

export class Project implements Selectable {
    selected: boolean;
    id: number;
    description?: string;
    title: Title;
    grade: Grade;
    building?: string;
    stair?: number;
    room?: string;
    name: string;
    site?: string;
    titles: Titles;
    grades: Grades;
    eventer: Eventer;

    constructor() {
        if (this.title) {
            this.title = Mix.castAs(Title, JSON.parse(this.title.toString()));
        }
        if (this.grade) {
            this.grade = Mix.castAs(Grade, JSON.parse(this.grade.toString()));
        }
        this.grades = new Grades();
        this.titles = new Titles();
        this.eventer = new Eventer();
    }

    async init() {
        this.eventer.trigger('init:start');
        await this.titles.sync();
        await this.grades.sync();
        if (!this.grade) {
            this.grade = this.grades.all[0];
        }
        if (!this.title) {
            this.title = this.titles.all[0];
        }
        this.eventer.trigger('init:end');
    }

    async

    toJson() {
            

        return {
            id: this.id,
            description: this.description,
            name: this.name,
            id_title: this.title.id,
            id_grade: this.grade.id,
            building: this.building,
            site: this.site,
            stair: this.stair,
            room: this.room,
        };
    }

    async create() {
        try {
            let id_project = await  http.post(`/lystore/project`, this.toJson());
            this.id = (id_project.data["id"]);
            this.eventer.trigger('create:end');
            return id_project;

        } catch (e) {
            notify.error('lystore.project.create.err');
        }
    }
}


export class Projects extends Selection<Project> {
    constructor() {
        super([]);
    }

    async sync(): Promise<void> {
        {
            let projects = await http.get(`/lystore/projects`);
            this.all = Mix.castArrayAs(Project, projects.data);

        }
    }
}

