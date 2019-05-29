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
                operation:false
            }
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
        $scope.openOperationForm = () =>{
            $scope.operation = new Operation();
            $scope.display.lightbox.operation = true;
            template.open('operation.lightbox', 'administrator/operation/operation-form');
            Utils.safeApply($scope);
        };
        $scope.validOperationForm = (operation:Operation) =>{
         return  operation.id_label ;
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
            $scope.isAllOperationSelected  =  !$scope.isAllOperationSelected
            if ( $scope.isAllOperationSelected) {
                $scope.operations.all.map(operationSelected => operationSelected.selected = true)
            } else {
                $scope.operations.all.map(operationSelected => operationSelected.selected = false)
            }
            Utils.safeApply($scope);
        }
    }]);