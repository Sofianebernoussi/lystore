import { ng, template, notify, moment, _ } from 'entcore';
import {labels, Operation, Utils} from "../../model";

declare let window: any;

export const operationController = ng.controller('operationController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {

        $scope.sort = {
            operation : {
                type: 'name',
                reverse: false
            }
        };
        $scope.allOrdersSelected = false;
        $scope.search = {
            filterWord : '',
            filterWords : []
        };
        $scope.display = {
            lightbox : {
                operation:false,
            }
        };

        $scope.formatDataStringArrayToArray = stringLookForArray => {
            return Utils.formatDataStringArrayToArray(stringLookForArray).length !== 1 &&
            Utils.formatDataStringArrayToArray(stringLookForArray)[0] !== null ?
                Utils.formatDataStringArrayToArray(stringLookForArray)[0] : "-";
        };

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.addEquipmentFilter = (event?) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.trim() !== '') {
                event.target.value = '';
            }
        };

        $scope.openOperationForm = action => {
                if(action === 'create'){
                    $scope.operation = new Operation();
                } else if (action === 'edit'){
                    $scope.operation = $scope.operations.selected[0];
                    $scope.operation.status = !!$scope.operation.status;
                }
            $scope.display.lightbox.operation = true;
            template.open('operation.lightbox', 'administrator/operation/operation-form');
            Utils.safeApply($scope);
        };

        $scope.validOperationForm = (operation:Operation) =>{
         return  operation.id_label;
        };

        $scope.cancelOperationForm = () =>{
            $scope.display.lightbox.operation = false;
            template.close('operation.lightbox');
        };

        $scope.validOperation = async (operation:Operation) =>{
           await operation.save();
           $scope.cancelOperationForm();
           await $scope.initOperation();
           Utils.safeApply($scope);
        };

        $scope.isAllOperationSelected = false;
        $scope.switchAllOperations = () => {
            $scope.isAllOperationSelected  =  !$scope.isAllOperationSelected;
            if ( $scope.isAllOperationSelected) {
                $scope.operations.all.map(operationSelected => operationSelected.selected = true)
            } else {
                $scope.operations.all.map(operationSelected => operationSelected.selected = false)
            }
            Utils.safeApply($scope);
        };

        $scope.openLightboxDeleteOperation = () => {
            $scope.display.lightbox.operation = true;
            template.open('operation.lightbox', 'administrator/operation/operation-delete-lightbox');
            Utils.safeApply($scope);
        };

        $scope.deleteOperations = () => {
            if($scope.operations.selected.some(operation => operation.nbr_sub !== 0 )){
                template.open('operation.lightbox', 'administrator/operation/operation-delete-reject-lightbox');
            } else {
                $scope.operations.delete();
                template.close('operation.lightbox');
                $scope.display.lightbox.operation = false;
            }
        };
    }]);