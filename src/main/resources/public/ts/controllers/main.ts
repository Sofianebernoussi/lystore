import { ng, template, notify, idiom as lang } from 'entcore';
import { Structures, Agents } from '../model';

export const mainController = ng.controller('MainController', ['$scope', 'route', '$location',
    ($scope, route, $location) => {
        template.open('main', 'administrator/main');
        $scope.lang = lang;
        $scope.agents = new Agents();
        $scope.agents.sync();
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
            manageAgents: () => {
                template.open('administrator-main', 'administrator/manage-agents');
            }
        });

        $scope.redirectTo = (path: string) => {
            $location.path(path);
        };
    }]);
