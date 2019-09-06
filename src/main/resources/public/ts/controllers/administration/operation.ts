import {_, ng, template, idiom as lang} from 'entcore';
import {Notification, Operation, OrderClient, OrderRegion, OrdersRegion, Utils} from "../../model";
import {Mix} from 'entcore-toolkit';

declare let window: any;

export const operationController = ng.controller('operationController',
    ['$scope',  '$routeParams',($scope, $routeParams) => {
        $scope.lang = lang;
        $scope.orderRegion = new OrderRegion();
        $scope.ordersRegion = new OrdersRegion();
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
            for(let i = 0 ; i<arrayLookFor.length ; i++){
                if(arrayLookFor[i] !== null){
                    return arrayLookFor[i];
                }
            }
            return "-" ;
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
            if($scope.operations.selected.some(operation => operation.nb_orders !== 0 )){
                template.open('operation.lightbox', 'administrator/operation/operation-delete-reject-lightbox');
            } else {
                await $scope.operations.delete();
                await $scope.initOperation();
                template.close('operation.lightbox');
                $scope.display.lightbox.operation = false;
                Utils.safeApply($scope);
            }
        };


        $scope.dropOrdersOperation = async (orders)=>{
            let ordersToDelete = [];
              orders.forEach(  order =>
               ordersToDelete.push($scope.dropOrderOperation(order))
             );
           await Promise.all([ordersToDelete ,
               await $scope.initOperation(),
               await $scope.syncOrderByOperation($scope.operation),]);

                  $scope.notifications.push(
                      new Notification('lystore.order.operation.delete', 'confirm'))
                  Utils.safeApply($scope)


        };

        $scope.syncOrderByOperation = async (operation: Operation) =>{
            console.log("calling sync ")
            $scope.ordersClientByOperation = await operation.getOrders($scope.structures.all);
        };
        $scope.dropOrderOperation = async (order:any , bool?) => {
            if(order.isOrderRegion){
                await $scope.orderRegion.delete(order.id);
                if(order.id_order_client_equipment){
                    await order.updateStatusOrder('WAITING', order.id_order_client_equipment);
                }
            } else {
                await order.updateStatusOrder('WAITING');
            }

            if(bool){
                $scope.notifications.push(new Notification('lystore.order.operation.delete', 'confirm'));
                Utils.safeApply($scope);
            }
            //

        };
        $scope.formatArrayToolTip = (tooltipsIn:string) => {
            let tooltips = JSON.parse(tooltipsIn);
            if(tooltips.length === 0 ){
                return ""
            } else {
                tooltips = tooltips.filter( el => el !== null);
                return _.uniq(tooltips).join(" - ");
            }
        };
        $scope.formatDate = (date) => {
            return Utils.formatDate(date)
        };

        $scope.insertOrderRegion = (order: OrderClient):void => {
            $scope.order = order;
            let type = order.isOrderRegion? 'region' : 'client';
            $scope.redirectTo(`/order/operation/update/${order.id}/${type}`);
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

        $scope.oneOrderSelected = () : boolean =>{
            let nbSelected =  0 ;
            $scope.ordersClientByOperation.forEach(order =>{
                if(order.selected){
                    nbSelected++;
                }
            });
            return  nbSelected === 1;
        };

        $scope.getSelectedOrder  = () =>{
            return $scope.ordersClientByOperation.find(order => order.selected);
        };

        $scope.getSelectedOrders = () =>{
            let selectedOrders = [] ;
            $scope.ordersClientByOperation.forEach(order =>{
                if (order.selected)
                    selectedOrders.push(order);
            });
            return selectedOrders;
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
            let idsOrdersClient = $scope.ordersClientByOperation.filter(order => order.selected && !order.isOrderRegion).map(order => order.id);
            let idsOrdersRegion = $scope.ordersClientByOperation.filter(order => order.selected && order.isOrderRegion).map(order => order.id);
            if(idsOrdersClient.length !== 0){
                await $scope.ordersClient.addOperation(operation.id, idsOrdersClient);
            }
            if(idsOrdersRegion.length !== 0){
                await $scope.ordersRegion.updateOperation(operation.id, idsOrdersRegion);
            }
            $scope.ordersClientByOperation = await $scope.operation.getOrders();
            $scope.display.lightbox.operation = false;
            Utils.safeApply($scope);
        };
    }]);