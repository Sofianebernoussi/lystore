import {moment, ng, template} from "entcore";
import {Export, Exports, Notification, Utils} from "../../model";

declare let window: any;

export const exportCtrl = ng.controller('exportCtrl', [
    '$scope', async ($scope) => {
        $scope.display = {
            delete: false
        };
        $scope.exports = new Exports([]);
        await $scope.exports.getExports();


        $scope.getFormatedDate = (date) => {
            return moment(date).format("DD/MM/YYYY HH:mm:ss");
        };
        $scope.updateDate = () => {
            $scope.exports.all.map(exportT => {
                exportT.created = $scope.getFormatedDate(exportT.created)
            });
            Utils.safeApply($scope);
        };
        $scope.getExport = (exportTemp: Export) => {
            window.location = `lystore/export/${exportTemp.fileid}`;

        };

        $scope.confirmDelete = (exportToDelete: Export) => {
            $scope.display.delete = true;
            $scope.exportToDelete = exportToDelete;
            template.open('export.delete.lightbox', 'administrator/exports/export-lightbox-delete');
        };

        $scope.cancelExportLightbox = () => {
            $scope.exportToDelete = new Export();
            $scope.display.delete = false;
            template.close('export.delete.lightbox');
            Utils.safeApply($scope);

        };

        $scope.deleteExport = async (exportToDelete: Export) => {
            await exportToDelete.delete();
            $scope.display.delete = false;
            template.close('export.delete.lightbox');
            $scope.notifications.push(new Notification('lystore.delete.notif', 'confirm'));
            $scope.exportToDelete = new Export();
            await $scope.exports.getExports();
            $scope.updateDate()
        };
        $scope.updateDate();
    }
]);