import {ng, moment, template} from 'entcore';
import { OrderClient, Utils, Notification } from '../../model';

declare let window: any;

export const orderPersonnelController = ng.controller('orderPersonnelController',
['$scope', '$routeParams',  ($scope, $routeParams) => {

    $scope.display = {
        ordersClientOption : [],
        lightbox : {
            deleteOrder : false,
        }
    };

    $scope.exportCSV = () => {
        let idCampaign = $scope.ordersClient.all[0].id_campaign;
        let idStructure = $scope.ordersClient.all[0].id_structure;
        window.location = `/lystore/orders/export/${idCampaign}/${idStructure}`;
    };

    $scope.displayEquipmentOption = (index: number) => {
        $scope.display.ordersClientOption[index] = !$scope.display.ordersClientOption[index];
        Utils.safeApply($scope);
    };

    $scope.calculateDelivreryDate = (date: Date) => {
        return moment(date).add(60, 'days').calendar();
    };
    $scope.calculateTotal = (orderClient: OrderClient, roundNumber: number) => {
        let totalPrice = $scope.calculatePriceOfEquipment(orderClient, true, roundNumber) * orderClient.amount;
        return totalPrice.toFixed(roundNumber);
    };

    $scope.displayLightboxDelete = (orderEquipment: OrderClient) => {
        template.open('orderClient.delete', 'customer/campaign/order/delete-confirmation');
        $scope.orderEquipmentToDelete = orderEquipment;
        $scope.display.lightbox.deleteOrder = true;
        Utils.safeApply($scope);
    };
    $scope.cancelOrderEquipmentDelete = () => {
        delete $scope.orderEquipmentToDelete;
        $scope.display.lightbox.deleteOrder = false;
        template.close('orderClient.delete');
        Utils.safeApply($scope);
    };

    $scope.deleteOrderEquipment = async (orderEquipmentToDelete: OrderClient) => {
        let { status, data } = await orderEquipmentToDelete.delete();
        if (status === 200) {
            $scope.campaign.nb_order = data.nb_order;
            $scope.campaign.purse_amount = data.amount;
            $scope.notifications.push(new Notification('lystore.orderEquipment.delete.confirm', 'confirm'));
        }
        $scope.cancelOrderEquipmentDelete();
        await $scope.ordersClient.sync(null, [], $routeParams.idCampaign, $scope.current.structure.id );
        Utils.safeApply($scope);
    };

}]);
