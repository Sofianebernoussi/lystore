import { ng, template, notify, moment, _ } from 'entcore';
import {labels, Operation, OrderClient, Utils} from "../../model";

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
                ordersListOfOperation:false,
            }
        };

        $scope.getFirstElement = jsonArray => {
            let arrayLookFor = JSON.parse(jsonArray);
            return arrayLookFor[0] !== null ? arrayLookFor[0] : "-" ;
        };

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.addOperationFilter = async (event?) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.trim() !== '') {
                if(!_.contains($scope.operations.filters, event.target.value)){
                    $scope.operations.filters = [...$scope.operations.filters, event.target.value];
                }
                event.target.value = '';
                await $scope.initOperation();
                Utils.safeApply($scope);
            }
        };

        $scope.dropOperatonFilter = async (filter: string) => {
            $scope.operations.filters = $scope.operations.filters.filter( filterWord => filterWord !== filter);
            await $scope.initOperation();
            Utils.safeApply($scope);
        };

        $scope.openOperationForm = (action: string) => {
            if(action === 'create'){
                $scope.operation = new Operation();
            } else if (action === 'edit'){
                $scope.operation = $scope.operations.selected[0];
                $scope.operation.status = ($scope.operation.status === 'true');
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
            $scope.display.lightbox.ordersListOfOperation = false;
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
        $scope.deleteOperations = async () => {
            if($scope.operations.selected.some(operation => operation.nbr_sub !== 0 )){
                template.open('operation.lightbox', 'administrator/operation/operation-delete-reject-lightbox');
            } else {
                await $scope.operations.delete();
                await $scope.initOperation();
                template.close('operation.lightbox');
                $scope.display.lightbox.operation = false;
                Utils.safeApply($scope);
            }
        };
        $scope.openLightBoxOrdersList = async () => {
            $scope.display.lightbox.ordersListOfOperation = true;
            $scope.operation = $scope.operations.selected[0];
            await $scope.ordersClient.ordersClientOfOperation($scope.operation.id);
            template.open('operation.lightbox', 'administrator/operation/operation-orders-list-lightbox');
            Utils.safeApply($scope);
        };
        $scope.addOrderFilter = async (event?) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.trim() !== '') {
                if(!_.contains($scope.ordersClient.filters, event.target.value)){
                    $scope.ordersClient.filters = [...$scope.ordersClient.filters, event.target.value];
                }
                event.target.value = '';
                await $scope.ordersClient.ordersClientOfOperation($scope.operation.id);
                Utils.safeApply($scope);
            }
        };
        $scope.dropOrderFilter = async (filter: string) =>{
            $scope.ordersClient.filters = $scope.ordersClient.filters.filter( filterWord => filterWord !== filter);
            await $scope.ordersClient.ordersClientOfOperation($scope.operation.id);
            Utils.safeApply($scope);
        };
        $scope.dropOrderOperation = async (order:OrderClient) => {
            await order.updateStatusOrder('WAITING');
            await Promise.all([
                await $scope.ordersClient.ordersClientOfOperation($scope.operation.id),
                await $scope.initOperation(),
            ]);
            Utils.safeApply($scope);
        };
        $scope.formatArrayToolTip = (tooltipsIn:string) => {
            let tooltips = JSON.parse(tooltipsIn);
            if(tooltips.some(tooltip => tooltip === null) || (tooltips.length === 1 && tooltips[0] === null)){
                return ""
            } else {
                return tooltips.join(" - ")
            }
        }
    }]);