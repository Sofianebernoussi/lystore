import {ng, moment, template} from 'entcore';
import { OrdersEquipments, OrderEquipment, Utils } from '../../model';

export const orderEquipmentController = ng.controller('orderEquipmentController',
['$scope', '$routeParams',  ($scope, $routeParams) => {

    $scope.display = {
        orderEquipmentOption : [],
        lightbox : {
            deleteOrder : false,
        }
    };
    $scope.displayEquipmentOption = (index: number) => {
        $scope.display.orderEquipmentOption[index] = !$scope.display.orderEquipmentOption[index];
        Utils.safeApply($scope);
    };

    $scope.calculateDelivreryDate = () => {
        return moment().add(60, 'days').calendar();
    };
    $scope.calculateTotal = (orderEquipment: OrderEquipment, roundNumber: number) => {
        let totalPrice = $scope.calculatePriceOfEquipment(orderEquipment, false, roundNumber) * orderEquipment.amount;
        return totalPrice.toFixed(roundNumber);
    };

}]);
