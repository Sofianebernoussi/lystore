import {_, ng, template, idiom as lang} from 'entcore';
import {Operation, OrderClient, Utils} from "../../model";
import {Mix} from 'entcore-toolkit';

declare let window: any;

export const operationController = ng.controller('operationController',
    ['$scope',  '$routeParams',($scope, $routeParams) => {
        $scope.lang = lang;
        $scope.allOrdersOperationSelected = false;
        $scope.sort = {
            operation : {
                type: 'label.label',
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

        $scope.cancelOperationForm = async () =>{
            $scope.display.lightbox.operation = false;
            template.close('operation.lightbox');
            await $scope.initOperation();
            Utils.safeApply($scope);
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
        $scope.syncOrderByOperation = async (operation: Operation) =>{
            $scope.ordersClientByOperation = await operation.getOrders();
        };
        $scope.dropOrderOperation = async (order:OrderClient) => {
            await order.updateStatusOrder('WAITING');
            await Promise.all([
                await $scope.syncOrderByOperation($scope.operation),
                await $scope.initOperation(),
            ]);
            Utils.safeApply($scope);
        };
        $scope.formatArrayToolTip = (tooltipsIn:string) => {
            let tooltips = JSON.parse(tooltipsIn);
            if(tooltips.some(tooltip => tooltip === null) || (tooltips.length === 1 && tooltips[0] === null)){
                return ""
            } else {
                return _.uniq(tooltips).join(" - ");
            }
        };
        $scope.formatDate = (date) => {
            return Utils.formatDate(date)
        };

        $scope.insertOrderRegion = (order: OrderClient):void => {
            $scope.order = order;
            $scope.redirectTo(`/order/operation/update/${order.id}`);
        };
        $scope.switchAllOrders = ():void => {
            $scope.allOrdersOperationSelected  =  !$scope.allOrdersOperationSelected;
            if ( $scope.allOrdersOperationSelected) {
                $scope.ordersClientByOperation.map(order => order.selected = true);
            } else {
                $scope.ordersClientByOperation.map(order => order.selected = false);
            }
            Utils.safeApply($scope);
        };
        $scope.isOrderOperationSelected = ():boolean => {
            return $scope.ordersClientByOperation.some(order => order.selected)
        };
        $scope.selectOperationForOrder = async () =>{
            await $scope.initOperation();
            $scope.operations.all = $scope.operations.all.filter(operation => operation.id !== $scope.operation.id);
            $scope.isOperationsIsEmpty = !$scope.operations.all.some(operation => operation.status === 'true');
            template.open('operation.lightbox', 'administrator/order/order-select-operation');
            $scope.display.lightbox.operation = true;
        };
        $scope.operationSelected = async (operation:Operation) => {
            template.close('operation.lightbox');
            let idsOrder = $scope.ordersClientByOperation.filter(order => order.selected).map(order => order.id);
            await $scope.ordersClient.addOperation(operation.id, idsOrder);
            $scope.ordersClientByOperation = await $scope.operation.getOrders();
            $scope.display.lightbox.operation = false;
            Utils.safeApply($scope);
        };
    }]);