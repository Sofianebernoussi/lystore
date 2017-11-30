import { ng, template, notify, idiom as lang, moment } from 'entcore';
import {
    Structures,
    Agents,
    Suppliers,
    Programs,
    ContractTypes,
    Contracts
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
        $scope.structures.sync().then(() => {
            if ($scope.structures.all.length > 0) {
                $scope.structure = $scope.structures.all[0];
            }  else {
                notify.error('Aucune structure');
            }
        });

        route({
            main: () => {},
            manageAgents: async () => {
                template.open('administrator-main', 'administrator/agent/manage-agents');
                await $scope.agents.sync();
                $scope.$apply();
            },
            manageSuppliers: async () => {
                template.open('administrator-main', 'administrator/supplier/manage-suppliers');
                await $scope.suppliers.sync();
                $scope.$apply();
            },
            manageContracts: async () => {
                await $scope.contracts.sync();
                template.open('administrator-main', 'administrator/contract/manage-contract');
                $scope.agents.sync();
                $scope.suppliers.sync();
                $scope.contractTypes.sync();
                $scope.programs.sync();
                $scope.$apply();
            }
        });

        $scope.redirectTo = (path: string) => {
            $location.path(path);
        };

        $scope.formatDate = (date: string | Date, format: string) => {
            return moment(date).format(format);
        };
    }]);
