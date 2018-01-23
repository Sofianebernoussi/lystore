import { model } from 'entcore';

export class Structure {
    id: string;
    name: string;
    uai: string;

    constructor (name: string, uai: string) {
        this.name = name;
        this.uai = uai;
    }
}

export class Structures {
    all: Structure[];

    constructor () {
        this.all = [];
    }

   async sync () {
        // TODO Changer l'implémentation de la synchronisation lorsque le référentiel des établissements sera intégré
        for (let i = 0; i < model.me.structures.length; i++) {
            this.all.push(new Structure(model.me.structureNames[i], ''));
        }
    }

}