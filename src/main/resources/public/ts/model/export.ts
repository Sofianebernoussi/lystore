import http from "axios";
import {notify} from "entcore";
import {Mix, Selectable, Selection} from "entcore-toolkit";

export class Export implements Selectable {
    selected: boolean;
    filename: string;
    fileid: string;
    ownerid: string;


    async delete() {
        try {
            await http.delete(`/lystore/export/${this.fileid}`);
        } catch (e) {
            notify.error('lystore.export.delete.err')
        }

    }
}

export class Exports extends Selection<Export> {
    async getExports() {
        try {
            let {data} = await http.get(`/lystore/exports`);
            this.all = Mix.castArrayAs(Export, data);
        } catch (e) {
            notify.error('lystore.instruction.create.err');
            throw e;
        }
    }

}