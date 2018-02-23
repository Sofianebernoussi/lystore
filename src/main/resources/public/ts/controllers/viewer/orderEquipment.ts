import {ng, moment, template} from 'entcore';
import { OrdersEquipments, OrderEquipment, Utils, Notification,
} from '../../model';
declare let window: any;
export const orderEquipmentController = ng.controller('orderEquipmentController',
['$scope', '$routeParams',  ($scope, $routeParams) => {

    $scope.display = {
        orderEquipmentOption : [],
        lightbox : {
            deleteOrderEquipement : false,
        }
    };
    $scope.exportCSV = () => {
       let idCampaign = $scope.ordersEquipments.all[0].id_campaign;
       let idStructure = $scope.ordersEquipments.all[0].id_structure;
        window.location = `/lystore/orders/export/${idCampaign}/${idStructure}`;
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

    $scope.displayLightboxDelete = (orderEquipment: OrderEquipment) => {
        template.open('orderEquipment.delete', 'customer/campaign/order/delete-confirmation');
        $scope.orderEquipmentToDelete = orderEquipment;
        $scope.display.lightbox.deleteOrderEquipement = true;
        Utils.safeApply($scope);
    };
    $scope.cancelOrderEquipmentDelete = () => {
        delete $scope.orderEquipmentToDelete;
        $scope.display.lightbox.deleteOrderEquipement = false;
        template.close('orderEquipment.delete');
        Utils.safeApply($scope);
    };

    $scope.deleteOrderEquipment = async (orderEquipmentToDelete: OrderEquipment) => {
        let { status, data } = await orderEquipmentToDelete.delete($scope.current.structure.id);
        if (status === 200) {
            $scope.campaign.nb_order = data.nb_order;
            $scope.campaign.purse_amount = data.amount;
            $scope.notifications.push(new Notification('lystore.orderEquipment.delete.confirm', 'confirm'));
        }
        $scope.cancelOrderEquipmentDelete();
        await $scope.ordersEquipments.sync($routeParams.idCampaign, $scope.current.structure.id );
        Utils.safeApply($scope);
    };
}]);
