import { ng, template, moment, _, idiom as lang } from 'entcore';
import { Mix } from 'entcore-toolkit';
import {
Agent,
Supplier,
Contract,
Tag,
Equipment,
COMBO_LABELS,
Utils
} from '../model';

export const administratorController = ng.controller('administratorController',
    ['$scope', ($scope) => {
        $scope.COMBO_LABELS = COMBO_LABELS;
        $scope.display = {
            lightbox : {
                agent: false,
                supplier: false,
                contract: false,
                tag: false,
                equipment: false
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
            $scope.contract = new Contract();
            Mix.extend($scope.contract, contract);
            $scope.contract.start_date = (contract !== undefined ? moment(contract.start_date) : new Date());
            template.open('contract.lightbox', 'administrator/contract/contract-form');
            $scope.display.lightbox.contract = true;
            Utils.safeApply($scope);
        };

        $scope.validContract = async (contract: Contract) => {
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
            await $scope.tags.sync();
            $scope.display.lightbox.tag = false;
            Utils.safeApply($scope);
            delete $scope.tag;
            template.close('tag.lightbox');
        };

        $scope.validTagForm = (tag: Tag) => {
            return tag.name !== undefined
                && tag.name.trim() !== ''
                && tag.color !== undefined
                && tag.color.trim() !== ''
        };

        $scope.openTagsDeletion = () => {
            template.open('tag.lightbox', 'administrator/tag/tag-delete-validation');
            $scope.display.lightbox.tag = true;
        };

        $scope.deleteTags = async (tags: Tag[]) => {
            await $scope.tags.delete(tags);
            await $scope.tags.sync();
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
        };

        $scope.removeTagToEquipment = (tag: Tag) => {
            $scope.equipment.tags = _.without($scope.equipment.tags, tag);
        };

        $scope.validEquipmentForm = (equipment: Equipment) => {
            return equipment.name !== undefined
                && equipment.name.trim() !== ''
                && equipment.price !== undefined
                && equipment.price.toString().trim() !== ''
                && !isNaN(parseFloat(equipment.price.toString()))
                && equipment.id_contract !== undefined
                && equipment.id_tax !== undefined
                && equipment.tags.length > 0;
        };

        $scope.validEquipment = async (equipment: Equipment) => {
            await equipment.save();
            $scope.redirectTo('/equipments');
            Utils.safeApply($scope);
        };

        $scope.tagFilter = function (tag) {
            return _.findWhere($scope.equipment.tags, { id : tag.id }) === undefined;
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
                || equipment.price.toString().includes($scope.search.equipment.toLowerCase())
                || $scope.contracts.get(equipment.id_contract).supplier_display_name.toLowerCase().includes($scope.search.equipment.toLowerCase())
                || $scope.contracts.get(equipment.id_contract).name.toLowerCase().includes($scope.search.equipment.toLowerCase())
                || lang.translate('lystore.' + equipment.status).toLowerCase().includes($scope.search.equipment.toLowerCase())
                : true;
        };

    }]);
