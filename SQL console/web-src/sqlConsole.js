/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

import angular from 'angular';
import maSqlConsole from './components/sqlConsole';
import maSqlConsoleFactory from './services/sqlConsole';

const sqlConsoleModule = angular.module('maSqlConsole', [])
.component('maSqlConsole', maSqlConsole)
.factory('maSqlConsole', maSqlConsoleFactory)
.config(['maUiMenuProvider', function(maUiMenuProvider) {
    maUiMenuProvider.registerMenuItems([
        {
            name: 'ui.settings.sqlConsole',
            url: '/sql-console',
            template: '<ma-sql-console></ma-sql-console>',
            menuTr: 'header.sql',
            menuIcon: 'storage',
            permission: ['superadmin'],
            weight: 2000,
            menuHidden: true,
            showInUtilities: true,
            params: {
                noPadding: false,
                hideFooter: false,
                helpPage: 'ui.help.sqlConsole'
            },
        },
        {
            name: 'ui.help.sqlConsole',
            url: '/sql-console/help',
            templatePromise() {
                return import(/* webpackMode: "eager" */ './help/helpPage.html');
            },
            menuTr: 'header.sql'
        }
    ]);
}]);

export default sqlConsoleModule;
