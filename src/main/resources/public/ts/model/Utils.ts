export class Utils {

    static safeApply ($scope: any) {
        let phase = $scope.$root.$$phase;
        if(phase !== '$apply' && phase !== '$digest') {
            $scope.$apply();
        }
    }
}