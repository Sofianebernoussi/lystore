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
                $scope.instruction.operations = new Operations();
            } else if (action === 'edit'){
                $scope.instruction = $scope.instructions.selected[0];
            }
            template.open('instruction-main', 'administrator/instruction/instruction-form');
            Utils.safeApply($scope);
        };
        $scope.isNewOperation = false;
        $scope.addOperationLigne = () => {
            if ( $scope.operations.all.length !== 0) {
                $scope.isNewOperation = true;
            }
            Utils.safeApply($scope);
        };
        $scope.operationSelected = [];
        $scope.operationIsSelect = () => {
            $scope.operation = new Operation();
            $scope.operationAdd = JSON.parse($scope.instruction.operation);
            $scope.operation.id = $scope.operationAdd.id;
            $scope.operation.label = $scope.operationAdd.label;
            $scope.operation.id_label = $scope.operationAdd.id_label;
            $scope.operation.amount = $scope.operationAdd.amount;
            $scope.operation.status = true;
            $scope.operations.all = $scope.operations.all.filter( operation => operation.id !== $scope.operation.id);
            $scope.instruction.operations.all.push($scope.operation);
            $scope.isNewOperation = false;
        };
        $scope.dropOperation = (indexSelect: Number, operation: Operation) => {
            $scope.instruction.operations.all = $scope.instruction.operations.all
                .filter((operation, index) => index !== indexSelect);
            $scope.operations.all.push(operation);
            Utils.safeApply($scope);
        };
        $scope.cancelFormAddOperation = () => {
            $scope.isNewOperation = false;
        };
        $scope.cancelInstructionForm = () =>{
            template.open('instruction-main', 'administrator/instruction/manage-instruction');
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
            if($scope.instructions.selected.some(instruction => instruction.operations.length !== 0)){
                template.open('instruction.lightbox', 'administrator/instruction/instruction-delete-reject-lightbox');
            } else {
                await $scope.instructions.delete();
                await $scope.initInstructions();
                template.close('instruction.lightbox');
                $scope.display.lightbox.instruction = false;
                Utils.safeApply($scope);
            }
        };
        $scope.sendInstruction = async () => {
            await $scope.instruction.save();
            if($scope.instruction.operations.length !== 0){
                let operationIds = $scope.instruction.operations.all.map( operation => operation.id );
                if($scope.instruction.id){
                    await $scope.operations.updateOperations($scope.instruction.id, operationIds);
                }
            }
            await $scope.initInstructions();
            $scope.cancelInstructionForm();
            Utils.safeApply($scope);
        };
    }]);