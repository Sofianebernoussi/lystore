import { ng, template, notify, moment, _ } from 'entcore';
import { PurseImporter, Utils } from '../../model';

declare let window: any;

export const purseController = ng.controller('PurseController',
    ['$scope', ($scope) => {
        $scope.lightbox = {
            open: false
        };

        $scope.openPurseImporter = (): void => {
            $scope.importer = new PurseImporter($scope.campaign.id);
            $scope.lightbox.open = true;
        };

        $scope.importPurses = async (importer: PurseImporter): Promise<void> => {
            try {
                await importer.validate();
            } catch (err) {
                importer.message = err.message;
            } finally {
                if (!importer.message) {
                    $scope.lightbox.open = false;
                    delete $scope.importer;
                } else {
                    importer.files = [];
                }
                Utils.safeApply($scope);
            }
        };

        $scope.exportPurses = (id: number) => {
            window.location = `/lystore/campaign/${id}/purses/export`;
        };
    }]);