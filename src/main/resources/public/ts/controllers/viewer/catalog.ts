/**
 * Created by rahnir on 22/01/2018.
 */
/**
 * Created by rahnir on 18/01/2018.
 */
import { ng, template, _ } from 'entcore';
import {
    Utils
} from '../../model';


export const catalogController = ng.controller('catalogController',
    ['$scope', ($scope) => {
        $scope.search = {
            filterWrod: '',
            filterWrods: []
        };
        $scope.addFilter = (filterWrod: string, event?) => {
            if (event && (event.which === 13 || event.keyCode === 13 )) {
                $scope.addfilterWords(filterWrod);
            } else if (!event) {
                $scope.addfilterWords(filterWrod);
            }
        };
        $scope.addfilterWords = (filterWrod) => {
            if (filterWrod !== '') {
                $scope.search.filterWrods = _.union($scope.search.filterWrods, [filterWrod]);
                $scope.search.filterWrod = '';
                Utils.safeApply($scope);
            }
        };
        $scope.pullFilterWord = (filterWord) => {
            $scope.search.filterWrods = _.without( $scope.search.filterWrods , filterWord);
        };
    }]);