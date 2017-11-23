import { ng, template } from 'entcore';

export const mainController = ng.controller('MainController', ['$scope', 'route',($scope, route) => {
    route({
       main: () => {
           template.open('main', 'main');
       }
    });
}]);
