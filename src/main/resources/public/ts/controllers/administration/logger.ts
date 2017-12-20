import { ng } from 'entcore';
import { Utils } from '../../model';

export const loggerController = ng.controller('loggerController', [
    '$scope', async ($scope) => {
        $scope.currentPage = 0;
        $scope.JSON = JSON;

        $scope.formatJson = (json: string, indent: number = 0) => {
            let value = JSON.stringify(JSON.parse(JSON.parse(json)), undefined, indent);
            return value !== 'null' ? value : '';
        };

        $scope.loadMoreLogs = async () => {
            await $scope.logs.loadPage(++$scope.currentPage);
            Utils.safeApply($scope);
        };

        await $scope.logs.loadPage($scope.currentPage);
        Utils.safeApply($scope);
    }
]);