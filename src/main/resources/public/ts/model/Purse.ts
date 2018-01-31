import { notify } from 'entcore';
import http from 'axios';
import {Mix, Selectable, Selection} from "entcore-toolkit";

export class Purse implements Selectable {
    id?: number;
    id_structure: string;
    amount: number;
    id_campaign: number;

    selected: boolean;

    constructor (id_structure?: string, amount?: number, id_campaign?: number) {
        if (id_structure) this.id_structure = id_structure;
        if (amount) this.amount = amount;
        if (id_campaign) this.id_campaign = id_campaign;

        this.selected = false;
    }

    async save (): Promise<void> {
        try {
            let purse = await http.put(`/lystore/purse/${this.id}`, this.toJson());
            let { amount } = purse.data;
            this.amount = amount;
        } catch (e) {
            notify.error('lystore.purse.update.err');
        }
    }

    toJson () {
        return {
            id_structure: this.id_structure,
            amount: this.amount,
            id_campaign: this.id_campaign
        };
    }
}

export class Purses extends Selection<Purse> {
    all: Purse[];

    constructor () {
        super([]);
        this.all = [];
    }

    async sync (id_campaign): Promise<void> {
        let purses = await http.get(`/lystore/campaign/${id_campaign}/purses/list`);
        this.all = Mix.castArrayAs(Purse, purses.data);
    }
}

export class PurseImporter {
    files: File[];
    id_campaign: number;
    message: string;

    constructor (id_campaign: number) {
        this.files = [];
        this.id_campaign = id_campaign;
    }

    isValid(): boolean {
        return this.files.length > 0
            ? this.files[0].name.endsWith('.csv') && this.files[0].name.trim() !== ''
            : false;
    }

    async validate(): Promise<any> {
        try {
            await this.postFile();
        } catch (err) {
            throw err;
        }
    }

    private async postFile(): Promise<any> {
        let formData = new FormData();
        formData.append('file', this.files[0], this.files[0].name);
        let response;
        try {
            response = await http.post(`/lystore/campaign/${this.id_campaign}/purses/import`,
                formData, {'headers' : { 'Content-Type': 'multipart/form-data' }});
        } catch (err) {
            throw err.response.data;
        }
        return response;
    }
}