import {_, idiom as lang, ng, notify, template} from 'entcore';
import {
    Notification, Operation, OrderClient, OrderRegion, OrdersRegion, Structure, StructureGroup, StructureGroups,
    Structures, Title, Titles, Utils
} from "../../model";
import {Equipments} from "../../model/Equipment";
import {Mix} from "entcore-toolkit";


declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', '$location', '$routeParams', ($scope, $location, $routeParams) => {

        $scope.orderToCreate = new OrderRegion();
        $scope.equipments = new Equipments();
        $scope.structure_groups = new StructureGroups();
        $scope.structuresToDisplay = new Structures();
        $scope.titles = new Titles();
        $scope.contract_type = "-";
        $scope.display = {
            lightbox: {
                validOrder: false,
            },
        };
        $scope.translate = (key: string) => lang.translate(key);

        $scope.updateCampaign = async () => {
            $scope.orderToCreate.project = undefined;
            await $scope.titles.syncAdmin($scope.orderToCreate.campaign.id);
            await $scope.structure_groups.syncByCampaign($scope.orderToCreate.campaign.id);
            let structures = new Structures();
            $scope.structure_groups.all.map(structureGR => {
                structureGR.structures.map(structureId => {
                    let newStructure = new Structure();
                    newStructure.id = structureId;
                    newStructure = $scope.structures.all.find(s => s.id === newStructure.id);
                    if (structures.all.indexOf(newStructure) === -1) // no duplicate data
                        structures.push(newStructure);
                })
            });

            $scope.structuresToDisplay = structures;

            $scope.structuresToDisplay.all.sort((s, ss) => {
                if (s.name < ss.name) return 1;
                if (s.name > ss.name) return -1;
                return 0;
            });

            $scope.orderToCreate.rows = [];
            Utils.safeApply($scope);
        };

        $scope.initDataUpdate = async () => {
            await $scope.equipments.sync($scope.orderToUpdate.id_campaign, $scope.orderToUpdate.id_structure);
            $scope.orderToUpdate.equipment = $scope.equipments.all.find((e) => {
                return e.id === $scope.orderToUpdate.equipment_key;
            });
            $scope.getContractType();
        };


        if ($routeParams.idOrder) {
            let idOrder = $routeParams.idOrder;
            $scope.orderToUpdate.structure = $scope.structures.filter(structureFilter => structureFilter.id === $scope.orderToUpdate.id_structure)[0];
            $scope.orderToUpdate.price_proposal = $scope.orderToUpdate.price_single_ttc;
            if(  $scope.orderToUpdate.campaign.orderPriorityEnable()){
                $scope.orderToUpdate.rank = $scope.orderToUpdate.rank === null? null : $scope.orderToUpdate.rank + 1;
            }
            if (!$scope.orderToUpdate.project.room)
                $scope.orderToUpdate.project.room = '-';
            if (!$scope.orderToUpdate.project.building)
                $scope.orderToUpdate.project.building = '-';

            $scope.initDataUpdate();
        }
        $scope.isUpdating = $location.$$path.includes('/order/update');
        $scope.isUpdatingFromOrder = $location.$$path.includes('/order/operation/update');

        $scope.getTotal = () => {
            return ($scope.orderToUpdate.amount * $scope.orderToUpdate.price_proposal).toFixed(2);
        };


        $scope.operationSelected = async (operation: Operation) => {
            $scope.isOperationSelected = true;
            $scope.operation = operation;
            if ($scope.isUpdating) {

                let orderRegion = new OrderRegion();
                orderRegion.createFromOrderClient($scope.orderToUpdate);

                orderRegion.id_operation = operation.id;
                orderRegion.equipment_key = $scope.orderToUpdate.equipment_key;
                orderRegion.technical_spec = $scope.orderToUpdate.equipment.technical_specs;
                let {status, data} = await orderRegion.set();
                if (status === 200) {
                    $scope.notifications.push(new Notification('lystore.order.region.update', 'confirm'));
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
        $scope.selectOperationForOrder = async () => {
            await $scope.initOperation();
            $scope.isOperationsIsEmpty = !$scope.operations.all.some(operation => operation.status === 'true');
            template.open('validOrder.lightbox', 'administrator/order/order-select-operation');
            $scope.display.lightbox.validOrder = true;
        };

        $scope.cancelUpdate = () => {
            if ($scope.isUpdating)
                $scope.redirectTo('/order/waiting');
            if ($scope.isUpdatingFromOrder)
                $scope.redirectTo('/operation');
        };
        $scope.updateOrderConfirm = async () => {
            await $scope.selectOperationForOrder();
        };

        $scope.updateLinkedOrderConfirm = async () => {
            let orderRegion = new OrderRegion();
            orderRegion.createFromOrderClient($scope.orderToUpdate);
            orderRegion.equipment_key = $scope.orderToUpdate.equipment_key;
            $scope.redirectTo('/operation');
            if($scope.orderToUpdate.isOrderRegion){
                await orderRegion.update($scope.orderToUpdate.id);
            } else {
                await orderRegion.set();
            }
            $scope.notifications.push(new Notification('lystore.order.region.update', 'confirm'));
        };
        $scope.isValidFormUpdate = () => {
            return $scope.orderToUpdate.equipment_key
                &&  $scope.orderToUpdate.equipment
                && $scope.orderToUpdate.price_proposal
                && $scope.orderToUpdate.amount
                && (($scope.orderToUpdate.campaign.orderPriorityEnable() &&
                    ($scope.orderToUpdate.rank>0 &&
                        $scope.orderToUpdate.rank<11  ||
                        $scope.orderToUpdate.rank === null)) ||
                    !$scope.orderToUpdate.campaign.orderPriorityEnable())
        };

        function checkRow(row) {
            return row.equipment && row.price && row.structure && row.amount
        }

        $scope.oneRow = () => {
            let oneValidRow = false;
            if ($scope.orderToCreate.rows)
                $scope.orderToCreate.rows.map(r => {
                    if (checkRow(r))
                        oneValidRow = true;
                });
            return oneValidRow;
        };

        $scope.validForm = () => {
            return $scope.orderToCreate.campaign
                && $scope.orderToCreate.project
                && $scope.orderToCreate.operation
                && $scope.oneRow()
                ;
        };
        $scope.getContractType = () => {
            let contract;
            if($scope.orderToUpdate.equipment){
                $scope.contracts.all.map(c => {
                    if (c.id === $scope.orderToUpdate.equipment.id_contract)
                        contract = c
                });
                $scope.contractTypes.all.map(c => {
                    if (c.id === contract.id_contract_type) {
                        $scope.contract_type = c.displayName
                    }
                });
            }
            Utils.safeApply($scope);
        };

        $scope.addRow = () => {
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

        $scope.dropRow = (index) => {
            $scope.orderToCreate.rows.splice(index, 1);
        };

        $scope.duplicateRow = (index) => {
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
        $scope.cancelBasketDelete = () => {
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
        };

        $scope.switchStructure = async (row, structure) => {
            await row.equipments.syncAll($scope.orderToCreate.campaign.id, (structure) ? structure.id : undefined);
            row.equipment = undefined;
            Utils.safeApply($scope);

        };
        $scope.initEquipmentData = (row) => {
            row.price = row.equipment.priceTTC;
            row.amount = 1;

        };
        $scope.swapTypeStruct = (row) => {
            row.display.struct = !row.display.struct;
            Utils.safeApply($scope);
        };

        $scope.createOrder = async () => {
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
                            if (row.rank)
                                orderRegionTemp.rank = row.rank;
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
                        if (row.rank)
                            orderRegionTemp.rank = row.rank;
                        ordersToCreate.all.push(orderRegionTemp);
                    }
                }
            });
            let {status, data} = await ordersToCreate.create();
            if (status === 200) {
                $scope.notifications.push(new Notification('lystore.order.region.create.message', 'confirm'));
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