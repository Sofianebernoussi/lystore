import { ng, template, notify, idiom as lang, moment, model, Behaviours, _} from 'entcore';
import {
    Agents,
    Suppliers,
    Programs,
    ContractTypes,
    Contracts,
    Tags,
    Equipments,
    Taxes,
    Logs,
    StructureGroups,
    Campaigns,
    Campaign,
    Utils,
    Equipment,
    Baskets,
    Structures,
    Basket,
    Notification,
    OrdersEquipments,
    OrderEquipment
} from '../model';

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', '$rootScope',
    ($scope, route, $location, $rootScope) => {
        template.open('main', 'main');

        $scope.display = {
            equipment: false
        };
        $scope.structures = [];
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
        $scope.logs = new Logs();
        $scope.baskets = new Baskets();
        $scope.ordersEquipments = new OrdersEquipments();
        route({
            main:  async() => {
                if ($scope.isManager() || $scope.isAdministrator()) {
                    template.open('main-profile', 'administrator/management-main');
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
                $scope.programs.sync();
                Utils.safeApply($scope);
            },
            manageEquipmentTags: async () => {
                template.open('administrator-main', 'administrator/tag/manage-tags');
                await $scope.tags.sync(true);
                Utils.safeApply($scope);
            },
            manageEquipments: async () => {
                template.open('administrator-main', 'administrator/equipment/equipment-container');
                template.open('equipments-main', 'administrator/equipment/manage-equipments');
                await $scope.equipments.sync();
                await $scope.contracts.sync();
                $scope.taxes.sync();
                $scope.tags.sync();
                Utils.safeApply($scope);
            },
            createEquipment: async () => {
                if (template.isEmpty('administrator-main')) { $scope.redirectTo('/equipments'); }
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
                if (template.isEmpty('administrator-main')) { $scope.redirectTo('/campaigns'); }
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                await $scope.tags.sync();
                await $scope.structureGroups.sync();
                Utils.safeApply($scope);
            },
            updateCampaigns: async () => {
                if (template.isEmpty('administrator-main')) { $scope.redirectTo('/campaigns'); }
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                Utils.safeApply($scope);
            },
            managePurse: async (params) => {
                if (template.isEmpty('administrator-main')) { $scope.redirectTo('/campaigns'); }
                $scope.campaign = $scope.campaigns.get(parseInt(params.idCampaign));
                template.open('campaigns-main', 'administrator/campaign/purse/manage-purse');
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
                if (template.isEmpty('administrator-main')) { $scope.redirectTo('/structureGroups'); }
                template.open('structureGroups-main', 'administrator/structureGroup/structureGroup-form');
                Utils.safeApply($scope);
            },
            campaignCatalog : async (params) => {
                let id = params.idCampaign;
                $scope.idIsInteger(id);
                $scope.current.structure ? await $scope.equipments.sync(id, $scope.current.structure.id) : null ;
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/catalog-list');
                template.close('right-side');
                $scope.display.equipment = false;
                Utils.safeApply($scope);
            },
            equipmentDetail : async (params) => {
                let idCampaign = params.idCampaign;
                let idEquipment = params.idEquipment;
                $scope.idIsInteger(idCampaign);
                $scope.idIsInteger(idEquipment);
                $scope.current.structure
                    ? await $scope.initBasketItem( parseInt(idEquipment), parseInt(idCampaign), $scope.current.structure.id )
                    : null;
                template.open('right-side', 'customer/campaign/catalog/equipment-detail');
                window.scrollTo(0, 0);
                Utils.safeApply($scope);
            },
            campaignOrder : async (params) => {
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                $scope.current.structure
                    ? await $scope.ordersEquipments.sync(idCampaign, $scope.current.structure.id )
                    : null;
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/order/manage-orderEquipment');
                Utils.safeApply($scope);
            },
            campaignBasket : async (params) => {
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                $scope.current.structure
                ? await $scope.baskets.sync(idCampaign, $scope.current.structure.id )
                : null;
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/basket/manage-basket');
                Utils.safeApply($scope);
            }
        });
        $scope.initBasketItem = async (idEquipment: number, idCampaign: number, structure) => {
            $scope.equipment = _.findWhere( $scope.equipments.all, {id: idEquipment});
            if ($scope.equipment === undefined && !isNaN(idEquipment)) {
                $scope.equipment = new Equipment();
                await $scope.equipment.sync(idEquipment);
            }
            $scope.basket = new Basket($scope.equipment , idCampaign, structure);
        };
        $scope.idIsInteger = (id) => {
            try {
                id = parseInt(id) ;
                if (isNaN(id) ) {
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
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.administrator) ;
        };

        $scope.redirectTo = (path: string) => {
            $location.path(path);
        };

        $rootScope.$on('eventEmitedCampaign', function(event, data) {
            $scope.campaign = data;
        });

        $scope.formatDate = (date: string | Date, format: string) => {
            return moment(date).format(format);
        };

        $scope.calculatePriceTTC = (price, tax_value, roundNumber?: number) => {
            let priceFloat = parseFloat(price);
            let taxFloat = parseFloat(tax_value);
            let price_TTC = (( priceFloat + ((priceFloat *  taxFloat) / 100)));
            return (!isNaN(price_TTC)) ? (roundNumber ? price_TTC.toFixed(roundNumber) : price_TTC ) : '';
        };

        /**
         * Calculate the price of an equipment
         * @param {Equipment} equipment
         * @param {boolean} selectedOptions [Consider selected options or not)
         * @param {number} roundNumber [number of digits after the decimal point]
         * @returns {string | number}
         */
        $scope.calculatePriceOfEquipment = (equipment: any, selectedOptions: boolean, roundNumber?: number) => {
            let price = parseFloat( $scope.calculatePriceTTC(equipment.price , equipment.tax_amount) );
            equipment.options.map((option) => {
                (option.required === true  || (selectedOptions ? option.selected === true : false) )
                    ? price += parseFloat($scope.calculatePriceTTC(option.price , option.tax_amount) )
                    : null ;
            });
            return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
        };
        $scope.initStructures = async () => {
            for ( let i = 0 ; i < model.me.structures.length ; i++) {
                $scope.structures[i] = {
                    id: model.me.structures[i],
                    name : model.me.structureNames[i],
                    i : i
                };
            }
            $scope.current.structure = $scope.structures[0];
        };

        $scope.avoidDecimals = (event) => {
            return event.charCode >= 48 && event.charCode <= 57;
        };

        $scope.notifyBasket = (action: String, basket: Basket) => {
            let messageForOne =  basket.amount + ' ' + lang.translate('article') + ' "'
                + basket.equipment.name  + '" ' + lang.translate('lystore.basket.' + action + '.article');
            let messageForMany = basket.amount + ' ' + lang.translate('articles') + ' "'
                + basket.equipment.name  + '" ' + lang.translate('lystore.basket.'  + action + '.articles');
            $scope.notifications.push(new Notification(basket.amount === 1 ?  messageForOne : messageForMany , 'confirm'));
        };
        $scope. initCampaign = async (structure) => {
            if (structure) {
                await $scope.campaigns.sync(structure.id);
                Utils.safeApply($scope);
            }
        };
        $rootScope.$on('$routeChangeSuccess', ( $currentRoute, $previousRoute, $location ) => {
            if ( $location === undefined) {
                $scope.redirectTo('/');
            }
        });
        if ($scope.isManager() || $scope.isAdministrator()) {
            template.open('main-profile', 'administrator/management-main');
        }
        else if ($scope.hasAccess() && !$scope.isManager() && !$scope.isAdministrator()) {
            template.open('main-profile', 'customer/campaign/campaign-list');
        }
        Utils.safeApply($scope);
    }]);
