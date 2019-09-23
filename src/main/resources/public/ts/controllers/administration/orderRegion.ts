import {_, idiom as lang, ng, notify, template, toasts} from 'entcore';
import {
    Notification,
    Operation,
    OrderRegion,
    OrdersRegion,
    Structure,
    StructureGroup,
    StructureGroups,
    Structures,
    Titles,
    Utils,
    Equipments
} from "../../model";

declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', '$location', '$routeParams', ($scope, $location, $routeParams) => {

        $scope.orderToCreate = new OrderRegion();
        $scope.structure_groups = new StructureGroups();
        $scope.structuresToDisplay = new Structures();
        $scope.titles = new Titles();
        $scope.display = {
            lightbox: {
                validOrder: false,
            },
        };
        $scope.translate = (key: string):string => lang.translate(key);

        $scope.updateCampaign = async ():Promise<void> => {
            $scope.orderToCreate.project = undefined;
            await $scope.titles.syncAdmin($scope.orderToCreate.campaign.id);
            await $scope.structure_groups.syncByCampaign($scope.orderToCreate.campaign.id);
            let structures = new Structures();
            $scope.structure_groups.all.map(structureGR => {
                structureGR.structures.map(structureId => {
                    let newStructure = new Structure();
                    newStructure.id = structureId;
                    newStructure = $scope.structures.all.find(s => s.id === newStructure.id);
                    if (structures.all.indexOf(newStructure) === -1)
                        structures.push(newStructure);
                })
            });
            $scope.structuresToDisplay = structures;
            $scope.structuresToDisplay.all.sort((firstStructure, secondStructure) => {
                if (firstStructure.name < secondStructure.name) return 1;
                if (firstStructure.name > secondStructure.name) return -1;
                return 0;
            });
            Utils.safeApply($scope);
        };

        $scope.operationSelected = async (operation: Operation):Promise<void> => {
            $scope.isOperationSelected = true;
            $scope.operation = operation;
            if (!$scope.orderToUpdate.id_operation) {
                let orderRegionCreate = new OrderRegion();
                orderRegionCreate.createFromOrderClient($scope.orderToUpdate);
                orderRegionCreate.id_operation = operation.id;
                orderRegionCreate.equipment_key = $scope.orderToUpdate.equipment_key;
                orderRegionCreate.technical_spec = $scope.orderToUpdate.equipment.technical_specs;
                const { status } = await orderRegionCreate.create();
                if (status === 200) {
                    toasts.confirm('lystore.order.region.update');
                    await $scope.ordersClient.addOperationInProgress(operation.id, [$routeParams.idOrder]);
                    $scope.cancelUpdate();
                }
                else {
                    notify.error('lystore.admin.order.update.err');
                }
                Utils.safeApply($scope);

            }
        };

        $scope.isOperationsIsEmpty = false;
        $scope.selectOperationForOrder = async ():Promise<void> => {
            await $scope.initOperation();
            $scope.isOperationsIsEmpty = !$scope.operations.all.some(operation => operation.status === 'true');
            template.open('validOrder.lightbox', 'administrator/order/order-select-operation');
            $scope.display.lightbox.validOrder = true;
        };

        $scope.cancelUpdate = ():void => {
            template.open('administrator-main', 'administrator/order/order-waiting');
        };
        $scope.updateOrderConfirm = async ():Promise<void> => {
            await $scope.selectOperationForOrder();
        };

        $scope.updateLinkedOrderConfirm = async ():Promise<void> => {
            let orderRegion = new OrderRegion();
            orderRegion.createFromOrderClient($scope.orderToUpdate);
            orderRegion.equipment_key = $scope.orderToUpdate.equipment_key;
            $scope.cancelUpdate();
            if($scope.orderToUpdate.typeOrder === "region"){
                await orderRegion.update($scope.orderToUpdate.id);
            } else {
                await orderRegion.create();
            }
            toasts.confirm('lystore.order.region.update');
        };
        $scope.isValidFormUpdate = ():boolean => {
            return $scope.orderToUpdate.equipment_key
                &&  $scope.orderToUpdate.equipment
                && $scope.orderToUpdate.price_single_ttc
                && $scope.orderToUpdate.amount
                && ((($scope.orderToUpdate.rank>0 &&
                    $scope.orderToUpdate.rank<11  ||
                    $scope.orderToUpdate.rank === null)) ||
                    !$scope.orderToUpdate.campaign.orderPriorityEnable())
        };

        function checkRow(row):boolean {
            return row.equipment && row.price && row.structure && row.amount
        }

        $scope.oneRow = ():boolean => {
            let oneValidRow = false;
            if ($scope.orderToCreate.rows)
                $scope.orderToCreate.rows.map(row => {
                    if (checkRow(row))
                        oneValidRow = true;
                });
            return oneValidRow;
        };

        $scope.validForm = ():boolean => {
            return $scope.orderToCreate.campaign
                && $scope.orderToCreate.project
                && $scope.orderToCreate.operation
                && $scope.oneRow()
                && ($scope.orderToCreate.rows.every( row => (row.rank>0 &&
                    row.rank<11  ||
                    row.rank === null))
                    || !$scope.orderToCreate.campaign.orderPriorityEnable());
        };

        $scope.addRow = ():void => {
            let row = {
                equipment: undefined,
                equipments: new Equipments(),
                structure: undefined,
                price: undefined,
                amount: undefined,
                comment: "",
                display: {
                    struct: false
                }
            };
            if (!$scope.orderToCreate.rows)
                $scope.orderToCreate.rows = [];
            $scope.orderToCreate.rows.push(row);
            Utils.safeApply($scope)

        };

        $scope.dropRow = (index:number):void => {
            $scope.orderToCreate.rows.splice(index, 1);
        };

        $scope.duplicateRow = (index:number):void => {
            let row = JSON.parse(JSON.stringify($scope.orderToCreate.rows[index]));
            row.equipments = new Equipments();

            if (row.structure.structures) {
                row.structure = $scope.structure_groups.all.find(struct => row.structure.id === struct.id);
            } else {
                row.structure = $scope.structures.all.find(struct => row.structure.id === struct.id);
            }
            $scope.orderToCreate.rows[index].equipments.forEach(equipment => {
                row.equipments.push(equipment);
                if (row.equipment.id === equipment.id)
                    row.equipment = equipment;
            });
            $scope.orderToCreate.rows.splice(index + 1, 0, row)
        };
        $scope.cancelBasketDelete = ():void => {
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
        };

        $scope.switchStructure = async (row:any, structure:Structure):Promise<void> => {
            await row.equipments.syncAll($scope.orderToCreate.campaign.id, (structure) ? structure.id : undefined);
            row.equipment = undefined;
            Utils.safeApply($scope);
        };
        $scope.initEquipmentData = (row:OrderRegion):void => {
            row.price = row.equipment.priceTTC;
            row.amount = 1;
        };
        $scope.swapTypeStruct = (row):void => {
            row.display.struct = !row.display.struct;
            Utils.safeApply($scope);
        };

        $scope.createOrder = async ():Promise<void> => {
            let ordersToCreate = new OrdersRegion();
            $scope.orderToCreate.rows.map(row => {
                if (checkRow(row)) {
                    if (row.structure instanceof StructureGroup) {
                        row.structure.structures.map(s => {
                            let orderRegionTemp = new OrderRegion();
                            orderRegionTemp.id_campaign = $scope.orderToCreate.campaign.id;
                            orderRegionTemp.id_structure = s;
                            orderRegionTemp.title_id = $scope.orderToCreate.project;
                            orderRegionTemp.id_operation = $scope.orderToCreate.operation;
                            orderRegionTemp.equipment_key = row.equipment.id;
                            orderRegionTemp.equipment = row.equipment;
                            orderRegionTemp.comment = row.comment;
                            orderRegionTemp.amount = row.amount;
                            orderRegionTemp.price = row.price;
                            orderRegionTemp.name = row.equipment.name;
                            orderRegionTemp.technical_spec = row.equipment.technical_specs;
                            orderRegionTemp.id_contract = row.equipment.id_contract;
                            if (!row.rank){
                                orderRegionTemp.rank = 0;
                            } else {
                                orderRegionTemp.rank = row.rank;
                            }
                            let struct = $scope.structures.all.find(struct => s.id === struct.id);
                            (struct) ? orderRegionTemp.name_structure = struct.name : orderRegionTemp.name_structure = "";
                            ordersToCreate.all.push(orderRegionTemp);
                        })
                    } else {
                        let orderRegionTemp = new OrderRegion();
                        orderRegionTemp.id_campaign = $scope.orderToCreate.campaign.id;
                        orderRegionTemp.id_structure = row.structure.id;
                        orderRegionTemp.title_id = $scope.orderToCreate.project;
                        orderRegionTemp.equipment = row.equipment;
                        orderRegionTemp.equipment_key = row.equipment.id;
                        orderRegionTemp.id_operation = $scope.orderToCreate.operation;
                        orderRegionTemp.comment = row.comment;
                        orderRegionTemp.amount = row.amount;
                        orderRegionTemp.price = row.price;
                        orderRegionTemp.name = row.equipment.name;
                        orderRegionTemp.technical_spec = row.equipment.technical_specs;
                        orderRegionTemp.id_contract = row.equipment.id_contract;
                        orderRegionTemp.name_structure = row.structure.name;
                        if (!row.rank){
                            orderRegionTemp.rank = 0;
                        } else {
                            orderRegionTemp.rank = row.rank;
                        }
                        ordersToCreate.all.push(orderRegionTemp);
                    }
                }
            });
            let {status} = await ordersToCreate.create();
            if (status === 201) {
                toasts.confirm('lystore.order.region.create.message');
                $scope.orderToCreate = new OrderRegion();
                $scope.titles = new Titles();
            }
            else {
                notify.error('lystore.admin.order.create.err');
            }
            Utils.safeApply($scope);
        }
    }
    ]);