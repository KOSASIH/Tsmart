const ModuleFederationPlugin = require('webpack/lib/container/ModuleFederationPlugin');
const packageJson = require('../package.json');
const appVersion = packageJson.version;

module.exports = (config, options, targetOptions) => {
  return {
    experiments: {
      outputModule: true,
    },
    plugins: [
      new ModuleFederationPlugin({
        library: {
          type: 'module',
        },
        name: 'tsmart',
        filename: 'remoteEntry.js',
        exposes: {
          './translation-module': 'app/shared/language/translation.module.ts',
          './entity-navbar-items': 'app/entities/entity-navbar-items.ts',
          './entity-routing': 'app/entities/entity-routing.module.ts',
        },
        shared: {
          // { eager?, import?, packageName?, requiredVersion?, shareKey?, shareScope?, singleton?, strictVersion?, version? }
          '@angular/core': { singleton: true, strictVersion: true, requiredVersion: '16.1.4' },
          '@angular/common': { singleton: true, strictVersion: true, requiredVersion: '16.1.4' },
          '@angular/common/http': { singleton: true, strictVersion: true, requiredVersion: '16.1.4' },
          '@angular/router': { singleton: true, strictVersion: true, requiredVersion: '16.1.4' },
          '@angular/forms': { singleton: true, strictVersion: true, requiredVersion: '16.1.4' },
          '@angular/platform-browser': { singleton: true, strictVersion: true, requiredVersion: '16.1.4' },
          '@fortawesome/angular-fontawesome': { singleton: true, strictVersion: true, requiredVersion: '0.13.0' },
          '@fortawesome/fontawesome-common-types': { singleton: true, strictVersion: true },
          '@fortawesome/fontawesome-svg-core': { singleton: true, strictVersion: true, requiredVersion: '6.4.0' },
          '@fortawesome/free-solid-svg-icons': { singleton: true },
          '@ng-bootstrap/ng-bootstrap': { singleton: true, strictVersion: true, requiredVersion: '15.1.0' },
          '@ngx-translate/core': { singleton: true, strictVersion: true, requiredVersion: '15.0.0' },
          '@ngx-translate/http-loader': { singleton: true, strictVersion: true, requiredVersion: '8.0.0' },
          dayjs: { singleton: true, strictVersion: true, requiredVersion: '1.11.9' },
          'ngx-infinite-scroll': { singleton: true, strictVersion: true, requiredVersion: '16.0.0' },
          rxjs: { singleton: true, strictVersion: true, requiredVersion: '7.8.1' },
          'rxjs/operators': { singleton: true, strictVersion: true, requiredVersion: '7.8.1' },
          'zone.js': { singleton: true, strictVersion: true, requiredVersion: '0.13.1' },
          'app/config/input.constants': {
            singleton: true,
            import: 'app/config/input.constants',
            requiredVersion: appVersion,
          },
          'app/config/pagination.constants': {
            singleton: true,
            import: 'app/config/pagination.constants',
            requiredVersion: appVersion,
          },
          'app/config/translation.config': {
            singleton: true,
            import: 'app/config/translation.config',
            requiredVersion: appVersion,
          },
          'app/core/auth': {
            singleton: true,
            import: 'app/core/auth',
            requiredVersion: appVersion,
          },
          'app/core/config': {
            singleton: true,
            import: 'app/core/config',
            requiredVersion: appVersion,
          },
          'app/core/interceptor': {
            singleton: true,
            import: 'app/core/interceptor',
            requiredVersion: appVersion,
          },
          'app/core/request': {
            singleton: true,
            import: 'app/core/request',
            requiredVersion: appVersion,
          },
          'app/core/util': {
            singleton: true,
            import: 'app/core/util',
            requiredVersion: appVersion,
          },
          'app/shared': {
            singleton: true,
            import: 'app/shared',
            requiredVersion: appVersion,
          },
          'app/shared/alert': {
            singleton: true,
            import: 'app/shared/alert',
            requiredVersion: appVersion,
          },
          'app/shared/auth': {
            singleton: true,
            import: 'app/shared/auth',
            requiredVersion: appVersion,
          },
          'app/shared/date': {
            singleton: true,
            import: 'app/shared/date',
            requiredVersion: appVersion,
          },
          'app/shared/language': {
            singleton: true,
            import: 'app/shared/language',
            requiredVersion: appVersion,
          },
          'app/shared/pagination': {
            singleton: true,
            import: 'app/shared/pagination',
            requiredVersion: appVersion,
          },
          'app/shared/sort': {
            singleton: true,
            import: 'app/shared/sort',
            requiredVersion: appVersion,
          },
        },
      }),
    ],
    output: {
      publicPath: 'auto',
      uniqueName: 'tsmart',
    },
    optimization: {
      runtimeChunk: false,
    },
  };
};
