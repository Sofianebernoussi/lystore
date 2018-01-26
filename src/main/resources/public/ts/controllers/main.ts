import { ng, template, notify, idiom as lang, moment, model, Behaviours} from 'entcore';
import {
    Structures,
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
    StructureGroup,
    Utils,
    Equipment
} from '../model';

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location',
    ($scope, route, $location) => {
        template.open('main', 'administrator/main');
        $scope.lang = lang;
        $scope.agents = new Agents();
        $scope.suppliers = new Suppliers();
        $scope.structures = new Structures();
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
       /* $scope.structures.sync().then(() => {
            if ($scope.structures.all.length > 0) {
                $scope.structure = $scope.structures.all[0];
            }  else {
                notify.error('Aucune structure');
            }
        });*/

        route({
            main:  async() => {
                if ($scope.isManager() || $scope.isAdministrator()) {
                    template.open('main-profile', 'administrator/management-main');
                }
                else if ($scope.isPersonnel() && !$scope.isManager() && !$scope.isAdministrator()) {
                    template.open('main-profile', 'customer/campaign/campaign-list');
                    await $scope.campaigns.sync();
                    Utils.safeApply($scope);
                }
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
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                await $scope.tags.sync();
                await $scope.structureGroups.sync();
                Utils.safeApply($scope);
            },
            updateCampaigns: async () => {
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                Utils.safeApply($scope);
            },
            managePurse: (params) => {
                $scope.campaign = $scope.campaigns.get(parseInt(params.idCampaign));
                template.open('campaigns-main', 'administrator/campaign/purse/manage-purse');
                Utils.safeApply($scope);
            },
            manageStructureGroups: async () => {
                template.open('administrator-main', 'administrator/structureGroup/structureGroup-container');
                await $scope.structureGroups.sync();
                template.open('structureGroups-main', 'administrator/structureGroup/manage-structureGroup');
                await $scope.structures.sync();
                Utils.safeApply($scope);
            },
            createStructureGroup: async () => {
                template.open('structureGroups-main', 'administrator/structureGroup/structureGroup-form');
                Utils.safeApply($scope);
            },
            campaignCatalog : async (params) => {
                let id = params.idCampaign;
                $scope.idIsInteger(id);
                await $scope.equipments.sync(id);
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/catalog-list');
                template.close('right-side');
                Utils.safeApply($scope);
            },
            equipmentDetail : async (params) => {
                let idCampaign = params.idCampaign;
                let idEquipment = params.idEquipment;
                $scope.idIsInteger(idCampaign);
                $scope.idIsInteger(idEquipment);
                template.open('right-side', 'customer/campaign/catalog/equipment-detail');
                Utils.safeApply($scope);
            },
            campaignOrder : async (params) => {
                Utils.safeApply($scope);
            },
            campaignBasket : async (params) => {
                Utils.safeApply($scope);
            }
        });
        $scope.idIsInteger = (id) => {
            try {
                parseInt(id) ;
            } catch (e) {
                $scope.redirectTo(`/`);
                Utils.safeApply($scope);
            }
        };
        $scope.isPersonnel = () => {
            return model.me.type === 'PERSEDUCNAT';
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

        $scope.formatDate = (date: string | Date, format: string) => {
            return moment(date).format(format);
        };
        $scope.calculatePriceTTC = (price, tax_value, roundNumber?: number) => {
            let priceFloat = parseFloat(price);
            let taxFloat = parseFloat(tax_value);
            let price_TTC = (( priceFloat + ((priceFloat *  taxFloat) / 100)));
            return (!isNaN(price_TTC)) ? (roundNumber ? price_TTC.toFixed(roundNumber) : price_TTC ) : '';
        };
        $scope.calculatePriceOfEquipment = (equipment: Equipment, selectedOptions: boolean, roundNumber?: number) => {
            let price = parseFloat( $scope.calculatePriceTTC(equipment.price , equipment.tax_amount) );
            equipment.options.map((option) => {
                (option.required === true  || (selectedOptions ? option.selected === true : false) )
                    ? price += parseFloat($scope.calculatePriceTTC(option.price , option.tax_amount) )
                    : null ;
            });
            return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
        };
    }]);
