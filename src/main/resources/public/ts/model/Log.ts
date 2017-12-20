import http from 'axios';
import { notify } from 'entcore';
import { Mix } from 'entcore-toolkit';

export class Log {
    id: number;
    date: string;
    action: string;
    context: string;
    value: any;
    id_user: string;
    username: string;
    item: string;
    selected: boolean;
}

export class Logs {
    all: Log[];

    constructor () {
        this.all = [];
    }

    async loadPage (pageNumber: number = 0) {
        try {
            let { data } = await http.get(`/lystore/logs?page=${pageNumber}`);
            this.all = [...this.all, ...Mix.castArrayAs(Log, data)];
            this.all.map((log) => log.selected = false)
        } catch (e) {
            notify.error('lystore.logs.sync.err');
        }
    }

    export () {
        location.replace(`/lystore/logs/export`);
    }
}