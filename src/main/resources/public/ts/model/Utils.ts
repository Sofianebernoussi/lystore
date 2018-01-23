export class Utils {

    static parsePostgreSQLJson (json: any): any {
        let res = JSON.parse(json);
        if (typeof res !== 'string') {
            return res;
        }
        return JSON.parse(res);
    }

    static safeApply ($scope: any) {
        let phase = $scope.$root.$$phase;
        if (phase !== '$apply' && phase !== '$digest') {
            $scope.$apply();
        }
    }
}