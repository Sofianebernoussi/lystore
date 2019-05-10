import {ng, idiom as lang, $} from "entcore";

export const searchAndSelect = ng.directive('searchAndSelect', function() {
    return {
        restrict: 'E',
        replace: false,
        scope: {
            options: '=',
            ngModel: '=',
            searchOn: '@',
            orderBy: '@',
            disable: '&',
            ngChange: '&'
        },
        templateUrl: '/lystore/public/template/directives/search-and-select/main.html',
        controller: ['$scope', '$filter', '$timeout', function($scope, $filter, $timeout) {
            /* Search input */
            $scope.search = {
                input: '',
                reset: function(){ this.input = "" }
            };
            $scope.lang = lang;
            /* Combo box visibility */
            $scope.show = false;
            $scope.toggleVisibility = function() {
                $scope.show = !$scope.show;
                if ($scope.show) {
                    $scope.addClickEvent();
                    $scope.search.reset();
                    $timeout(function() {
                        $scope.setComboPosition()
                    }, 1)
                }
            };
            $scope.toggleItem = function(item) {
               $scope.ngModel = item;
                $scope.show = false;
            };
            $scope.fsearch = (item) => {
              if ($scope.search.input){
                  return (item.name.toLowerCase()).includes($scope.search.input.toLowerCase())
                      || (item.UAI.toLowerCase()).includes($scope.search.input.toLowerCase())
              }else
                  return true
            };

            /* Item display */
            $scope.display = function(item) {
                return item instanceof Object ? item.UAI + " - " + item.name : item
            };
            $scope.$watch(()=> $scope.ngModel, (newVal, oldVal)=>{
                if(newVal!=oldVal){
                    $scope.ngChange();
                }
            })
        }],
        link: function(scope, element, attributes) {
            if (!attributes.options ) {
                throw '[<search-and-select> directive] Error: combo-model & filtered-model attributes are required.'
            }

            /* Visibility mouse click event */
            scope.addClickEvent = function() {
                if (!scope.show)
                    return;

                let timeId = new Date().getTime();
                $('body').on('click.multi-combo' + timeId, function(e) {
                    if (!(element.find(e.originalEvent.target).length)) {
                        scope.show = false;
                        $('body').off('click.multi-combo' + timeId);
                        scope.$apply()
                    }
                })
            };

            /* Drop down position */
            scope.setComboPosition = function() {
                element.css('position', 'relative');
                element.find('.search-and-select-panel').css('top',
                    element.find('.search-and-select-button').outerHeight()
                )
            };
            scope.setComboPosition()
        }
    }
});