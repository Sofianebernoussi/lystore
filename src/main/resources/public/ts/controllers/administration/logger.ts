import { ng } from 'entcore';
import { Utils } from '../../model';

export const loggerController = ng.controller('loggerController', [
    '$scope', async ($scope) => {
        $scope.currentPage = 0;
        $scope.JSON = JSON;
        $scope.display = {
            lightbox: false
        };

        $scope.formatJson = (json: string, indent: number = 0) => {
            let value = JSON.stringify(Utils.parsePostgreSQLJson(json), undefined, indent);
            return value !== 'null' ? value : '';
        };

        $scope.parseJson = (json: string) => Utils.parsePostgreSQLJson(json);

        $scope.loadMoreLogs = async () => {
            await $scope.logs.loadPage(++$scope.currentPage);
            Utils.safeApply($scope);
        };

        $scope.showFile = (log: Log) => {
            $scope.log = log;
            $scope.display.lightbox = true;
        };

        await $scope.logs.loadPage($scope.currentPage);
        Utils.safeApply($scope);
    }
]);