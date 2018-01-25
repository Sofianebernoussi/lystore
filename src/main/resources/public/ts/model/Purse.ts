import http from 'axios';

export class Purse {
    id?: number;
    id_structure: string;
    amount: number;
    id_campaign: number;
}

export class Purses {
    all: Purse[];

    constructor () {
        this.all = [];
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