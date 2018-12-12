import {moment, ng, template} from 'entcore';
import {Notification, OrderClient, OrdersClient, Project, Projects, Utils} from '../../model';

declare let window: any;

export const orderPersonnelController = ng.controller('orderPersonnelController',
    ['$scope', '$routeParams',  ($scope, $routeParams) => {

        $scope.display = {
            ordersClientOption : [],
            lightbox : {
                deleteOrder : false,
                deleteProject: false,
                udpateProject: false,
            }
        };

        $scope.exportCSV = () => {
            let idCampaign = $scope.ordersClient.all[0].id_campaign;
            let idStructure = $scope.ordersClient.all[0].id_structure;
            window.location = `/lystore/orders/export/${idCampaign}/${idStructure}`;
        };

        $scope.hasAProposalPrice = (orderClient: OrderClient) => {
            
            return (orderClient.price_proposal);
        }

        $scope.displayEquipmentOption = (index: number) => {
            $scope.display.ordersClientOption[index] = !$scope.display.ordersClientOption[index];
            Utils.safeApply($scope);
        };

        $scope.calculateDelivreryDate = (date: Date) => {
            return moment(date).add(60, 'days').calendar();
        };

        $scope.calculateTotal = (orderClient: OrderClient, roundNumber: number) => {
            let totalPrice = $scope.calculatePriceOfEquipment(orderClient, true, roundNumber) * orderClient.amount;
            return totalPrice.toFixed(roundNumber);
        };

        $scope.updateComment= (orderClient: OrderClient) =>{
            if (!orderClient.comment || orderClient.comment.trim() == " ") {
                orderClient.comment="";

            }
            orderClient.updateComment();
        }

        $scope.priceProposal = (orderClient: OrderClient) => {

        }


        $scope.displayLightboxDelete = (orderEquipments: OrdersClient) => {
            template.open('orderClient.delete', 'customer/campaign/order/delete-confirmation');
            $scope.ordersEquipmentToDelete = orderEquipments;
            $scope.display.lightbox.deleteOrder = true;
            Utils.safeApply($scope);
        };
        $scope.cancelOrderEquipmentDelete = () => {
            delete $scope.orderEquipmentToDelete;
            $scope.display.lightbox.deleteOrder = false;
            template.close('orderClient.delete');

            Utils.safeApply($scope);
        };


        $scope.deleteOrdersEquipment = (ordersEquipment: OrdersClient) => {
            for (let i = 0; i < ordersEquipment.length; i++) {
                $scope.deleteOrderEquipment(ordersEquipment[i]);
            }
        }
        $scope.deleteOrderEquipment = async (orderEquipmentToDelete: OrderClient) => {
            let { status, data } = await orderEquipmentToDelete.delete();
            if (status === 200) {
                $scope.campaign.nb_order = data.nb_order;
                $scope.campaign.purse_amount = data.amount;
                ($scope.campaign.purse_enabled) ? $scope.notifications.push(new Notification('lystore.orderEquipment.delete.confirm', 'confirm'))
                    : $scope.notifications.push(new Notification('lystore.requestEquipment.delete.confirm', 'confirm'));



            }
            $scope.cancelOrderEquipmentDelete();
            await $scope.ordersClient.sync(null, [], $routeParams.idCampaign, $scope.current.structure.id );
            Utils.safeApply($scope);
        };

        $scope.projectHasBuilding = (project: Project) => {
            if (project)
                return (project.building);
            else
                return false;
        }

        $scope.projectHasRoom = (project: Project) => {
            if (project)
                return (project.room);
            else
                return false;
        }

        $scope.projectHasStair = (project: Project) => {
            if (project)
                return (project.stair);
            else
                return false;
        }

        $scope.projectHasDescription = (project: Project) => {
            if (project)
                return (project.description);
            else
                return false;
        }

        $scope.projectHasSite = (project: Project) => {
            if (project)
                return (project.site);
            else
                return false;
        }


        $scope.displayLightboxProjectDelete = (project: Project) => {
            template.open('orderClient.deleteProject', 'customer/campaign/order/delete-project-confirmation');
            $scope.projectToDelete = project;
            $scope.display.lightbox.deleteProject = true;
            Utils.safeApply($scope);
        }

        $scope.cancelProjectDelete = () => {
            delete $scope.projectsToDelete;
            $scope.display.lightbox.deleteProject = false;
            template.close('orderClient.deleteProject');
            Utils.safeApply($scope);
        }

        $scope.deleteProject = async (projects: Projects) => {
            for (let i = 0; i < projects.length; i++) {
                if ($scope.projectIsDeletable(projects[i])) {
                    let {status} = await projects[i].delete();
                    if (status == 200) {
                        $scope.notifications.push(new Notification('lystore.project.delete.confirm', 'confirm'));
                    }
                }
            }
            await $scope.ordersClient.sync(null, [], $routeParams.idCampaign, $scope.current.structure.id);
            $scope.display.lightbox.deleteProject = false;
            template.close('orderClient.deleteProject');
            Utils.safeApply($scope);

        }

        $scope.openProjectForm = (project: Project) => {
            $scope.display.lightbox.udpateProject = true;
            $scope.projectToUpdate = project;
            template.open('orderClient.updateProject', 'customer/campaign/order/update-project-confirmation');
        }
        $scope.cancelProjectUpdate = () => {
            delete $scope.projectToUpdate;
            $scope.display.lightbox.udpateProject = false;
            template.close('orderClient.updateProject');
            Utils.safeApply($scope);
        }

        $scope.openProjectsDeletion = (projects: Projects) => {
            $scope.projectsToDelete = projects;
            $scope.display.lightbox.deleteProject = true;
            template.open('orderClient.deleteProject', 'customer/campaign/order/delete-project-confirmation');
        }

        $scope.updateProject = async () => {
            let {status} = await $scope.projectToUpdate.update();
            delete $scope.projectToUpdate;
            $scope.display.lightbox.udpateProject = false;
            template.close('orderClient.updateProject');
            Utils.safeApply($scope);
        }


        $scope.alltheProjectsDeletable = (projects: Projects) => {
            let oneDeletable = true;
            for (let i = 0; i < projects.length; i++) {
                oneDeletable = oneDeletable && $scope.projectIsDeletable(projects[i]);
            }
            return oneDeletable;
        }

        $scope.projectIsDeletable = (project: Project) => {
            let isDeletable = true;
            let orderTemp;
            for (let i = 0; i < $scope.ordersClient.all.length; i++) {
                orderTemp = $scope.ordersClient.all[i];
                if (orderTemp.project.id == project.id && orderTemp.status !== "WAITING") {
                    isDeletable = false;
                }
            }
            return isDeletable;
        }
    }]);
