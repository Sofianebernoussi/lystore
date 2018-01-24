import { ng, template, _ } from 'entcore';
import {
    Campaign,
    Utils
} from '../../model';


export const campaignsListController = ng.controller('campaignsListController',
    ['$scope', ($scope) => {

        $scope.openCampaign = (campaign: Campaign) => {
            $scope.redirectTo(`/campaign/${campaign.id}/catalog`);
            Utils.safeApply($scope);
        };

    }]);