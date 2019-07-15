import {ng, notify, template} from 'entcore';
import {Notification, Operation, OrderClient, OrderRegion, Utils} from "../../model";
import {Equipments} from "../../model/Equipment";


declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', '$location', '$routeParams', ($scope, $location, $routeParams) => {
        $scope.orderToUpdate = new OrderClient();
        $scope.equipments = new Equipments();
        $scope.contract_type = "-";
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
            $scope.getContractType();
        };


        if ($routeParams.idOrder) {
            let idOrder = $routeParams.idOrder;
            $scope.ordersClient.all.forEach((o) => {
                if (o.id == idOrder)
                    $scope.orderToUpdate = o;
            });
            ($scope.orderToUpdate.price_proposal)
                ? $scope.orderToUpdate.price_proposal = parseFloat($scope.orderToUpdate.price_proposal)
                : $scope.orderToUpdate.price_proposal = $scope.orderToUpdate.priceTTCtotal;
            if (!$scope.orderToUpdate.project.room)
                $scope.orderToUpdate.project.room = '-';
            if (!$scope.orderToUpdate.project.building)
                $scope.orderToUpdate.project.building = '-';


            $scope.initDataUpdate();
        }
        $scope.isUpdating = $location.$$path.includes('/order/update');
        $scope.isUpdatingFromOrder = $location.$$path.includes('/order/operation/update');

        $scope.getTotal = () => {
            return ($scope.orderToUpdate.amount * $scope.orderToUpdate.price_proposal).toFixed(2);
        };


        $scope.operationSelected = async (operation: Operation) => {
            $scope.isOperationSelected = true;
            $scope.operation = operation;
            if ($scope.isUpdating) {

                let orderRegion = new OrderRegion();
                orderRegion.createFromOrderClient($scope.orderToUpdate);

                orderRegion.id_operation = operation.id;
                orderRegion.equipment_key = $scope.orderToUpdate.equipment_key;
                let {status, data} = await orderRegion.set();
                if (status === 200) {
                    $scope.notifications.push(new Notification('lystore.order.region.update', 'confirm'));
                    $scope.cancelUpdate();
                }
                else {
                    notify.error('lystore.admin.order.update.err');
                }
                Utils.safeApply($scope);

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
        $scope.isValidFormUpdate = () => {
            return $scope.orderToUpdate.equipment_key
                && $scope.orderToUpdate.price_proposal
                && $scope.orderToUpdate.amount
                && (($scope.orderToUpdate.campaign.orderPriorityEnable() && $scope.orderToUpdate.rank) || !$scope.orderToUpdate.campaign.orderPriorityEnable())
        }

        $scope.getContractType = () => {
            let contract;
            $scope.contracts.all.map(c => {
                if (c.id === $scope.orderToUpdate.equipment.id_contract)
                    contract = c
            });
            $scope.contractTypes.all.map(c => {
                if (c.id === contract.id_contract_type) {
                    $scope.contract_type = c.displayName
                }
            });
            Utils.safeApply($scope);
        }

        $scope.cancelBasketDelete = () => {
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
        }
    }
    ]);