import { ng, routes } from 'entcore';
import * as controllers from './controllers';
import * as directives from './directives';

for(let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

for (let directive in directives) {
    ng.directives.push(directives[directive]);
}

routes.define(($routeProvider) => {
    $routeProvider
        .when('/', {
            action: 'main'
        })
        .when('/agents', {
            action: 'manageAgents'
        })
        .when('/suppliers', {
            action: 'manageSuppliers'
        })
        .otherwise({
            redirectTo: '/'
        });
});