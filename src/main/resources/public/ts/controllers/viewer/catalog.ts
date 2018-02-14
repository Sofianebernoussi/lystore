/**
 * Created by rahnir on 22/01/2018.
 */
/**
 * Created by rahnir on 18/01/2018.
 */
import { ng, template, _} from 'entcore';
import {
    Equipment,
    Basket,
    Utils
} from '../../model';



export const catalogController = ng.controller('catalogController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.search = {
            filterWrod: '',
            filterWrods: []
        };
        $scope.alloptionsSelected = false;
        $scope.equipment = new Equipment();
        $scope.addFilter = (filterWrod: string, event?) => {
            if (event && (event.which === 13 || event.keyCode === 13 )) {
                $scope.addfilterWords(filterWrod);
            } else if (!event) {
                $scope.addfilterWords(filterWrod);
            }
        };
        $scope.addfilterWords = (filterWrod) => {
            if (filterWrod !== '') {
                $scope.search.filterWrods = _.union($scope.search.filterWrods, [filterWrod]);
                $scope.search.filterWrod = '';
                Utils.safeApply($scope);
            }
        };
        $scope.pullFilterWord = (filterWord) => {
            $scope.search.filterWrods = _.without( $scope.search.filterWrods , filterWord);
        };
        $scope.openEquipment = (equipment: Equipment) => {
            if (equipment.status === 'AVAILABLE') {
                $scope.redirectTo(`/campaign/${$routeParams.idCampaign}/catalog/equipment/${equipment.id}`);
                $scope.display.equipment = true;
            }
        };
        $scope.validArticle = (equipment: Equipment) => {
            return !isNaN(parseFloat($scope.calculatePriceOfEquipment(equipment)))
                && $scope.basket.amount > 0;
        };
        $scope.switchAll = (model: boolean, collection) => {
           collection.forEach((col) => {col.selected = col.required ? false : col.selected = model; });
            Utils.safeApply($scope);
        };
        $scope.thereAreOptionalOptions = (equipment: Equipment) => {
            return !(_.findWhere(equipment.options, {required : false}) === undefined) ;
        };
        $scope.addBasketItem = async (basket: Basket) => {
            let { status } = await basket.create();
            if (status === 200 && basket.amount > 0 ) {
                $scope.campaign.nb_panier += 1;
                await $scope.notifyBasket('added', basket);
            }

            Utils.safeApply($scope);
        };
        $scope.amountIncrease = () => {
            $scope.basket.amount += 1;
        };
        $scope.amountDecrease = () => {
            $scope.basket.amount -= 1;
        };
    }]);