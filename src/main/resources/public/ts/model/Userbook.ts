import http from 'axios';

export class Userbook {
    id: number;
    name: string;


    putPreferences(preferences){
        http.put('/userbook/preference/lystore',preferences);
    }

    async getPreferences(){
         let data = await http.get('/userbook/preference/lystore');
         return data.data;
    }
}