import {moment, ng, template, _} from 'entcore';
import {
    Basket,
    Baskets,
    Utils
} from '../../model';
export const basketController = ng.controller('basketController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.display = {
            equipmentOption : [],

            lightbox : {
                deleteBasket : false,
                confirmOrder : false
            }
        };
        $scope.calculatePriceOfEquipments = (baskets: Baskets, roundNumber?: number) => {
            let totalPrice = 0;
            baskets.all.map((basket) => {
                if (basket.equipment.status !== 'AVAILABLE') return false;
                let basketItemPrice = $scope.calculatePriceOfBasket(basket, 2, false);
                totalPrice += !isNaN(basketItemPrice) ? parseFloat(basketItemPrice) : 0;
            });
            return (!isNaN(totalPrice)) ? (roundNumber ? totalPrice.toFixed(roundNumber) : totalPrice ) : '';
        };
        $scope.calculatePriceOfBasket = (basket: Basket, roundNumber?: number,  toDisplay?: boolean ) => {
            let equipmentPrice =  $scope.calculatePriceOfEquipment(basket.equipment, true, roundNumber);
            equipmentPrice = basket.amount === 0 && toDisplay  ? equipmentPrice : equipmentPrice * basket.amount;
            return (!isNaN(equipmentPrice)) ? (roundNumber ? equipmentPrice.toFixed(roundNumber) : equipmentPrice ) : '';
        };
        $scope.calculeDeliveryDate = () => {
            return moment().add(60, 'days').calendar();
        };
        $scope.displayOptions = (index: number) => {
            $scope.display.equipmentOption[index] = !$scope.display.equipmentOption[index] ;
            Utils.safeApply($scope);
        };
        $scope.displayLightboxDelete = (basket: Basket) => {
            template.open('basket.delete', 'customer/campaign/basket/delete-confirmation');
            $scope.basketToDelete = basket;
            $scope.display.lightbox.deleteBasket = true;
            Utils.safeApply($scope);
        };
        $scope.deleteBasket = async (basket: Basket) => {
            let { status } = await basket.delete();
            if (status === 200) {
                $scope.campaign.nb_panier -= 1;
              await $scope.notifyBasket('deleted', basket);
            }
            $scope.cancelBasketDelete();
            await $scope.baskets.sync($routeParams.idCampaign, $scope.current.structure.id);
            Utils.safeApply($scope);
        };
        $scope.cancelBasketDelete = () => {
            delete $scope.basketToDelete;
            $scope.display.lightbox.deleteBasket = false;
            template.close('basket.delete');
            Utils.safeApply($scope);
        };
        $scope.updateBasketAmount = (basket: Basket) => {
            if (basket.amount === 0) {
                $scope.displayLightboxDelete(basket);
            }
            else if (basket.amount > 0) {
                basket.updateAmount();
            }
        };
        $scope.takeClientOrder = async (baskets: Baskets) => {
            let { status, data } = await baskets.takeOrder(parseInt($routeParams.idCampaign), $scope.current.structure);
            $scope.totalPrice = $scope.calculatePriceOfEquipments(baskets, 2);
            await baskets.sync(parseInt($routeParams.idCampaign), $scope.current.structure.id);
            status === 200 ?  $scope.confirmOrder(data) :  null ;
            Utils.safeApply($scope);
        };
        $scope.confirmOrder = (data) => {
            $scope.campaign.nb_panier = $scope.baskets.all.length;
            $scope.campaign.nb_order = data.nb_order;
            $scope.campaign.purse_amount = data.amount;
            template.open('basket.order', 'customer/campaign/basket/order-confirmation');
            $scope.display.lightbox.confirmOrder = true;
            Utils.safeApply($scope);
        };
        $scope.cancelConfirmOrder = () => {
            $scope.display.lightbox.confirmOrder = false;
            template.close('basket.order');
            Utils.safeApply($scope);
        };
        $scope.validOrder = (baskets: Baskets) => {
            let equipmentsBasket = _.pluck(baskets.all, 'equipment' );
          return $scope.calculatePriceOfEquipments(baskets) <= $scope.campaign.purse_amount
              && _.findWhere( equipmentsBasket, {status : 'OUT_OF_STOCK'}) === undefined
              &&  _.findWhere( equipmentsBasket, {status : 'UNAVAILABLE'}) === undefined;
        };
    }]);