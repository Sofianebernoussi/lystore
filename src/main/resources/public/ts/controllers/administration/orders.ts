import {_, idiom as lang, model, ng, template} from 'entcore';
import {
    ContractTypes, Notification, Operation, OrderClient, OrderRegion, OrdersClient, orderWaiting, PRIORITY_FIELD,
    Utils
} from '../../model';
import {Mix} from 'entcore-toolkit';
import {Equipments} from "../../model/Equipment";


declare let window: any;
export const orderController = ng.controller('orderController',
    ['$scope', '$location', ($scope, $location) => {
        ($scope.ordersClient.selected[0]) ? $scope.orderToUpdate = $scope.ordersClient.selected[0] : $scope.orderToUpdate;
        $scope.allOrdersSelected = false;
        $scope.tableFields = orderWaiting;
        $scope.sort = {
            order : {
                type: 'name_structure',
                reverse: false
            }
        };
        $scope.isUpdating = $location.$$path.includes('/order/update');
        $scope.equipments = new Equipments();
        $scope.contractTypes = new ContractTypes();


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
        $scope.initDataUpdate = async () => {
            await $scope.equipments.sync($scope.orderToUpdate.id_campaign, $scope.orderToUpdate.id_structure);
            $scope.orderToUpdate.equipment = $scope.equipments.all.find((e) => {
                return e.id === $scope.orderToUpdate.equipment_key;
            });
            Utils.safeApply($scope);
        };
        if ($scope.orderToUpdate && $scope.orderToUpdate.id_campaign)
            $scope.initDataUpdate();


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
                $scope.filterDisplayedOrders();
            }
        };

        $scope.switchAllOrders = () => {
            $scope.displayedOrders.all.map((order) => order.selected = $scope.allOrdersSelected);
        };

        $scope.getSelectedOrders = () => $scope.displayedOrders.selected;

        $scope.getStructureGroupsList = (structureGroups: string[]): string => {
            return structureGroups.join(', ');
        };

        $scope.addFilterWords = (filterWord) => {
            if (filterWord !== '') {
                $scope.search.filterWords = _.union($scope.search.filterWords, [filterWord]);
                $scope.search.filterWord = '';
                Utils.safeApply($scope);
            }
        };

        function generateRegexp (words: string[]): RegExp {
            function escapeRegExp(str: string) {
                return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, '\\$&');
            }
            let reg;
            if (words.length > 0) {
                reg = '.*(';
                words.map((word: string) => reg += `${escapeRegExp(word.toLowerCase())}|`);
                reg = reg.slice(0, -1);
                reg += ').*';
            } else {
                reg = '.*';
            }
            return new RegExp(reg);
        }

        $scope.filterDisplayedOrders = () => {
            let searchResult = [];
            let regex;

            const matchStructureGroups = (structureGroups: string[]): boolean => {
                let bool: boolean = false;
                if (typeof structureGroups === 'string') structureGroups = Utils.parsePostgreSQLJson(structureGroups);
                structureGroups.map((groupName) => bool = bool || regex.test(groupName.toLowerCase()));
                return bool;
            };
            if($scope.search.filterWords.length > 0){
                $scope.search.filterWords.map((searchTerm: string, index: number): void => {
                    let searchItems: OrderClient[] = index === 0 ? $scope.ordersClient.all : searchResult;
                    regex = generateRegexp([searchTerm]);

                    searchResult = _.filter(searchItems, (order: OrderClient) => {
                        return ('name_structure' in order ? regex.test(order.name_structure.toLowerCase()) : false)
                            || ('structure' in order && order.structure['name'] ? regex.test(order.structure.name.toLowerCase()): false)
                            || ('structure' in order && order.structure['academy'] ? regex.test(order.structure.academy.toLowerCase()) : false)
                            || ('structure' in order && order.structure['type'] ? regex.test(order.structure.type.toLowerCase()) : false)
                            || ('project' in order ? regex.test(order.project.title['name'].toLowerCase()) : false)
                            || ('contract_type' in order ? regex.test(order.contract_type.name.toLowerCase()) : false)
                            || regex.test('contract' in (order as OrderClient)
                                ? order.contract.name.toLowerCase()
                                : order.contract_name)
                            || regex.test('supplier' in (order as OrderClient)
                                ? order.supplier.name.toLowerCase()
                                : order.supplier_name)
                            || ('campaign' in order ? regex.test(order.campaign.name.toLowerCase()) : false)
                            || ('name' in order ? regex.test(order.name.toLowerCase()) : false)
                            || matchStructureGroups(order.structure_groups)
                            || (order.number_validation !== null
                                ? regex.test(order.number_validation.toLowerCase())
                                : false)
                            || (order.order_number !== null && 'order_number' in order
                                ? regex.test(order.order_number.toLowerCase())
                                : false)
                            || (order.label_program !== null && 'label_program' in order
                                ? regex.test(order.label_program.toLowerCase())
                                : false);

                    });
                });

                $scope.displayedOrders.all = searchResult;
            }else{
                $scope.displayedOrders.all = $scope.ordersClient.all;
            }
        };

        $scope.pullFilterWord = (filterWord) => {
            $scope.search.filterWords = _.without( $scope.search.filterWords , filterWord);
            $scope.filterDisplayedOrders();
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
            await $scope.syncOrders('WAITING');
            Utils.safeApply($scope);
        };
        $scope.cancelBasketDelete = () => {
            $scope.operation = undefined;
            $scope.isOperationSelected = false;
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
            await $scope.syncOrders('SENT');
            Utils.safeApply($scope);
        };
        $scope.validateSentOrders = (orders: OrderClient[]) => {
            if (_.where(orders, { status : 'SENT' }).length > 0) {
                let orderNumber = orders[0].order_number;
                return _.every(orders, (order) => order.order_number === orderNumber);
            } else {
                let id_suppliers = (_.uniq(_.pluck(orders, 'id_contract')));
                return (id_suppliers.length === 1);
            }
        };

        $scope.disableCancelValidation = (orders: OrderClient[]) => {
            return _.where(orders, { status : 'SENT' }).length > 0;
        };

        $scope.prepareSendOrder = async (orders: OrderClient[]) => {
            if ($scope.validateSentOrders(orders)) {
                try {
                    await $scope.programs.sync();
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
                && orderToSend.bc_number.trim() !== '' && orderToSend.engagement_number.trim() !== ''
                && orderToSend.id_program !== undefined;
        };
        $scope.sendOrders = async (orders: OrdersClient) => {
            let { status, data } = await orders.updateStatus('SENT');
            $scope.saveByteArray(`BC_${orders.bc_number}`, data);
            if (status === 200) {
                $scope.notifications.push(new Notification( 'lystore.sent.notif' , 'confirm'));
            }
            $scope.redirectTo('/order/valid');
            Utils.safeApply($scope);
        };

        $scope.saveByteArray = (reportName, data) => {
            let blob = new Blob([data]);
            let link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download =  reportName + '.pdf';
            document.body.appendChild(link);
            link.click();
            setTimeout(function() {
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

        $scope.isValidOrdersWaitingSelection = () => {
            const orders: OrderClient[] = $scope.getSelectedOrders();
            if (orders.length > 1) {
                let isValid: boolean = true;
                let contractId = orders[0].id_contract;
                for (let i = 1; i < orders.length; i++) {
                    isValid = isValid && (contractId === orders[i].id_contract);
                }
                return isValid;
            } else {
                return true;
            }
        };

        $scope.exportOrder = (orders: OrderClient[]) => {
            if (_.where(orders, { status : 'SENT' }).length === orders.length && $scope.validateSentOrders(orders)) {
                let orderNumber = _.uniq(_.pluck(orders, 'order_number'));
                window.location = `/lystore/order?number=${orderNumber}`;
            } else {
                $scope.exportValidOrders(orders, 'order');
            }
        };

        $scope.exportValidOrders = (orders: OrderClient[], fileType: string) => {
            let params = '';
            orders.map((order: OrderClient) => {
                params += `number_validation=${order.number_validation}&`;
            });
            params = params.slice(0, -1);
            window.location = `/lystore/orders/valid/export/${fileType}?${params}`;
        };

        $scope.cancelValidation = async (orders: OrderClient[]) => {
            try {
                await $scope.displayedOrders.cancel(orders);
                await $scope.syncOrders('VALID');
                $scope.notifications.push(new Notification('lystore.orders.valid.cancel.confirmation', 'confirm'));
            } catch (e) {
                $scope.notifications.push(new Notification('lystore.orders.valid.cancel.error', 'warning'));
            } finally {
                Utils.safeApply($scope);
            }
        };

        $scope.getProgramName = (idProgram: number) => idProgram !== undefined
            ? _.findWhere($scope.programs.all, { id: idProgram }).name
            : '';

        $scope.countColSpan = (field:string):number =>{
            let totaux = $scope.isManager() ? 1 :0;
            let price = $scope.isManager() ? 1 : 0;
            let amount_field  =6;
            for (let _i = 0; _i < $scope.tableFields.length; _i++) {
                if(_i < amount_field && $scope.tableFields[_i].display){
                    totaux++;
                }else if(_i> amount_field && $scope.tableFields[_i].display)  {
                    price++;
                }
            }
            return field == 'totaux' ? totaux : price;
        };
        $scope.isOperationsIsEmpty = false;
        $scope.selectOperationForOrder = async () =>{
            await $scope.initOperation();
            $scope.isOperationsIsEmpty = !$scope.operations.all.some(operation => operation.status === 'true');
            template.open('validOrder.lightbox', 'administrator/order/order-select-operation');
            $scope.display.lightbox.validOrder = true;
        };
        $scope.isOperationSelected = false;
        $scope.operationSelected = async (operation:Operation) => {
            $scope.isOperationSelected = true;
            $scope.operation = operation;
            if ($scope.isUpdating) {
                let orderRegion = new OrderRegion();
                orderRegion.createFromOrderClient($scope.orderToUpdate);
                orderRegion.id_operation = operation.id;
                orderRegion.equipment_key = $scope.orderToUpdate.equipment_key;
                $scope.cancelUpdate();
                await orderRegion.set();
            } else {
                let idsOrder = $scope.ordersClient.selected.map(order => order.id);
                await $scope.ordersClient.addOperation(operation.id, idsOrder);
                await $scope.validateOrders($scope.getSelectedOrders());
                await Promise.all([
                    await $scope.syncOrders('WAITING'),
                    await $scope.initOperation()
                ])
            }
        };
        $scope.showPriceProposalOrNot = (order:OrderClient) => {
            return  order.price_proposal !== null? order.priceProposalTTCTotal : order.priceTTCtotal;
        };

        $scope.updateOrder = (order: OrderClient) => {
            $scope.orderToUpdate = order;
            $scope.redirectTo('/order/update');

        };

        $scope.cancelUpdate = () => {
            if ($scope.ordersClient.selected[0])
                $scope.ordersClient.selected[0].selected = false;
            $scope.redirectTo('/order/waiting');
        };
        $scope.updateOrderConfirm = async () => {

            await $scope.selectOperationForOrder();
        };

        $scope.updateLinkedOrderConfirm = async () => {
            $scope.cancelUpdate();
        };

        $scope.getTotal = () => {

            return ($scope.orderToUpdate.amount * $scope.orderToUpdate.priceTTCtotal).toFixed(2);
        };

        $scope.orderShow = (order:OrderClient) => {
            if(order.rank !== undefined){
                if(order.campaign.priority_field === PRIORITY_FIELD.ORDER && order.campaign.orderPriorityEnable()){
                    return order.rank = order.rank + 1;
                } else if (order.campaign.priority_field === PRIORITY_FIELD.PROJECT && order.project.preference !== null && order.campaign.projectPriorityEnable()){
                    return order.rank = order.project.preference + 1;
                }
            }
            return order.rank = lang.translate("lystore.order.not.prioritized");
        };
    }]);