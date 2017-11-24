import { ng } from 'entcore';
import { Agent } from '../model';

export const administratorController = ng.controller('administratorController',
    ['$scope', ($scope) => {
        $scope.display = {
            lightbox : {
                agent: false
            }
        };

        $scope.sort = {
            agent: {
                type: 'name',
                reverse: false
            }
        };

        $scope.openAgentForm = (agent?: Agent) => {
            $scope.agent = agent || new Agent();
            $scope.display.lightbox.agent = true;
        };

        $scope.validAgent = async (agent: Agent) => {
            await agent.save();
            await $scope.agents.sync();
            delete $scope.agent;
            $scope.allAgentSelected = false;
            $scope.display.lightbox.agent = false;
            $scope.$apply();
        };

        $scope.cancelAgentForm = () => {
            $scope.display.lightbox.agent = false;
            delete $scope.agent;
        };

        $scope.deleteAgents = async (agents: Agent[]) => {
            await $scope.agents.delete(agents);
            await $scope.agents.sync();
            $scope.allAgentSelected = false;
            $scope.$apply();
        };

        $scope.switchAllAgent = (allAgentSelected: boolean) => {
            allAgentSelected ? $scope.agents.selectAll() : $scope.agents.deselectAll();
            $scope.$apply();
        };
    }]);
