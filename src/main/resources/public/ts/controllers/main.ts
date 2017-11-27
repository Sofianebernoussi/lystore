import { ng, template, notify, idiom as lang } from 'entcore';
import { Structures, Agents, Suppliers } from '../model';

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location',
    ($scope, route, $location) => {
        template.open('main', 'administrator/main');
        $scope.lang = lang;
        $scope.agents = new Agents();
        $scope.suppliers = new Suppliers();
        $scope.structures = new Structures();
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
            }
        });

        $scope.redirectTo = (path: string) => {
            $location.path(path);
        };
    }]);
