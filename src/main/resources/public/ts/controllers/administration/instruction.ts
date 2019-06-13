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
        $scope.formatDate = (date) => {
            return Utils.formatDate(date)
        };
        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            Utils.safeApply($scope);
        };

        $scope.isOperationEdit = false;
        $scope.openInstructionForm = async (action: string) => {
            await $scope.initOperation();
            $scope.operationEditRemoveInstructionIds = [];
            if(action === 'create'){
                $scope.instruction = new Instruction();
                $scope.instruction.operations = [];
            } else if (action === 'edit'){
                $scope.instruction = $scope.instructions.selected[0];
                $scope.isOperationEdit = true;
            }
            template.open('instruction-main', 'administrator/instruction/instruction-form');
            Utils.safeApply($scope);
        };
        $scope.isNewOperation = false;
        $scope.addOperationLigne = () => {
            if($scope.isOperationEdit)
            if ( $scope.operations.all.length !== 0) {
                $scope.isNewOperation = true;
            }
            Utils.safeApply($scope);
        };
        $scope.operationIsSelect = () => {
            $scope.operation = new Operation();
            $scope.operationAdd = JSON.parse($scope.instruction.operation);
            $scope.operation.id = $scope.operationAdd.id;
            $scope.operation.label = $scope.operationAdd.label;
            $scope.operation.id_label = $scope.operationAdd.id_label;
            $scope.operation.amount = $scope.operationAdd.amount;
            $scope.operation.status = true;
            $scope.operations.all = $scope.operations.all.filter( operation => operation.id !== $scope.operation.id);
            $scope.instruction.operations.push($scope.operation);
            $scope.isNewOperation = false;
            if($scope.isOperationEdit) {
                $scope.operationEditRemoveInstructionIds = $scope.operationEditRemoveInstructionIds
                    .filter( id => id !== $scope.operation.id);
            }
        };
        $scope.dropOperation = (indexSelect: Number, operation: Operation) => {
            if($scope.isOperationEdit) $scope.operationEditRemoveInstructionIds.push(operation.id);
            $scope.instruction.operations = $scope.instruction.operations
                .filter((operation, index) => index !== indexSelect);
            $scope.operations.all.push(operation);
            Utils.safeApply($scope);
        };
        $scope.cancelFormAddOperation = () => {
            $scope.isNewOperation = false;
        };
        $scope.cancelInstructionForm = () =>{
            $scope.cancelFormAddOperation();
            $scope.isOperationEdit = false;
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
                let operationIds = $scope.instruction.operations.map( operation => operation.id );
                if($scope.instruction.id){
                    await $scope.operations.updateOperations($scope.instruction.id, operationIds);
                }
            }
            if($scope.operationEditRemoveInstructionIds.length !== 0){
                await $scope.operations.updateRemoveOperations($scope.operationEditRemoveInstructionIds);
            }
            $scope.isOperationEdit = false;
            await $scope.initInstructions();
            $scope.cancelInstructionForm();
            Utils.safeApply($scope);
        };
    }]);