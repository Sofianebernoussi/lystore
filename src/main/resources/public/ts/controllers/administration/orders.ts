import {ng, moment, template, _, model} from 'entcore';
import {OrderClient, Utils, OrdersClient, Notification, Supplier} from '../../model';
import {Mix} from 'entcore-toolkit';
declare let window: any;
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
                sendOrder : false,
                validOrder : false,
            },
            generation: {
                type: 'ORDER'
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
        $scope.windUpOrders = async (orders: OrderClient[]) => {
            let ordersToWindUp  = new OrdersClient();
            ordersToWindUp.all = Mix.castArrayAs(OrderClient, orders);
            let { status } = await ordersToWindUp.updateStatus('DONE');
            if (status === 200) {
                $scope.notifications.push(new Notification('lystore.windUp.notif', 'confirm'));
            }
            await $scope.ordersClient.sync($scope.structures.all);
            $scope.ordersClient.all = _.where($scope.ordersClient.all, {status: 'SENT'});
            Utils.safeApply($scope);
        };
        $scope.validateSentOrders = (orders: OrderClient[]) => {
            let id_suppliers = (_.uniq(_.pluck(orders, 'id_contract')));
            return id_suppliers.length === 1 ;
        };

        $scope.prepareSendOrder = async (orders: OrderClient[]) => {
            if ($scope.validateSentOrders(orders)) {
                try {
                    await $scope.initOrdersForPreview(orders);
                } catch (e) {
                    console.error(e);
                    $scope.notifications.push(new Notification('lystore.order.pdf.preview.error', 'warning'));
                } finally {
                    if ($scope.orderToSend.hasOwnProperty('preview')) {
                        $scope.redirectTo('/order/preview');
                    }
                    Utils.safeApply($scope);
                }
            }
        };
        $scope.validatePrepareSentOrders = (orderToSend: OrdersClient) => {
            return orderToSend && orderToSend.supplier && orderToSend.bc_number && orderToSend.engagement_number
                && orderToSend.bc_number !== undefined && orderToSend.engagement_number !== undefined
                && orderToSend.bc_number.trim() !== '' && orderToSend.engagement_number.trim() !== '';
        };
        $scope.sendOrders = async (orders: OrdersClient) => {
            let { status, data } = await orders.updateStatus('SENT');
            $scope.saveByteArray(`BC_${orders.bc_number}`, data);
            if (status === 200) {
                $scope.notifications.push(new Notification( 'lystore.sent.notif' , 'confirm'));
            }
            await $scope.ordersClient.sync($scope.structures.all);
            $scope.cancelPrepareSentOrders();
            $scope.redirectTo('/order/valid');
            Utils.safeApply($scope);
        };
        $scope.cancelPrepareSentOrders = () => {
            $scope.display.lightbox.sendOrder = false;
            template.close('sendOrder.lightbox');
            Utils.safeApply($scope);
        };
        $scope.saveByteArray = (reportName, data) => {
            let blob = new Blob([data]);
            let link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download =  reportName + '.pdf';
            document.body.appendChild(link);
            link.click();
            setTimeout(function(){
                document.body.removeChild(link);
                window.URL.revokeObjectURL(link.href);
            }, 100);
        };
        $scope.exportCSV = async() => {
            let params = Utils.formatKeyToParameter($scope.ordersClient.selected, 'id');
            window.location = `/lystore/orders/export?${params}`;
        };

        $scope.getUsername = () => model.me.username;

        $scope.concatOrders = () => {
            let arr = [];
            $scope.orderToSend.preview.certificates.map((certificate) => {
               arr = [...arr, ...certificate.orders];
            });
            return arr;
        };
        $scope.exportCSV = async() => {
            let params = Utils.formatKeyToParameter($scope.ordersClient.selected, 'id');
            window.location = `/lystore/orders/export?${params}`;
        };

    }]);