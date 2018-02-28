import {ng, moment, template, _} from 'entcore';
import {  OrderClient, Utils } from '../../model';

export const orderClientController = ng.controller('orderClientController',
    ['$scope',  ($scope) => {
        $scope.allOrdersSelected = false;
        $scope.sort = {
            order : {
                type: 'name',
                reverse: false
            }
        };
        $scope.search = {
            filterWord : '',
          filterWords : []
        };
        $scope.display = {
            ordersClientOptionOption : [],
            lightbox : {
                deleteOrder : false,
            }
        };
        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };
        $scope.calculateTotal = (orderClient: OrderClient, roundNumber: number) => {
            let totalPrice = $scope.calculatePriceOfEquipment(orderClient, false, roundNumber) * orderClient.amount;
            return totalPrice.toFixed(roundNumber);
        };

        $scope.addFilter = (filterWord: string, event?) => {
            if (event && (event.which === 13 || event.keyCode === 13 )) {
                $scope.addFilterWords(filterWord);
            } else if (!event) {
                $scope.addFilterWords(filterWord);
            }
        };
        $scope.addFilterWords = (filterWord) => {
            if (filterWord !== '') {
                $scope.search.filterWords = _.union($scope.search.filterWords, [filterWord]);
                $scope.search.filterWord = '';
                Utils.safeApply($scope);
            }
        };
        $scope.pullFilterWord = (filterWord) => {
            $scope.search.filterWords = _.without( $scope.search.filterWords , filterWord);
        };
    }]);