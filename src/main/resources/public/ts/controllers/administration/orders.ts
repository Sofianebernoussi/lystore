import {ng, moment, template, _} from 'entcore';
import {  OrderClient, Utils, OrdersClient } from '../../model';
import {Mix} from 'entcore-toolkit';

export const orderController = ng.controller('orderController',
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
                validOrder : false
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
        $scope.validateOrders = async (orders: OrderClient[]) => {
           let ordersToValidat  = new OrdersClient();
            ordersToValidat.all = Mix.castArrayAs(OrderClient, orders);
           let { status, data } = await ordersToValidat.updateStatus('VALID');
            if (status === 200) {
                $scope.orderValidationData = {
                    agents: _.uniq(data.agent),
                    number_validation: data.number_validation,
                    structures: _.uniq(_.pluck(ordersToValidat.all, 'name_structure'))
                } ;
                template.open('validOrder.lightbox', 'administrator/order/order-valid-confirmation');
                $scope.display.lightbox.validOrder = true;
            }
            await $scope.ordersClient.sync($scope.structures.all);
            $scope.ordersClient.all = _.where($scope.ordersClient.all, {status: 'WAITING'});
            Utils.safeApply($scope);
        };
        $scope.cancelBasketDelete = () => {
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
            Utils.safeApply($scope);
        };
    }]);