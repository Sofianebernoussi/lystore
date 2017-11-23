import { Behaviours } from 'entcore';

Behaviours.register('lystore', {
	rights: {
		workflow: {},
		resource: {}
	},
	loadResources: async function (): Promise<void> {}
});
