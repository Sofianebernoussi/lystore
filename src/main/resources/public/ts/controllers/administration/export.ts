import {moment, ng, template} from "entcore";
import {Export, Exports, Notification, Utils, STATUS} from "../../model";

declare let window: any;

export const exportCtrl = ng.controller('exportCtrl', [
    '$scope', async ($scope) => {
        $scope.display = {
            delete: false
        };
        $scope.exports = new Exports([]);
        $scope.STATUS = STATUS;
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
            if(exportTemp.status === STATUS.SUCCESS){
                window.location = `lystore/export/${exportTemp.fileid}`;
            }
        };

        $scope.confirmDelete = () => {
                $scope.display.delete = true;
                $scope.exportsToDelete = $scope.exports.all.filter(exportFiltered => exportFiltered.selected && exportFiltered.status !== STATUS.WAITING);
                template.open('export.delete.lightbox', 'administrator/exports/export-lightbox-delete');
        };

        $scope.cancelExportLightbox = () => {
            $scope.exportToDelete = new Export();
            $scope.display.delete = false;
            template.close('export.delete.lightbox');
            Utils.safeApply($scope);

        };

        $scope.deleteExport = async () => {
            await $scope.exports.delete( $scope.exportsToDelete.map(exportMap => exportMap.id));
            $scope.isAllExportSelected = false;
            $scope.display.delete = false;
            template.close('export.delete.lightbox');
            $scope.notifications.push(new Notification('lystore.delete.notif', 'confirm'));
            $scope.exportToDelete = [];
            await $scope.exports.getExports();
            $scope.updateDate()
        };

        $scope.isAllExportSelected = false;
        $scope.switchAllExports = () => {
            $scope.isAllExportSelected  =  !$scope.isAllExportSelected;
            if ( $scope.isAllExportSelected) {
                $scope.exports.all.map(exportSelected => exportSelected.selected = true)
            } else {
                $scope.exports.all.map(exportSelected => exportSelected.selected = false)
            }
            Utils.safeApply($scope);
        };

        $scope.updateDate();
    }
]);