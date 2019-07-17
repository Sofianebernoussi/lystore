import {idiom as lang, ng, notify, template} from 'entcore';
import {
    ContractTypes, Notification, Operation, OrderClient, OrderRegion, OrdersRegion, Structure, StructureGroups,
    Structures, Titles, Utils
} from "../../model";
import {Equipments} from "../../model/Equipment";


declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', '$location', '$routeParams', ($scope, $location, $routeParams) => {
        $scope.orderToUpdate = new OrderClient();
        $scope.orderToCreate = new OrderRegion();
        $scope.equipments = new Equipments();
        $scope.contractTypes = new ContractTypes();
        $scope.structure_groups = new StructureGroups();
        $scope.structuresToDisplay = new Structures();
        $scope.titles = new Titles();
        $scope.display = {
            lightbox: {
                validOrder: false,
            },
            struct: false
        };
        $scope.translate = (key: string) => lang.translate(key);

        $scope.updateCampaign = async () => {
            $scope.orderToCreate.project = undefined;
            await $scope.titles.syncAdmin($scope.orderToCreate.campaign);
            await $scope.structure_groups.syncByCampaign($scope.orderToCreate.campaign);
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
            $scope.ordersClient.all.forEach((o) => {
                if (o.id == idOrder)
                    $scope.orderToUpdate = o;
            });
            ($scope.orderToUpdate.price_proposal)
                ? $scope.orderToUpdate.price_proposal = parseFloat($scope.orderToUpdate.price_proposal)
                : $scope.orderToUpdate.price_proposal = $scope.orderToUpdate.priceTTCtotal;
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
                let {status, data} = await orderRegion.set();
                if (status === 200) {
                    $scope.notifications.push(new Notification('lystore.order.region.update', 'confirm'));
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
            await orderRegion.set();
            $scope.notifications.push(new Notification('lystore.order.region.update', 'confirm'));
        };
        $scope.isValidFormUpdate = () => {
            return $scope.orderToUpdate.equipment_key
                && $scope.orderToUpdate.price_proposal
                && $scope.orderToUpdate.amount
                && (($scope.orderToUpdate.campaign.orderPriorityEnable() && $scope.orderToUpdate.rank) || !$scope.orderToUpdate.campaign.orderPriorityEnable())
        };

        $scope.oneRow = () => {
            let oneValidRow = false;
            if ($scope.orderToCreate.rows)
                $scope.orderToCreate.rows.map(r => {
                    if (r.equipment && r.price && r.structure && r.amount)
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
            $scope.contracts.all.map(c => {
                if (c.id === $scope.orderToUpdate.equipment.id_contract)
                    contract = c
            });
            $scope.contractTypes.all.map(c => {
                if (c.id === contract.id_contract_type) {
                    $scope.contract_type = c.displayName
                }
            });
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
            await row.equipments.syncAll($scope.orderToCreate.campaign, structure);
            Utils.safeApply($scope);

        };
        $scope.initEquipmentData = (row) => {
            row.price = row.equipment.priceTTC;
            row.amount = 1;

        }
        $scope.swapTypeStruct = (row) => {
            $scope.display.struct = !$scope.display.struct;
            console.log($scope.structures);
        }
        $scope.createOrder = () => {
            let ordersToCreate = new OrdersRegion()
        
            $scope.orderToCreate.create();
        }
    }
    ]);