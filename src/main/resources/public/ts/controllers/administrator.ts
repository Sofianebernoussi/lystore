import { ng, template } from 'entcore';
import { Agent, Supplier } from '../model';

export const administratorController = ng.controller('administratorController',
    ['$scope', ($scope) => {
        $scope.display = {
            lightbox : {
                agent: false,
                supplier: false
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
    }]);
