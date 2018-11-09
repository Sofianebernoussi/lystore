import {Tag, Utils} from './index';
import {_, notify} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';

export class Equipment implements Selectable {
    id?: number;
    name: string;
    summary: string;
    description: string;
    price: number;
    id_tax: number;
    id_contract: number;
    status: string;
    image: string;
    reference: string;
    search?: Equipment[];
    id_option?: number;
    technical_specs: TechnicalSpec[];
    tags: Tag[];
    tax_amount: number;
    selected: boolean;
    options: EquipmentOption[];
    deletedOptions?: EquipmentOption[];

    constructor (name?: string, price?: number) {
        if (name) this.name = name;
        if (price) this.price = price;
        this.technical_specs = [];
        this.tags = [];
        this.options = [];
    }

    toString(): string {
        return `${this.reference} - ${this.name}`;
    }

    toJson () {
        let optionList =  this.options.map((option: EquipmentOption) => option.toJson());
        return {
            name: this.name,
            summary: this.summary || null,
            description: this.description || null,
            price: parseFloat(this.price.toString()),
            id_tax: this.id_tax,
            status: this.status,
            warranty: 1,
            image: this.image || null,
            id_contract: this.id_contract,
            technical_specs: this.technical_specs.map((spec: TechnicalSpec) => spec.toJson()),
            tags: this.tags.map((tag: Tag) => tag.id),
            optionsCreate : _.filter(optionList, function(option) { return option.id === null ; }) ,
            optionsUpdate : _.filter(optionList, function(option) { return option.id !== null ; }) ,
            deletedOptions : this.deletedOptions || null,
        };
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
            await http.post(`/lystore/equipment`, this.toJson());
        } catch (e) {
            notify.error('lystore.equipment.create.err');
        }
    }

    async update () {
        try {
           await http.put(`/lystore/equipment/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.equipment.update.err');
            throw e;
        }
    }

    async delete () {
        try {
            await http.delete(`/lystore/equipment/${this.id}`);
        } catch (e) {
            notify.error('lystore.equipment.delete.err');
        }
    }

    async sync (id) {
        try {
            let { data } =  await http.get(`/lystore/equipment/${id}`);
            Mix.extend(this, data[0]);
            this.price = parseFloat(this.price.toString());
            this.tax_amount = parseFloat(this.tax_amount.toString());
            this.options.toString() !== '[null]' && this.options !== null ?
                this.options = Mix.castArrayAs(EquipmentOption, JSON.parse(this.options.toString()))
                : this.options = [];
        } catch (e) {
            notify.error('lystore.equipment.sync.err');
        }
    }
}

export class TechnicalSpec {
    name: string;
    value: string;

    toJson () {
        return {
            name: this.name,
            value: this.value
        };
    }
    toString () {
        return this.name + ' ' + this.value;
    }
}

export class Equipments extends Selection<Equipment> {

    constructor () {
        super([]);
    }

    async delete (equipments: Equipment[]): Promise<void> {
        try {
            let filter = '';
            equipments.map((equipment) => filter += `id=${equipment.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/lystore/equipment?${filter}`);
        } catch (e) {
            notify.error('lystore.equipment.delete.err');
        }
    }

    async sync (idCampaign?: number , idStructure?: string ) {
        try {
            let { data } = idCampaign ? await http.get(`/lystore/equipments/campaign/${idCampaign}?idStructure=${idStructure}`) : await http.get(`/lystore/equipments`) ;
            this.all = Mix.castArrayAs(Equipment, data);
            this.all.map((equipment) => {
                equipment.price = parseFloat(equipment.price.toString());
                equipment.tax_amount = parseFloat(equipment.tax_amount.toString());
                equipment.tags = equipment.tags !== null && equipment.tags.toString() !==  '[null]' ? JSON.parse(equipment.tags.toString()) : [];
                equipment.options.toString() !== '[null]' && equipment.options !== null ?
                    equipment.options = Mix.castArrayAs(EquipmentOption, JSON.parse(equipment.options.toString()))
                    : equipment.options = [];

            });
            this.all.map((equipment) =>
                equipment.technical_specs = equipment.technical_specs !== null
                    ? Mix.castArrayAs(TechnicalSpec, Utils.parsePostgreSQLJson(equipment.technical_specs.toString()))
                    : equipment.technical_specs);
        } catch (e) {
            notify.error('lystore.equipment.sync.err');
        }
    }

    async setStatus (status: string): Promise<void> {
        try {
            let params = Utils.formatKeyToParameter(this.selected, 'id');
            await http.put(`/lystore/equipments/${status}?${params}`);
        } catch (e) {
            notify.error('lystore.equipment.update.err');
            throw e;
        }
    }

    async search(text: String) {
        try {
            if (text.trim() === '' || !text) return;
            const {data} = await http.get(`/lystore/equipments/search?q=${text}`);
            return Mix.castArrayAs(Equipment, data);
        } catch (err) {
            notify.error('lystore.option.search.err');
            throw err;
        }
    }
}

export class EquipmentOption implements Selectable {

    id?: number;
    amount: number;
    required: boolean;
    selected: boolean;
    id_option: number;

    constructor () {
        this.amount = 1;
        this.required = true;
    }

    toJson () {
        return {
            id: this.id ? this.id : null,
            amount: parseInt(this.amount.toString()),
            required : this.required,
            id_option: this.id_option
        };
    }

}

export class EquipmentImporter {
    files: File[];
    message: string;
    id_contract?: number;

    constructor() {
        this.files = [];
    }

    isValid(): boolean {
        if (this.id_contract && this.id_contract >= 0) {
            return this.files.length > 0
                ? this.files[0].name.endsWith('.csv') && this.files[0].name.trim() !== ''
                : false;
        }
    }

    async validate(): Promise<any> {
        try {
            await this.postFile();
        } catch (err) {
            throw err;
        }
    }

    private async postFile(): Promise<any> {
        if (this.id_contract) {
            let formData = new FormData();
            formData.append('file', this.files[0], this.files[0].name);
            let response;
            try {
                response = await http.post(`/lystore/equipments/contract/${this.id_contract}/import`,
                    formData, {'headers': {'Content-Type': 'multipart/form-data'}});
                return response;
            } catch (err) {
                throw err.response.data;
            }
        } else throw new Error("lystore.equipment.import.contract");
    }
}