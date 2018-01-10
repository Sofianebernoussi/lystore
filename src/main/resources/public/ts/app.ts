import { ng, routes, model, Behaviours } from 'entcore';
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
        });
    if(model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.administrator)){
        $routeProvider.when('/campaigns/create', {
            action: 'createCampaigns'
        })
            .when('/campaigns/update', {
                action: 'updateCampaigns'
            })
            .when('/structuregroups', {
                action: ''
            })
            .when('/agents', {
                action: 'manageAgents'
            })
            .when('/suppliers', {
                action: 'manageSuppliers'
            })
            .when('/contracts', {
                action: 'manageContracts'
            })
            .when('/tags', {
                action: 'manageEquipmentTags'
            })
            .when('/equipments', {
                action: 'manageEquipments'
            })
            .when('/equipments/create', {
                action: 'createEquipment'
            })
            .when('/logs', {
                action: 'viewLogs'
            });
    }
    if(model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.manager)){
        $routeProvider.when('/campaigns', {

            action: 'manageCampaigns'
        })
    }



    $routeProvider.otherwise({
            redirectTo: '/'
        });
});