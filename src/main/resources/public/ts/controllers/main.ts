import {_, Behaviours, idiom as lang, model, moment, ng, template} from 'entcore';
import {
    Agents, Basket, Baskets, Campaign, Campaigns, Contracts, ContractTypes, Equipment, Equipments, EquipmentTypes,
    Exercises, Instructions, labels, Logs, Notification, Operations, OrderClient, OrdersClient, PRIORITY_FIELD,
    Programs, StructureGroups, Structures, Supplier, Suppliers, Tags, Taxes, Titles, Utils,
} from '../model';
import {Mix} from "entcore-toolkit";

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', '$rootScope',
    ($scope, route, $location, $rootScope) => {
        template.open('main', 'main');

        $scope.display = {
            equipment: false,
            lightbox: {lightBoxIsOpen: false,}
        };
        $scope.structures = new Structures();
        $scope.current = {};
        $scope.notifications = [];
        $scope.lang = lang;
        $scope.agents = new Agents();
        $scope.suppliers = new Suppliers();
        $scope.contractTypes = new ContractTypes();
        $scope.programs = new Programs();
        $scope.contracts = new Contracts();
        $scope.tags = new Tags();
        $scope.equipments = new Equipments();
        $scope.campaigns = new Campaigns();
        $scope.campaign = new Campaign();
        $scope.structureGroups = new StructureGroups();
        $scope.taxes = new Taxes();
        $scope.operations= new Operations();
        $scope.logs = new Logs();
        $scope.baskets = new Baskets();
        $scope.ordersClient = new OrdersClient();
        $scope.displayedOrders = new OrdersClient();
        $scope.equipmentTypes = new EquipmentTypes();
        $scope.instructions = new Instructions();
        $scope.exercises = new Exercises();
        $scope.equipments.eventer.on('loading::true', $scope.$apply);
        $scope.equipments.eventer.on('loading::false', $scope.$apply);

        route({
            main: async () => {
                if ($scope.isManager() || $scope.isAdministrator()) {
                    $scope.redirectTo('/campaigns');
                } else {
                    await $scope.initStructures();
                    await $scope.initCampaign($scope.current.structure);
                    template.open('main-profile', 'customer/campaign/campaign-list');
                }
                Utils.safeApply($scope);
            },
            manageAgents: async () => {
                template.open('administrator-main', 'administrator/agent/manage-agents');
                await $scope.agents.sync();
                Utils.safeApply($scope);
            },
            manageSuppliers: async () => {
                template.open('administrator-main', 'administrator/supplier/manage-suppliers');
                await $scope.suppliers.sync();
                Utils.safeApply($scope);
            },
            manageContracts: async () => {
                template.open('administrator-main', 'administrator/contract/manage-contract');
                await $scope.contracts.sync();
                $scope.agents.sync();
                $scope.suppliers.sync();
                $scope.contractTypes.sync();
                Utils.safeApply($scope);
            },
            manageEquipmentTags: async () => {
                template.open('administrator-main', 'administrator/tag/manage-tags');
                await $scope.tags.sync(true);
                Utils.safeApply($scope);
            },
            manageEquipments: async () => {
                delete $scope.equipment;

                template.open('administrator-main', 'administrator/equipment/equipment-container');
                template.open('equipments-main', 'administrator/equipment/manage-equipments');
                await $scope.equipments.sync();
                await $scope.contracts.sync();
                await $scope.equipmentTypes.sync();
                $scope.taxes.sync();
                $scope.tags.sync();
                Utils.safeApply($scope);
            },
            createEquipment: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/equipments');
                }
                template.open('equipments-main', 'administrator/equipment/equipment-form');
            },
            viewLogs: async () => {
                $scope.logs.reset();
                template.open('administrator-main', 'administrator/log/view-logs');
                Utils.safeApply($scope);
            },
            manageCampaigns: async () => {
                template.open('administrator-main', 'administrator/campaign/campaign_container');
                template.open('campaigns-main', 'administrator/campaign/manage-campaign');
                await $scope.campaigns.sync();
                Utils.safeApply($scope);
            },
            createCampaigns: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                Utils.safeApply($scope);
            },
            updateCampaigns: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                Utils.safeApply($scope);
            },
            managePurse: async (params) => {
                const campaign = $scope.campaigns.get(parseInt(params.idCampaign));
                if (template.isEmpty('administrator-main') || campaign === undefined || !campaign.purse_enabled) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/purse/manage-purse');
                Utils.safeApply($scope);
            },
            manageTitles: async (params) => {
                const campaign = $scope.campaigns.get(parseInt(params.idCampaign));
                campaign.titles = new Titles();
                if (template.isEmpty('administrator-main') || campaign === undefined) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/title/manage-title');
                Utils.safeApply($scope);
            },
            manageStructureGroups: async () => {
                template.open('administrator-main', 'administrator/structureGroup/structureGroup-container');
                await $scope.structureGroups.sync();
                template.open('structureGroups-main', 'administrator/structureGroup/manage-structureGroup');
                $scope.structures = new Structures();
                await $scope.structures.sync();
                Utils.safeApply($scope);
            },
            createStructureGroup: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/structureGroups');
                }
                template.open('structureGroups-main', 'administrator/structureGroup/structureGroup-form');
                Utils.safeApply($scope);
            },
            campaignCatalog: async (params) => {
                let id = params.idCampaign;
                $scope.idIsInteger(id);
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/catalog-list');
                template.close('right-side');
                $scope.display.equipment = false;
                Utils.safeApply($scope);
                $scope.current.structure ? await $scope.equipments.sync(id, $scope.current.structure.id) : null;
            },
            equipmentDetail: async (params) => {
                let idCampaign = params.idCampaign;
                let idEquipment = params.idEquipment;
                $scope.idIsInteger(idCampaign);
                $scope.idIsInteger(idEquipment);
                $scope.current.structure
                    ? await $scope.initBasketItem(parseInt(idEquipment), parseInt(idCampaign), $scope.current.structure.id)
                    : null;
                template.open('right-side', 'customer/campaign/catalog/equipment-detail');
                window.scrollTo(0, 0);
                Utils.safeApply($scope);
            },
            campaignOrder: async (params) => {
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                $scope.current.structure
                    ? await $scope.ordersClient.sync(null, [], idCampaign, $scope.current.structure.id)
                    : null;
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/order/manage-order');
                $scope.initCampaignOrderView();
                Utils.safeApply($scope);
            },
            campaignBasket: async (params) => {
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                $scope.current.structure
                    ? await $scope.baskets.sync(idCampaign, $scope.current.structure.id)
                    : null;
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/basket/manage-basket');
                Utils.safeApply($scope);
            },
            orderWaiting: async () => {
                await $scope.syncCampaignInputSelected();
                await $scope.openLightSelectCampaign();
                Utils.safeApply($scope);
            },
            orderSent: async () => {
                $scope.structures = new Structures();
                $scope.initOrders('SENT');
                template.open('administrator-main', 'administrator/order/order-sent');
                $scope.orderToSend = null;
                Utils.safeApply($scope);
            },
            orderClientValided: async () => {
                $scope.initOrders('VALID');
                template.open('administrator-main', 'administrator/order/order-valided');
                Utils.safeApply($scope);
            },
            previewOrder: async () => {
                template.open('administrator-main', 'administrator/order/order-send-prepare');
                template.open('sendOrder.preview', 'pdf/preview');
            },
            updateOrder: async () => {
                await $scope.contracts.sync();
                await $scope.contractTypes.sync();
                await $scope.initOrders('WAITING');
                template.open('administrator-main', 'administrator/order/order-update-form');
                Utils.safeApply($scope);

            },
            updateLinkedOrder: async () => {
                await $scope.initOrders('IN PROGRESS');
                template.open('administrator-main', 'administrator/order/order-update-form');
                Utils.safeApply($scope);
            },
            instruction: async () =>{
                await $scope.initInstructions();
                template.open('administrator-main', 'administrator/instruction/instruction-container');
                template.open('instruction-main', 'administrator/instruction/manage-instruction');
                Utils.safeApply($scope);
            },
            operation: async () =>{
                await $scope.initOperation();
                template.open('administrator-main', 'administrator/operation/operation-container');
                template.open('operation-main', 'administrator/operation/manage-operation');
                Utils.safeApply($scope);
            },
            operationOrders: async () =>{
                template.close('administrator-main');
                template.close('operation-main');
                if($scope.operations.selected.length === 0){
                    $scope.redirectTo(`/operation`);
                    return
                }
                $scope.operation = $scope.operations.selected[0];
                $scope.ordersClientByOperation = await $scope.operation.getOrders();
                template.open('administrator-main', 'administrator/operation/operation-container');
                template.open('operation-main', 'administrator/operation/operation-orders-list');
                Utils.safeApply($scope);
            },
        });
        $scope.initInstructions = async ()=>{
            await $scope.instructions.sync();
        };
        $scope.initCampaignOrderView=()=>{
            if( $scope.campaign.priority_enabled == true && $scope.campaign.priority_field == PRIORITY_FIELD.ORDER){
                template.open('order-list', 'customer/campaign/order/orders-by-equipment');
            } else {
                template.open('order-list', 'customer/campaign/order/orders-by-project');
            }
        };
        $scope.initOperation = async () =>{
            $scope.labelOperation = new labels();
            $scope.labelOperation.sync();
            await $scope.operations.sync();
        };
        $scope.initBasketItem = async (idEquipment: number, idCampaign: number, structure) => {
            $scope.equipment = _.findWhere($scope.equipments.all, {id: idEquipment});
            if ($scope.equipment === undefined && !isNaN(idEquipment)) {
                $scope.equipment = new Equipment();
                await $scope.equipment.sync(idEquipment);
            }
            $scope.basket = new Basket($scope.equipment, idCampaign, structure);
        };
        $scope.idIsInteger = (id) => {
            try {
                id = parseInt(id);
                if (isNaN(id)) {
                    $scope.redirectTo(`/`);
                    Utils.safeApply($scope);
                }
            } catch (e) {
                $scope.redirectTo(`/`);
                Utils.safeApply($scope);
            }
        };

        $scope.hasAccess = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.access);
        };

        $scope.isManager = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.manager);
        };

        $scope.isAdministrator = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.administrator);
        };

        $scope.redirectTo = (path: string) => {
            $location.path(path);
        };

        $rootScope.$on('eventEmitedCampaign', function (event, data) {
            $scope.campaign = data;
        });

        $scope.formatDate = (date: string | Date, format: string) => {
            return moment(date).format(format);
        };

        $scope.calculatePriceTTC = (price, tax_value, roundNumber?: number) => {
            let priceFloat = parseFloat(price);
            let taxFloat = parseFloat(tax_value);
            let price_TTC = ((priceFloat + ((priceFloat * taxFloat) / 100)));
            return (!isNaN(price_TTC)) ? (roundNumber ? price_TTC.toFixed(roundNumber) : price_TTC) : '';
        };

        /**
         * Calculate the price of an equipment
         * @param {Equipment} equipment
         * @param {boolean} selectedOptions [Consider selected options or not)
         * @param {number} roundNumber [number of digits after the decimal point]
         * @returns {string | number}
         */
        $scope.calculatePriceOfEquipment = (equipment: any, selectedOptions: boolean, roundNumber: number = 2) => {
            let price = parseFloat((equipment.price_proposal)? equipment.price_proposal : $scope.calculatePriceTTC(equipment.price, equipment.tax_amount));
            if(!equipment.price_proposal){
                equipment.options.map((option) => {
                    (option.required === true || (selectedOptions ? option.selected === true : false))
                        ? price += parseFloat($scope.calculatePriceTTC(option.price, option.tax_amount))
                        : null;
                });
            }

            return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price) : price;
        };
        $scope.initStructures = async () => {
            await $scope.structures.syncUserStructures();
            $scope.current.structure = $scope.structures.all[0];
        };

        $scope.avoidDecimals = (event) => {
            return event.charCode >= 48 && event.charCode <= 57;
        };

        $scope.notifyBasket = (action: String, basket: Basket) => {
            let messageForOne = basket.amount + ' ' + lang.translate('article') + ' "'
                + basket.equipment.name + '" ' + lang.translate('lystore.basket.' + action + '.article');
            let messageForMany = basket.amount + ' ' + lang.translate('articles') + ' "'
                + basket.equipment.name + '" ' + lang.translate('lystore.basket.' + action + '.articles');
            $scope.notifications.push(new Notification(basket.amount === 1 ? messageForOne : messageForMany, 'confirm'));
        };

        $scope.initCampaign = async (structure) => {
            if (structure) {
                await $scope.campaigns.sync(structure.id);
                Utils.safeApply($scope);
            }
        };

        $scope.syncOrders = async (status: string) => {
            await $scope.ordersClient.sync(status, $scope.structures.all);
            $scope.displayedOrders.all = $scope.ordersClient.all;
        };

        $scope.initOrders = async (status) => {
            $scope.structures = new Structures();
            await $scope.structures.sync();
            await $scope.structures.getStructureType();
            await $scope.syncOrders(status);
            Utils.safeApply($scope);
        };

        $scope.initOrdersForPreview = async (orders: OrderClient[]) => {
            $scope.orderToSend = new OrdersClient(Mix.castAs(Supplier, orders[0].supplier));
            $scope.orderToSend.all = Mix.castArrayAs(OrderClient, orders);
            $scope.orderToSend.preview = await $scope.orderToSend.getPreviewData();
            $scope.orderToSend.preview.index = 0;
        };
        $scope.syncCampaignInputSelected = async () => {
            $scope.campaignsForSelectInput = [];
            $scope.allCampaignsSelect = new Campaign(lang.translate("lystore.campaign.order.all"), '');
            $scope.allCampaignsSelect.id = 0;
            await $scope.campaigns.sync();
            $scope.campaignsForSelectInput = [...$scope.campaigns.all];
            $scope.campaignsForSelectInput.unshift( $scope.allCampaignsSelect);

        };
        $scope.openLightSelectCampaign = async () => {
            template.open('administrator-main');
            template.open('selectCampaign', 'administrator/order/select-campaign');
            $scope.display.lightbox.lightBoxIsOpen = true;
            $scope.initOrders('WAITING');
            Utils.safeApply($scope);
        };
        $scope.selectCampaignShow = (campaign?: Campaign) => {
            if(campaign){
                $scope.campaign = campaign;
                $scope.displayedOrders.all = $scope.ordersClient.all
                    .filter( order => order.campaign.id === campaign.id || campaign.id === 0);
                $scope.cancelSelectCampaign(false);
            } else {
                $scope.campaign = $scope.allCampaignsSelect;
                $scope.cancelSelectCampaign(true);
            }
        };
        $scope.getOrderWaitingFiltered = async (campaign:Campaign):Promise<void> =>{
            await $scope.initOrders('WAITING');
            $scope.selectCampaignShow(campaign);
        };
        $scope.cancelSelectCampaign = (initOrder: boolean) => {
            template.close('selectCampaign');
            $scope.display.lightbox.lightBoxIsOpen = false;
            if(initOrder) {
                $scope.displayedOrders.all = $scope.ordersClient.all;
            }
            template.open('administrator-main', 'administrator/order/order-waiting');
            Utils.safeApply($scope);
        };

        if ($scope.isManager() || $scope.isAdministrator()) {
            template.open('main-profile', 'administrator/management-main');
        }
        else if ($scope.hasAccess() && !$scope.isManager() && !$scope.isAdministrator()) {
            template.open('main-profile', 'customer/campaign/campaign-list');
        }
        Utils.safeApply($scope);
    }]);
