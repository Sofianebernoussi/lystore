import { _ } from 'entcore';
import http from 'axios';
import { Mix, Selectable, Selection } from 'entcore-toolkit';

export class Supplier implements Selectable{
    id: string;
    email: string;
    name: string;
    phone: string;
    address: string;

    selected: boolean;

    constructor (name?: string, email?: string, phone?: string, address?: string) {
        if (name) this.name = name;
        if (email) this.email = email;
        if (phone) this.phone = phone;
        if (address) this.address = address;

        this.selected = false;
    }

    toJson () {
        return {
            email: this.email,
            name: this.name,
            phone: this.phone,
            address: this.address
        }
    }

    async save (): Promise<void> {
        try {
            if (this.id) await this.update();
            else await this.create();
        } catch (e) {
            //TODO Que faire de l'erreur ? Gestion des codes erreurs ?
        }
    }

    async create (): Promise<void> {
        try {
            let supplier = await http.post(`/lystore/supplier`, this.toJson());
            this.id = supplier.data.id;
        } catch (e) {
            //TODO Que faire de l'erreur ? Gestion des codes erreurs ?
        }

    }

    async update (): Promise<void> {
        try {
            let supplier = await http.put(`/lystore/supplier/${this.id}`, this.toJson());
            let { name, phone, email, address } = supplier.data;
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.address = address;
        } catch (e) {
            //TODO Que faire de l'erreur ? Gestion des codes erreurs ?
        }
    }

    async delete (): Promise<void> {
        try {
            await http.delete(`/lystore/supplier?id=${this.id}`);
        } catch (e) {
            //TODO Que faire de l'erreur ? Gestion des codes erreurs ?
        }
    }

}

export class Suppliers extends Selection<Supplier> {

    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let agents = await http.get(`/lystore/suppliers`);
        this.all = Mix.castArrayAs(Supplier, agents.data);
    }

    async delete (suppliers: Supplier[]): Promise<void> {
        try {
            let filter = '';
            suppliers.map((supplier) => filter += `id=${supplier.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/lystore/supplier?${filter}`);
        } catch (e) {
            //TODO Gérer le cas en erreur
        }
    }
}