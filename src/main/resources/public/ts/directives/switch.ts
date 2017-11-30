import { ng } from 'entcore';

export const Switch = ng.directive('switch', () => {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            disabled: '@'
        },
        template: '<label class="switch">' +
        '<input type="checkbox" ng-model="ngModel" ng-disabled="disabled"/>' +
        '<span class="tick"></span>' +
        '</label>'
    }
});