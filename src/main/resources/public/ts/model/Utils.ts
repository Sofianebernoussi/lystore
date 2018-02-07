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
}