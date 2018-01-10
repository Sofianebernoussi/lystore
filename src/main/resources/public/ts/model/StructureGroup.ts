import {Structure, Tag} from "./index";
import {Selectable,Selection, Mix} from "entcore-toolkit";
import { notify } from 'entcore';
import http from 'axios';
/**
 * Created by rahnir on 27/12/2017.
 */

export class StructureGroup implements Selectable {
    id?: number;
    name: string;
    structures: Structure [];
    tags : Tag [];
    selected: boolean;

    constructor(id?, name?) {
        this.id = id;
        this.name = name || "";
        this.structures = [];
        this.tags =  [];
    }

    toJson() {
        return {
            id: this.id ,
            name :   this.name ,
            structure : this.structures,
            tags: this.tags.map((tag)=>{return tag.id})
        };
    }
}


export class StructureGroups extends Selection<StructureGroup> {

    constructor () {
        super([]);
    }
    async sync () {
        try {
            //let { data } = await http.get(`/lystore/structure/groups`);
            let data =  [new StructureGroup(1,'Particirants PPE'),new StructureGroup(2,'Lycées du 92'),new StructureGroup(3,'test name_group')];
             this.all = Mix.castArrayAs(StructureGroup, data) ;
           /* this.all.map((campaign) => {
                campaign.groups = JSON.parse(campaign.groups.toString());
                campaign.groups !== [null] && campaign.groups[0] !== null ? campaign.groups = Mix.castArrayAs(StructureGroup, campaign.groups) : campaign.groups = [];
            });*/
        } catch (e) {
            notify.error('lystore.campaign.sync.err');
        }
    }

}