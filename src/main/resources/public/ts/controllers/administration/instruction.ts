import { ng, template, notify, moment, _ } from 'entcore';
import {Exercises, Instruction, labels, Operation ,Operations , Utils} from "../../model";


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
                instruction:false,
            }
        };

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.openInstructionForm = async (action: string) => {
            await $scope.initOperation();
            if(action === 'create'){
                $scope.instruction = new Instruction();
                $scope.operation = new Operation();
            } else if (action === 'edit'){
                $scope.instruction = $scope.instruction.selected[0];
            }
            template.open('instruction-main', 'administrator/instruction/instruction-form');
            Utils.safeApply($scope);
        };
        $scope.addOperationLigne = () => {
            if ( $scope.operations.all.length !== 0) {
                $scope.operation = $scope.instruction.operations[0];
                $scope.operationsPush = $scope.operations.all;
                $scope.operationsPush.map(ope => ope.status = true);
                $scope.instruction.operations.push($scope.operationsPush);
            } else {

            }
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
        $scope.formatDate = (date:Date) => {
            return Utils.formatDate(date)
        };
        $scope.isAllInstructionSelected = false;
        $scope.switchAllInstruction = () => {
            $scope.isAllInstructionSelected  =  !$scope.isAllInstructionSelected;
            if ( $scope.isAllInstructionSelected) {
                $scope.instructions.all.map(instructionSelected => instructionSelected.selected = true)
            } else {
                $scope.instructions.all.map(instructionSelected => instructionSelected.selected = false)
            }
            Utils.safeApply($scope);
        };
        $scope.addInstructionFilter = async (event?) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.trim() !== '') {
                if(!_.contains($scope.instructions.filters, event.target.value)){
                    $scope.instructions.filters = [...$scope.instructions.filters, event.target.value];
                }
                event.target.value = '';
                await $scope.initInstructions();
                Utils.safeApply($scope);
            }
        };
        $scope.dropInstructionFilter = async (filter: string) => {
            $scope.instructions.filters = $scope.instructions.filters.filter( filterWord => filterWord !== filter);
            await $scope.initInstructions();
            Utils.safeApply($scope);
        };
        $scope.cancelInstructionLightbox = () =>{
            $scope.display.lightbox.instruction = false;
            template.close('instruction.lightbox');
        };
        $scope.openLightboxDeleteInstruction = () => {
            $scope.display.lightbox.instruction = true;
            template.open('instruction.lightbox', 'administrator/instruction/instruction-delete-lightbox');
            Utils.safeApply($scope);
        };
        $scope.deleteInstructions = async () => {
            if($scope.instructions.selected.some(instruction => instruction.operations[0] !== null )){
                template.open('instruction.lightbox', 'administrator/instruction/instruction-delete-reject-lightbox');
            } else {
                await $scope.instructions.delete();
                await $scope.initInstructions();
                template.close('instruction.lightbox');
                $scope.display.lightbox.instruction = false;
                Utils.safeApply($scope);
            }
        };
        $scope.validInstruction = async (instruction:Instruction) => {
            await instruction.save();
            await $scope.initInstructions();
            $scope.cancelInstructionForm();
            Utils.safeApply($scope);
        };
    }]);