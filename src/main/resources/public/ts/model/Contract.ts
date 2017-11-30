import { moment } from 'entcore';
import { Selection, Selectable, Mix } from 'entcore-toolkit';
import http from 'axios';

export class Contract implements Selectable {
    id?: number;
    name: string;
    reference: string;
    annual_min: number | string;
    annual_max: number | string;
    start_date: string | Date;
    nb_renewal: string;
    id_contract_type: number;
    max_brink: number | string;
    id_supplier: number;
    id_agent: number;
    id_program: number;
    end_date: string;
    renewal_end: string;

    selected: false;
    annual_min_enabled: boolean;
    annual_max_enabled: boolean;
    max_brink_enabled: boolean;

    constructor (name?: string, reference?: string, annual_min?: number, annual_max?: number,
                 start_date?: string | Date, max_brink?: number) {
        if (name) this.name = name;
        if (reference) this.reference = reference;
        if (annual_min) this.annual_min = annual_min;
        this.annual_min_enabled = this.annual_min !== undefined;
        if (annual_max) this.annual_max = annual_max;
        this.annual_max_enabled = this.annual_max !== undefined;
        if (start_date) this.start_date = start_date;
        if (max_brink) this.max_brink = max_brink;
        this.max_brink_enabled = this.max_brink !== undefined;
    }

    toJson() {
        return {
            name: this.name,
            reference: this.reference,
            start_date: moment(this.start_date).format('YYYY-MM-DD'),
            nb_renewal: parseInt(this.nb_renewal),
            id_contract_type: this.id_contract_type,
            id_supplier: this.id_supplier,
            id_agent: this.id_agent,
            end_date: moment(this.start_date).add(1, 'y').format('YYYY-MM-DD'),
            renewal_end: moment(this.end_date).add(this.nb_renewal, 'y').format('YYYY-MM-DD'),
            annual_min: this.annual_min_enabled ? parseFloat(this.annual_min.toString()) : null,
            annual_max: this.annual_max_enabled ? parseFloat(this.annual_max.toString()) : null,
            max_brink: this.max_brink_enabled ? parseFloat(this.max_brink.toString()) : null,
            id_program: this.id_program || null
        };
    }

    async save (): Promise<void> {
        try {
            if (this.id) {
                await this.update();
            } else {
                await this.create();
            }
        } catch (e) {
            throw e;
        }
    }

    async create (): Promise<void> {
        try {
            let res = await http.post(`/lystore/contract`, this.toJson());
            this.id = res.data.id;
        } catch (e) {
            //TODO lancer notification
            throw e;
        }
    }

    async update (): Promise<void> {
        try {
            await http.put(`/lystore/contract/${this.id}`, this.toJson());
        } catch (e) {
            //TODO Lancer notification
            throw e;
        }
    }

    async delete (): Promise<void> {
        try {
            await http.delete(`/lystore/contract?id=${this.id}`);
        } catch (e) {
            //TODO Lancer notification
            throw e;
        }
    }
}

export class Contracts extends Selection<Contract> {

    constructor () {
        super([]);
    }

    async sync () {
        let contracts = await http.get(`/lystore/contracts`);
        this.all = Mix.castArrayAs(Contract, contracts.data);
    }

    async delete (contracts: Contract[]): Promise<void> {
        try {
            let filter = '';
            contracts.map((contract) => filter += `id=${contract.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/lystore/contract?${filter}`);
        } catch (e) {
            //TODO Gérer le cas en erreur
        }
    }
}