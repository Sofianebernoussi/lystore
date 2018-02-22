import { ng, idiom } from 'entcore';

export const currency = ng.filter('currency', () =>
    (amount) => (parseFloat(amount).toLocaleString(undefined,
        {minimumFractionDigits: 2, maximumFractionDigits: 2}) +
        ' ' + idiom.translate('money.symbol'))
);
