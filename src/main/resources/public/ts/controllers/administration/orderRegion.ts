import {ng, template} from 'entcore';
import {ContractTypes, Notification, Operation, OrderClient, OrderRegion} from "../../model";
import {Equipments} from "../../model/Equipment";


declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', '$location', '$routeParams', ($scope, $location, $routeParams) => {
        $scope.orderToUpdate = new OrderClient();
        $scope.equipments = new Equipments();
        $scope.contractTypes = new ContractTypes();
        $scope.display = {
            lightbox: {
                validOrder: false,
            }
        };
        $scope.initDataUpdate = async () => {
            await $scope.equipments.sync($scope.orderToUpdate.id_campaign, $scope.orderToUpdate.id_structure);
            $scope.orderToUpdate.equipment = $scope.equipments.all.find((e) => {
                return e.id === $scope.orderToUpdate.equipment_key;
            });
        };


        if ($routeParams.idOrder) {
            let idOrder = $routeParams.idOrder;
            $scope.ordersClient.all.forEach((o) => {
                if (o.id == idOrder)
                    $scope.orderToUpdate = o;
            })
            $scope.initDataUpdate();
        }
        $scope.isUpdating = $location.$$path.includes('/order/update');
        $scope.isUpdatingFromOrder = $location.$$path.includes('/order/operation/update');

        $scope.getTotal = () => {
            return ($scope.orderToUpdate.amount * $scope.orderToUpdate.priceTTCtotal).toFixed(2);
        };


        $scope.operationSelected = async (operation: Operation) => {
            $scope.isOperationSelected = true;
            $scope.operation = operation;
            if ($scope.isUpdating) {
                let orderRegion = new OrderRegion();
                orderRegion.createFromOrderClient($scope.orderToUpdate);
                orderRegion.id_operation = operation.id;
                orderRegion.equipment_key = $scope.orderToUpdate.equipment_key;
                $scope.cancelUpdate();
                await orderRegion.set();
                $scope.notifications.push(new Notification('lystore.order.region.update', 'confirm'));
            }
        };

        $scope.isOperationsIsEmpty = false;
        $scope.selectOperationForOrder = async () => {
            await $scope.initOperation();
            $scope.isOperationsIsEmpty = !$scope.operations.all.some(operation => operation.status === 'true');
            template.open('validOrder.lightbox', 'administrator/order/order-select-operation');
            $scope.display.lightbox.validOrder = true;
        };

        $scope.cancelUpdate = () => {
            if ($scope.isUpdating)
                $scope.redirectTo('/order/waiting');
            if ($scope.isUpdatingFromOrder)
                $scope.redirectTo('/operation');
        };
        $scope.updateOrderConfirm = async () => {
            await $scope.selectOperationForOrder();
        };

        $scope.updateLinkedOrderConfirm = async () => {
            let orderRegion = new OrderRegion();
            orderRegion.createFromOrderClient($scope.orderToUpdate);
            orderRegion.equipment_key = $scope.orderToUpdate.equipment_key;
            $scope.redirectTo('/operation');
            await orderRegion.set();
            $scope.notifications.push(new Notification('lystore.order.region.update', 'confirm'));
        };
    }
    ]);