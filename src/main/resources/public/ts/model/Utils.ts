export class Utils {

    static parsePostgreSQLJson (json: any): any {
        try {
            let res = JSON.parse(json);
            if (typeof res !== 'string') {
                return res;
            }
            return JSON.parse(res);
        } catch (e) {
            return '';
        }
    }

    static safeApply ($scope: any) {
        let phase = $scope.$root.$$phase;
        if (phase !== '$apply' && phase !== '$digest') {
            $scope.$apply();
        }
    }

    static formatKeyToParameter (values: any[], key: string): string {
        let params: string = '';
        values.map((value) => params += value.hasOwnProperty(key) ? `${key}=${value[key]}&` : '');
        return params.slice(0, -1);
    }
    static calculatePriceTTC = (price, tax_value, roundNumber?: number) => {
        let priceFloat = parseFloat(price);
        let taxFloat = parseFloat(tax_value);
        let price_TTC = (( priceFloat + ((priceFloat *  taxFloat) / 100)));
        return (!isNaN(price_TTC)) ? (roundNumber ? price_TTC.toFixed(roundNumber) : price_TTC ) : '';
    }

}