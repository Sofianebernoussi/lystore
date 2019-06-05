import { ng, template, notify, moment, _ } from 'entcore';
import {ExerciseNumbers, Instruction, labels, Operation, Utils} from "../../model";


declare let window: any;

export const instructionController = ng.controller('instructionController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {

    $scope.exerciseNumbers = new ExerciseNumbers();

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
            $scope.labelOperation = new labels();
            $scope.labelOperation.sync();
            template.open('instruction-main', 'administrator/instruction/instruction-form');
            Utils.safeApply($scope);
        };
        $scope.addOperationLigne = () => {
            $scope.operation = new Operation();
            $scope.operation.status = true;
            $scope.instruction.operations.push($scope.operation);
            Utils.safeApply($scope);
        };
        $scope.dropOperation = (indexSelect: Number) => {
            $scope.instruction.operations = $scope.instruction.operations
                .filter((operation, index) => index !== indexSelect);
            Utils.safeApply($scope);
        };
        $scope.cancelInstructionForm = () =>{
            template.open('instruction-main', 'administrator/instruction/manage-instruction');
        };
    }]);