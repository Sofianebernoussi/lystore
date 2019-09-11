import {moment, ng, template} from "entcore";
import {Export, Notification, Utils, STATUS} from "../../model";

declare let window: any;

export const exportCtrl = ng.controller('exportCtrl', [
    '$scope', async ($scope) => {
        $scope.display = {
            delete: false
        };
        $scope.sort = {
            export : {
                type: 'created',
                reverse: true
            }
        };
        $scope.STATUS = STATUS;

        $scope.getExport = (exportTemp: Export) => {
            if(exportTemp.status === STATUS.SUCCESS){
                window.location = `lystore/export/${exportTemp.fileId}`;
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

        $scope.deleteExport = async ():Promise<void> => {
            await $scope.exports.delete( $scope.exportsToDelete
                    .map(exportMap => exportMap.id),
                $scope.exportsToDelete
                    .map(exportMap => exportMap.fileid));
            $scope.isAllExportSelected = false;
            $scope.display.delete = false;
            template.close('export.delete.lightbox');
            $scope.notifications.push(new Notification('lystore.delete.notif', 'confirm'));
            $scope.exportToDelete = [];
            await $scope.exports.getExports();

        };

        $scope.isAllExportSelected = false;
        $scope.switchAllExports = ():void => {
            $scope.isAllExportSelected  =  !$scope.isAllExportSelected;
            if ( $scope.isAllExportSelected) {
                $scope.exports.all.map(exportSelected => exportSelected.selected = true)
            } else {
                $scope.exports.all.map(exportSelected => exportSelected.selected = false)
            }
            Utils.safeApply($scope);
        };

        $scope.controlDeleteExport = ():Boolean => {
            return $scope.exports.selected.some(exportSome => exportSome.status === STATUS.WAITING)
        };

    }
]);