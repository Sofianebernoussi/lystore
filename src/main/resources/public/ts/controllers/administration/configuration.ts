import { ng, template, moment, _, idiom as lang } from 'entcore';
import { Mix } from 'entcore-toolkit';
import {
    Agent,
    Supplier,
    Contract,
    Tag,
    Equipment,
    EquipmentOption,
    TechnicalSpec,
    COMBO_LABELS,
    Campaign,
    Utils,
    StructureGroup,
    Notification
} from '../../model';

export const configurationController = ng.controller('configurationController',
    ['$scope', ($scope) => {
        $scope.COMBO_LABELS = COMBO_LABELS;
        $scope.display = {
            lightbox : {
                agent: false,
                supplier: false,
                contract: false,
                tag: false,
                equipment: false,
                campaign : false,
                structureGroup : false
            },
            input : {
                group : []
            }
        };

        $scope.sort = {
            agent: {
                type: 'name',
                reverse: false
            },
            supplier: {
                type: 'name',
                reverse: false
            },
            contract: {
                type: 'start_date',
                reverse: false
            },
            tag: {
                type: 'name',
                reverse: false
            },
            equipment: {
                type: 'name',
                reverse: false
            }
        };

        $scope.search = {};

        $scope.openAgentForm = (agent: Agent = new Agent()) => {
            $scope.agent = new Agent();
            Mix.extend($scope.agent, agent);
            template.open('agent.lightbox', 'administrator/agent/agent-form');
            $scope.display.lightbox.agent = true;
            Utils.safeApply($scope);
        };

        $scope.validAgent = async (agent: Agent) => {
            await agent.save();
            await $scope.agents.sync();
            delete $scope.agent;
            $scope.allAgentSelected = false;
            $scope.display.lightbox.agent = false;
            Utils.safeApply($scope);
        };

        $scope.cancelAgentForm = () => {
            $scope.display.lightbox.agent = false;
            delete $scope.agent;
        };

        $scope.deleteAgents = async (agents: Agent[]) => {
            await $scope.agents.delete(agents);
            await $scope.agents.sync();
            $scope.allAgentSelected = false;
            $scope.display.lightbox.agent = false;
            Utils.safeApply($scope);
        };

        $scope.openAgentsDeletion = () => {
            template.open('agent.lightbox', 'administrator/agent/agent-delete-validation');
            $scope.display.lightbox.agent = true;
            Utils.safeApply($scope);
        };

        $scope.openSupplierForm = (supplier: Supplier = new Supplier()) => {
            $scope.supplier = new Supplier();
            Mix.extend($scope.supplier, supplier);
            template.open('supplier.lightbox', 'administrator/supplier/supplier-form');
            $scope.display.lightbox.supplier = true;
            Utils.safeApply($scope);
        };

        $scope.validSupplier = async (supplier: Supplier) => {
            await supplier.save();
            await $scope.suppliers.sync();
            delete $scope.supplier;
            $scope.allHolderSelected = false;
            $scope.display.lightbox.supplier = false;
            Utils.safeApply($scope);
        };

        $scope.cancelSupplierForm = () => {
            $scope.display.lightbox.supplier = false;
            delete $scope.supplier;
        };

        $scope.deleteSuppliers = async (suppliers: Supplier[]) => {
            await $scope.suppliers.delete(suppliers);
            await $scope.suppliers.sync();
            $scope.allSupplierSelected = false;
            $scope.display.lightbox.supplier = false;
            Utils.safeApply($scope);
        };

        $scope.openSuppliersDeletion = () => {
            template.open('supplier.lightbox', 'administrator/supplier/supplier-delete-validation');
            $scope.display.lightbox.supplier = true;
            Utils.safeApply($scope);
        };

        $scope.openContractForm = (contract: Contract = new Contract()) => {
            if (contract) contract.syncBooleans();
            $scope.contract = new Contract();
            Mix.extend($scope.contract, contract);
            $scope.contract.start_date = (contract !== undefined ? moment(contract.start_date) : new Date());
            template.open('contract.lightbox', 'administrator/contract/contract-form');
            $scope.display.lightbox.contract = true;
            Utils.safeApply($scope);
        };

        $scope.validContract = async (contract: Contract) => {
            contract.end_date = moment(contract.start_date).add(1, 'y').format('YYYY-MM-DD');
            await contract.save();
            await $scope.contracts.sync(true);
            $scope.display.lightbox.contract = false;
            delete $scope.contract;
            Utils.safeApply($scope);
        };

        $scope.validContractForm = (contract: Contract) => {
            if (contract !== undefined) {
                return contract.name !== undefined
                    && contract.name.trim() !== ''
                    && contract.reference !== undefined
                    && contract.reference.trim() !== ''
                    && contract.start_date !== undefined
                    && contract.nb_renewal !== undefined
                    && contract.nb_renewal.trim() !== ''
                    && contract.id_contract_type !== undefined
                    && typeof contract.id_contract_type === 'number'
                    && contract.id_supplier !== undefined
                    && typeof contract.id_supplier === 'number'
                    && contract.id_agent !== undefined
                    && typeof contract.id_agent === 'number';
            }
        };

        $scope.cancelContractForm = () => {
            $scope.display.lightbox.contract = false;
            template.close('lightbox.contract');
            delete $scope.contract;
        };


        $scope.openContractsDeletion = () => {
            template.open('contract.lightbox', 'administrator/contract/contract-delete-validation');
            $scope.display.lightbox.contract = true;
            Utils.safeApply($scope);
        };

        $scope.deleteContracts = async (contracts: Contract[]) => {
            await $scope.contracts.delete(contracts);
            await $scope.contracts.sync(true);
            $scope.allContractSelected = false;
            $scope.display.lightbox.contract = false;
            Utils.safeApply($scope);
        };

        $scope.openTagForm = (tag: Tag = new Tag()) => {
            $scope.tag = new Tag();
            Mix.extend($scope.tag, tag);
            template.open('tag.lightbox', 'administrator/tag/tag-form');
            $scope.display.lightbox.tag = true;
        };

        $scope.cancelTagForm = () => {
            $scope.display.lightbox.tag = false;
            template.close('tag.lightbox');
            delete $scope.tag;
        };

        $scope.validTag = async (tag: Tag) => {
            await tag.save();
            await $scope.tags.sync(true);
            $scope.display.lightbox.tag = false;
            Utils.safeApply($scope);
            delete $scope.tag;
            template.close('tag.lightbox');
        };

        $scope.validTagForm = (tag: Tag) => {
            return tag.name !== undefined
                && tag.name.trim() !== ''
                && tag.color !== undefined
                && tag.color.trim() !== '';
        };

        $scope.openTagsDeletion = () => {
            template.open('tag.lightbox', 'administrator/tag/tag-delete-validation');
            $scope.display.lightbox.tag = true;
        };

        $scope.deleteTags = async (tags: Tag[]) => {
            await $scope.tags.delete(tags);
            await $scope.tags.sync(true);
            $scope.allTagSelected = false;
            $scope.display.lightbox.tag = false;
            Utils.safeApply($scope);
        };

        $scope.openEquipmentForm = (equipment: Equipment = new Equipment()) => {
            $scope.redirectTo('/equipments/create');
            $scope.equipment = new Equipment();
            Mix.extend($scope.equipment, equipment);
            $scope.equipment.tags = $scope.equipment.tags.map(
                (tagId) => _.findWhere($scope.tags.all, { id: tagId })
            );
            Utils.safeApply($scope);
        };

        $scope.addTagToEquipment = (tag: Tag) => {
            if (!_.contains($scope.equipment.tags, tag)) {
                $scope.equipment.tags.push(tag);
            }
            delete $scope.equipment._tag;
        };

        $scope.removeTagToEquipment = (tag: Tag) => {
            $scope.equipment.tags = _.without($scope.equipment.tags, tag);
        };

        $scope.validEquipmentOptions = (options: EquipmentOption[]) => {
            if ( options.length > 0 ) {
                let valid = true;
                for (let i = 0; i < options.length; i++) {
                    if ( options[i].name === undefined
                        || options[i].name.trim() === ''
                        || options[i].name === null
                        || options[i].price === (undefined || null)
                        || isNaN(options[i].price)
                        || options[i].amount === undefined
                        || options[i].amount.toString().trim() === ''
                        || isNaN(parseInt(options[i].amount.toString()))
                        || typeof(options[i].required ) !== 'boolean'
                        || options[i].id_tax === undefined ) {
                        valid = false;
                        break;
                    }
                }
                return valid;
            } else {
                return true;
            }
        };

        $scope.validEquipmentForm = (equipment: Equipment) => {
            return equipment.name !== undefined
                && equipment.name.trim() !== ''
                && equipment.price !== undefined
                && equipment.price.toString().trim() !== ''
                && !isNaN(parseFloat(equipment.price.toString()))
                && equipment.id_contract !== undefined
                && equipment.id_tax !== undefined
                && equipment.tags.length > 0
                && $scope.validEquipmentOptions (equipment.options);
        };

        $scope.validEquipment = async (equipment: Equipment) => {
            await equipment.save();
            $scope.redirectTo('/equipments');
            Utils.safeApply($scope);
        };

        $scope.openEquipmentsDeletion = () => {
            template.open('equipment.lightbox', 'administrator/equipment/equipment-delete-validation');
            $scope.display.lightbox.equipment = true;
        };

        $scope.deleteEquipments = async (equipments) => {
            await $scope.equipments.delete(equipments);
            await $scope.equipments.sync();
            $scope.allEquipmentSelected = false;
            $scope.display.lightbox.equipment = false;
            Utils.safeApply($scope);
        };

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.equipmentSearchFilter = (equipment: Equipment) => {
            return $scope.search.equipment !== undefined
                ? equipment.name.toLowerCase().includes($scope.search.equipment.toLowerCase())
                || $scope.calculatePriceOfEquipment(equipment, false, 2).toString().includes($scope.search.equipment.toLowerCase())
                || equipment.price.toString().includes($scope.search.equipment.toLowerCase())
                || $scope.contracts.get(equipment.id_contract).supplier_display_name.toLowerCase().includes($scope.search.equipment.toLowerCase())
                || $scope.contracts.get(equipment.id_contract).name.toLowerCase().includes($scope.search.equipment.toLowerCase())
                || lang.translate('lystore.' + equipment.status).toLowerCase().includes($scope.search.equipment.toLowerCase())
                : true;
        };

        $scope.addTechnicalSpec = (equipment: Equipment) => {
            equipment.technical_specs.push(new TechnicalSpec());
            Utils.safeApply($scope);
        };

        $scope.dropTechnicalSpec = (equipment: Equipment, technicalSpec: TechnicalSpec) => {
            equipment.technical_specs = _.without(equipment.technical_specs, technicalSpec);
            Utils.safeApply($scope);
        };

        $scope.calculatePriceOption = (price , tax_id, amount) => {
            let tax_value = parseFloat(_.findWhere($scope.taxes.all, {id: tax_id}).value) ;
            if (tax_value !== undefined ) {
                let priceFloat = parseFloat(price);
                let price_TTC = $scope.calculatePriceTTC(priceFloat, tax_value);
                let Price_TTC_QTe =  (price_TTC * parseFloat(amount));
                return (!isNaN(Price_TTC_QTe) && price_TTC !== '') ?  Price_TTC_QTe.toFixed(2) : '';
            } else {
                return NaN;
            }
        };

        $scope.addOptionLigne = () => {
            let option = new EquipmentOption();
            $scope.equipment.options.push(option);
            Utils.safeApply($scope);
        };

        $scope.openCampaignForm =  (campaign: Campaign = new Campaign()) => {
            let id = campaign.id ;
            id ? $scope.redirectTo('/campaigns/update') :  $scope.redirectTo('/campaigns/create');
            $scope.campaign = new Campaign();
            Mix.extend($scope.campaign, campaign);
            id ? $scope.updateSelectedCampaign(id) : null ;
            Utils.safeApply($scope);
        };

        $scope.updateSelectedCampaign = async (id) => {
            await $scope.tags.sync();
            await $scope.campaign.sync(id, $scope.tags.all);
            await $scope.structureGroups.sync();
            $scope.structureGroups.all =  $scope.structureGroups.all.map((group) => {
                let Cgroup =  _.findWhere($scope.campaign.groups, {id: group.id});
                if (Cgroup !== undefined) { group.selected = true ; group.tags = Cgroup.tags ; }
                return group;
            });
            $scope.structureGroups.updateSelected();
        };

        $scope.openCampaignsDeletion = () => {
            template.open('campaign.lightbox', 'administrator/campaign/campaign-delete-validation');
            $scope.display.lightbox.campaign = true;
        };

        $scope.validCampaignForm = (campaign: Campaign) => {
            return campaign.name !== undefined
                && campaign.name.trim() !== ''
                && _.findWhere($scope.structureGroups.all, {selected: true}) !== undefined
                && (_.where($scope.structureGroups.all, {selected : true } ).length > 0 ) ? _.every(_.where($scope.structureGroups.all, {selected : true}), (structureGroup) => { return structureGroup.tags.length > 0; } ) : false ;

        };

        $scope.addTagToCampaign = (index) => {
            $scope.structureGroups.all[index].tags.push($scope.search.tag[index]);
            $scope.structureGroups.all[index].tags = _.uniq($scope.structureGroups.all[index].tags);
            $scope.display.input.group[index] = false;
            Utils.safeApply($scope);
        };

        $scope.deleteTagFromCampaign = (index, tag) => {
            $scope.structureGroups.all[index].tags = _.without($scope.structureGroups.all[index].tags, tag);
            Utils.safeApply($scope);
        };

        $scope.validCampaign = async(campaign: Campaign) => {
            $scope.campaign.groups = [];
            $scope.structureGroups.all.map((group) => $scope.selectCampaignsStructureGroup(group) );
            _.uniq($scope.campaign.groups);
            await campaign.save();
            $scope.redirectTo('/campaigns');
            Utils.safeApply($scope);
        };

        $scope.deleteCampaigns = async (campaigns) => {
            await $scope.campaigns.delete(campaigns);
            await $scope.campaigns.sync();
            $scope.allCampaignSelected = false;
            $scope.display.lightbox.campaign = false;
            Utils.safeApply($scope);
        };

        $scope.selectCampaignsStructureGroup = (group) => {
            group.selected ? $scope.campaign.groups.push(group) : $scope.campaign.groups = _.reject($scope.campaign.groups, (groups) => { return groups.id === group.id; } );
        };

        $scope.openStructureGroupForm = (structureGroup: StructureGroup = new StructureGroup()) => {
            $scope.redirectTo('/structureGroups/create');
            $scope.structureGroup = new StructureGroup();
            Mix.extend($scope.structureGroup, structureGroup);
            $scope.structureGroup.structureIdToObject(structureGroup.structures, $scope.structures);
            Utils.safeApply($scope);
        };

        $scope.structuresFilter = (structureRight) => {
            return _.findWhere($scope.structureGroup.structures, {id : structureRight.id}) === undefined;
        };

        $scope.addStructuresInGroup = () => {
            $scope.structures.deselectAll($scope.structures.selected);
            $scope.structureGroup.structures.push.apply($scope.structureGroup.structures, $scope.structures.selectedElements);
            $scope.structureGroup.structures = _.uniq($scope.structureGroup.structures);
            $scope.structures.selectedElements = [];
            Utils.safeApply($scope);
        };

        $scope.deleteStructuresofGroup = () => {
            $scope.structureGroup.structures = _.difference($scope.structureGroup.structures, $scope.structureGroup.structures.filter(structureRight => structureRight.selected));
            $scope.structures.deselectAll($scope.structures.selectedElements);
            $scope.structures.selectedElements = [];
            Utils.safeApply($scope);
        };

        $scope.validStructureGroupForm  = (structureGroup: StructureGroup) => {
            return structureGroup.name !== undefined
                && structureGroup.name.trim() !== ''
                && structureGroup.structures.length > 0;
        };

        $scope.validStructureGroup = async (structureGroup: StructureGroup) => {
            await structureGroup.save();
            $scope.redirectTo('/structureGroups');
            Utils.safeApply($scope);
        };

        $scope.openStructureGroupDeletion = (structureGroup: StructureGroup) => {
            $scope.structureGroup = structureGroup;
            template.open('structureGroup.lightbox', 'administrator/structureGroup/structureGroup-delete');
            $scope.display.lightbox.structureGroup = true;
        };

        $scope.deleteStructureGroup = async () => {
            await $scope.structureGroup.delete();
            await $scope.structureGroups.sync();
            $scope.display.lightbox.structureGroup = false;
            Utils.safeApply($scope);
        };

        $scope.setStatus = async (status: string) => {
            await $scope.equipments.setStatus(status);
            await $scope.equipments.sync();
            $scope.allEquipmentSelected = false;
            $scope.notifications.push(new Notification('lystore.status.update.ok', 'confirm'));
            Utils.safeApply($scope);
        };
    }]);
