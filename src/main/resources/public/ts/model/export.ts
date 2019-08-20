import http from "axios";
import {notify} from "entcore";
import {Mix, Selectable, Selection} from "entcore-toolkit";

export class Export implements Selectable {
    selected: boolean;
    filename: string;
    fileid: string;
    ownerid: string;
    id: Number;
    status: STATUS;
    constructor(){
        this.status = STATUS.WAITING;
    }
}

export class Exports extends Selection<Export> {
    async getExports() {
        try {
            let {data} = await http.get(`/lystore/exports`);
            let response = data.map( exportResponse => {
                let exportEdit = {...exportResponse};
                switch(exportResponse.status) {
                    case STATUS.WAITING:
                        exportEdit.classStatus =  "disableRow";
                        break;
                    case STATUS.SUCCESS:
                        exportEdit.classStatus =  "successRow";
                        break;
                    default:
                        exportEdit.classStatus = "errorRow";
                }
                return exportEdit;
            });
            this.all = Mix.castArrayAs(Export, response);
        } catch (e) {
            notify.error('lystore.instruction.create.err');
            throw e;
        }
    }

    async delete(idsExports) {
        try {
            console.log(idsExports)
            //await http.delete('/lystore/exports', { data: idsExports });
        } catch (e) {
            throw notify.error('lystore.export.delete.err');
        }
    }
}

export enum STATUS  {
    WAITING = "WAITING",
    SUCCESS = "SUCCESS",
    ERROR = "ERROR",
}