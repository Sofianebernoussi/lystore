import {ng, moment, template} from 'entcore';
import {  OrderClient, Utils } from '../../model';

export const orderPersonnelController = ng.controller('orderPersonnelController',
['$scope', '$routeParams',  ($scope, $routeParams) => {

    $scope.display = {
        ordersClientOptionOption : [],
        lightbox : {
            deleteOrder : false,
        }
    };
    $scope.displayEquipmentOption = (index: number) => {
        $scope.display.ordersClientOptionOption[index] = !$scope.display.ordersClientOptionOption[index];
        Utils.safeApply($scope);
    };

    $scope.calculateDelivreryDate = () => {
        return moment().add(60, 'days').calendar();
    };
    $scope.calculateTotal = (orderClient: OrderClient, roundNumber: number) => {
        let totalPrice = $scope.calculatePriceOfEquipment(orderClient, false, roundNumber) * orderClient.amount;
        return totalPrice.toFixed(roundNumber);
    };

}]);
