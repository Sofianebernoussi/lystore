import { ng, template, notify, moment, _ } from 'entcore';
import {Instruction, Utils} from "../../model";


declare let window: any;

export const instructionController = ng.controller('instructionController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {

        $scope.sort = {
            instruction : {
                type: 'name',
                reverse: false
            }
        };
        $scope.search = {
            filterWord : '',
            filterWords : []
        };
        $scope.display = {
            lightbox : {
            }
        };
        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };
        $scope.addEquipmentFilter = (event?) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.trim() !== '') {
                event.target.value = '';
            }
        };
        $scope.openInstructionForm = () => {
            $scope.instruction = new Instruction();
            template.open('instruction-main', 'administrator/instruction/instruction-form');
            Utils.safeApply($scope);
        };

    }]);