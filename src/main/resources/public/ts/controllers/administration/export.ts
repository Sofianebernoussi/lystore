import {moment, ng} from "entcore";
import {Export, Exports, Utils} from "../../model";

declare let window: any;

export const exportCtrl = ng.controller('exportCtrl', [
    '$scope', async ($scope) => {

        $scope.exports = new Exports([]);
        await $scope.exports.getExports();


        $scope.getFormatedDate = (date) => {
            return moment(date).format("DD/MM/YYYY HH:mm:ss");
        };
        $scope.exports.all.map(exportT => {
            exportT.created = $scope.getFormatedDate(exportT.created)
        });
        Utils.safeApply($scope);

        $scope.getExport = (exportTemp: Export) => {
            window.location = `lystore/export/${exportTemp.fileid}`;

        };


    }
]);