import { ng, template, moment } from 'entcore';
import { Agent, Supplier, Contract, Tag } from '../model';

export const administratorController = ng.controller('administratorController',
    ['$scope', ($scope) => {
        $scope.display = {
            lightbox : {
                agent: false,
                supplier: false,
                contract: false,
                tag: false
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
            }
        };

        $scope.openAgentForm = (agent?: Agent) => {
            $scope.agent = agent || new Agent();
            template.open('agent.lightbox', 'administrator/agent/agent-form');
            $scope.display.lightbox.agent = true;
        };

        $scope.validAgent = async (agent: Agent) => {
            await agent.save();
            await $scope.agents.sync();
            delete $scope.agent;
            $scope.allAgentSelected = false;
            $scope.display.lightbox.agent = false;
            $scope.$apply();
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
            $scope.$apply();
        };

        $scope.openAgentsDeletion = () => {
            template.open('agent.lightbox', 'administrator/agent/agent-delete-validation');
            $scope.display.lightbox.agent = true;
            $scope.$apply();
        };

        $scope.switchAllAgent = (allAgentSelected: boolean) => {
            allAgentSelected ? $scope.agents.selectAll() : $scope.agents.deselectAll();
            $scope.$apply();
        };

        $scope.openSupplierForm = (supplier?: Supplier) => {
            $scope.supplier = supplier || new Supplier();
            template.open('supplier.lightbox', 'administrator/supplier/supplier-form');
            $scope.display.lightbox.supplier = true;
        };

        $scope.validSupplier = async (supplier: Supplier) => {
            await supplier.save();
            await $scope.suppliers.sync();
            delete $scope.supplier;
            $scope.allHolderSelected = false;
            $scope.display.lightbox.supplier = false;
            $scope.$apply();
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
            $scope.$apply();
        };

        $scope.switchAllSupplier = (allSupplierSelected: boolean) => {
            allSupplierSelected ? $scope.suppliers.selectAll() : $scope.suppliers.deselectAll();
            $scope.$apply();
        };

        $scope.openSuppliersDeletion = () => {
            template.open('supplier.lightbox', 'administrator/supplier/supplier-delete-validation');
            $scope.display.lightbox.supplier = true;
            $scope.$apply();
        };

        $scope.openContractForm = (contract?: Contract) => {
            $scope.contract = contract || new Contract();
            $scope.contract.start_date = (contract !== undefined ? moment(contract.start_date) : new Date());
            template.open('contract.lightbox', 'administrator/contract/contract-form');
            $scope.display.lightbox.contract = true;
        };

        $scope.validContract = async (contract: Contract) => {
            await contract.save();
            await $scope.contracts.sync();
            $scope.display.lightbox.contract = false;
            delete $scope.contract;
            $scope.$apply();
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

        $scope.switchAllContract = (allContractSelected: boolean) => {
            allContractSelected ? $scope.contracts.selectAll() : $scope.contracts.deselectAll();
            $scope.$apply();
        };

        $scope.openContractsDeletion = () => {
            template.open('contract.lightbox', 'administrator/contract/contract-delete-validation');
            $scope.display.lightbox.contract = true;
            $scope.$apply();
        };

        $scope.deleteContracts = async (contracts: Contract[]) => {
            await $scope.contracts.delete(contracts);
            await $scope.contracts.sync();
            $scope.allContractSelected = false;
            $scope.display.lightbox.contract = false;
            $scope.$apply();
        };

        $scope.openTagForm = (tag?: Tag) => {
            $scope.tag = tag || new Tag();
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
            $scope.$apply();
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
            $scope.$apply();
        };

        $scope.switchAllTag = () => {
            $scope.allTagSelected ? $scope.tags.selectAll() : $scope.tags.deselectAll();
        };

    }]);
