import http from 'axios';
import { notify, _ } from 'entcore';
import { Selectable, Selection, Mix } from 'entcore-toolkit';
import {StructureGroup,  Tags} from './index';


export class Campaign implements Selectable  {
    id?: number;
    name: string;
    description: string;
    image: string;
    accessible: boolean;
    groups: StructureGroup[];
    selected: boolean;
    nb_structures: number;
    nb_equipments: number;
    constructor (name?: string, description?: string) {
        if (name) this.name = name;
        if (description) this.description = description;
        this.groups = [];
        this.image = '';
    }

    toJson () {
        return {
            name: this.name,
            description: this.description || null,
            image: this.image || null,
            accessible: this.accessible || false,
            groups: this.groups.map((group) => {return group.toJson(); })
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
            await http.post(`/lystore/campaign`, this.toJson());
        } catch (e) {
            notify.error('lystore.campaign.create.err');
        }
    }

    async update () {
        try {
            await http.put(`/lystore/campaign/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.campaign.update.err');
        }
    }

    async delete () {
        try {
            await http.delete(`/lystore/campaign/${this.id}`);
        } catch (e) {
            notify.error('lystore.campaign.delete.err');
        }
    }
    async updateAccessibility() {
        try {
            await http.put(`/lystore/campaign/accessibility/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.campaign.update.err');
        }
    }
    async sync (id, tags?: Tags) {
        try {
            let { data } = await http.get(`/lystore/campaigns/${id}`);
            let groups = JSON.parse(data.groups.toString());
            if (groups[0] !== null ) {
                data.groups = Mix.castArrayAs(StructureGroup, groups) ;
                if (tags) {
                data.groups.map((group) => {
                    group.tags =  group.tags.map( (tag) => {
                        return _.findWhere(tags, {id: tag});
                    });
                });
                }
            } else data.groups = [];
            Mix.extend(this, Mix.castAs(Campaign, data) );

        } catch (e) {
            notify.error('lystore.campaign.sync.err');
        }
    }
}


export class Campaigns extends Selection<Campaign> {

    constructor () {
        super([]);
    }

    async delete (campaigns: Campaign[]): Promise<void> {
        try {
            let filter = '';
            campaigns.map((campaign) => filter += `id=${campaign.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/lystore/campaign?${filter}`);
        } catch (e) {
            notify.error('lystore.campaign.delete.err');
        }
    }

    async sync () {
        try {
            let { data } = await http.get(`/lystore/campaigns`);
            this.all = Mix.castArrayAs(Campaign, data);
        } catch (e) {
            notify.error('lystore.campaigns.sync.err');
        }
    }
}
