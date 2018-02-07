import {moment, ng, template, _ } from 'entcore';
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
                deleteBasket : false
            }
        };
        $scope.calculatePriceOfEquipments = (baskets: Baskets) => {
            let totalPrice = 0;
            baskets.all.map((basket) => {
                let basketItemPrice = $scope.calculatePriceOfBasket(basket, 2);
                totalPrice += !isNaN(basketItemPrice) ?  basketItemPrice : 0;
            });
            return totalPrice;
        };
        $scope.calculatePriceOfBasket = (basket: Basket, roundNumber: number ) => {
            let equipmentPrice =  $scope.calculatePriceOfEquipment(basket.equipment, false, roundNumber);
            return equipmentPrice * basket.amount;
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
            $scope.campaign.nb_panier -= status === 200 ? 1 : 0;
            $scope.cancelBasketDelete();
            await $scope.baskets.sync($routeParams.idCampaign, $scope.structure);
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
            else {

            }
        };
    }]);