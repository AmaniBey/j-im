/* eslint-env node */

/*
 * This file runs in a Node context (it's NOT transpiled by Babel), so use only
 * the ES6 features that are supported by your Node version. https://node.green/
 */

// Configuration for your app
// https://v2.quasar.dev/quasar-cli-vite/quasar-config-js
const {configure} = require('quasar/wrappers');

const path = require('node:path')
module.exports = configure(function (ctx) {

    return {
        define:{
            __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false',
        },
        eslint: {},

        // https://v2.quasar.dev/quasar-cli-vite/prefetch-feature
        // preFetch: true,

        // app boot file (/src/boot)
        // --> boot files are part of "main.js"
        // https://v2.quasar.dev/quasar-cli-vite/boot-files
        boot: [
            'eventbus',
            'axios',
            'stomp',
            'component',
            'dialog',
            'defConstant',
            'app'
        ],

        // https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#css
        css: [
            'app.scss',

        ],

        // https://github.com/quasarframework/quasar/tree/dev/extras
        extras: [
            // 'ionicons-v4',
            'mdi-v5',
            // 'fontawesome-v6',
            // 'eva-icons',
            // 'themify',
            // 'line-awesome',
            // 'roboto-font-latin-ext', // this or either 'roboto-font', NEVER both!
            'roboto-font', // optional, you are not bound to it
            'material-icons', // optional, you are not bound to it
        ],

        // Full list of options: https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#build
        build: {

            vendor: [
                'vue-cropper',
            ],
            target: {
                browser: ['es2019', 'edge88', 'firefox78', 'chrome87', 'safari13.1'],
                node: 'node16'
            },
            chainWebpack(chain) {
                chain.plugin('eslint-webpack-plugin')
                    .use(ESLintPlugin, [{extensions: ['ts', 'js', 'vue']}])
            },

            vueRouterMode: 'hash', // available values: 'hash', 'history'
            // vueRouterBase,
            // vueDevtools,
            // vueOptionsAPI: false,

            // rebuildCache: true, // rebuilds Vite/linter/etc cache on startup

            publicPath: '/',
            // analyze: true,
            // env: {},
            // rawDefine: {}
            // ignorePublicFolder: true,
            // minify: false,
            // polyfillModulePreload: true,
            // distDir

            // extendViteConf (viteConf) {},
            // viteVuePluginOptions: {},


            vitePlugins: [
                [ 'unplugin-auto-import/vite', {imports: ['vue', 'vue-router', 'quasar','pinia'],
                    dts: 'src/auto-imports.d.ts', }, ],
                {src: '~/plugins/vue-cropper', ssr: false}
            ],
            extendViteConf(viteConf, {isServer, isClient}) {
                Object.assign(viteConf.resolve.alias, {
                    "@": path.join(__dirname, 'src')
                })
            }

        },

        // Full list of options: https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#devServer
        devServer: {
            open: false,
            host: '0.0.0.0',
            port: 9001,
            proxy: {
                '^/dev-api': {
                    target: "http://127.0.0.1:8080",
                    // target: "http://117.72.67.125:8080", // 京东云
                    // target: "http://192.168.10.103:8080",
                    changOrigin: true,   //如果接口跨域这里就要这个参数配置
                    // 请求地址 http://localhost:9000/dev-api/xxx 实际发送的确实: http://localhost:8080/xxxx
                    rewrite: (path) => path.replace(new RegExp('^/dev-api'), ''),
                    ws: true,
                }
            }
        },
        // https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#framework
        framework: {
            config: {
                notify: {},
            },

            // iconSet: 'material-icons', // Quasar icon set
            // lang: 'en-US', // Quasar language pack

            // For special cases outside of where the auto-import strategy can have an impact
            // (like functional components as one of the examples),
            // you can manually specify Quasar components/directives to be available everywhere:
            //
            // components: [],
            // directives: [],

            // Quasar plugins
            plugins: [
                'Notify',
                'Dialog',
                'Loading',
                'LoadingBar',
            ]
        },

        // animations: 'all', // --- includes all animations
        // https://v2.quasar.dev/options/animations
        animations: [],

        // https://v2.quasar.dev/quasar-cli-vite/quasar-config-js#sourcefiles
        // sourceFiles: {
        //   rootComponent: 'src/App.vue',
        //   router: 'src/router/index',
        //   store: 'src/store/index',
        //   registerServiceWorker: 'src-pwa/register-service-worker',
        //   serviceWorker: 'src-pwa/custom-service-worker',
        //   pwaManifestFile: 'src-pwa/manifest.json',
        //   electronMain: 'src-electron/electron-main',
        //   electronPreload: 'src-electron/electron-preload'
        // },

        // https://v2.quasar.dev/quasar-cli-vite/developing-ssr/configuring-ssr
        ssr: {
            // ssrPwaHtmlFilename: 'offline.html', // do NOT use index.html as name!
            // will mess up SSR

            // extendSSRWebserverConf (esbuildConf) {},
            // extendPackageJson (json) {},

            pwa: false,

            // manualStoreHydration: true,
            // manualPostHydrationTrigger: true,

            prodPort: 3000, // The default port that the production server should use
                            // (gets superseded if process.env.PORT is specified at runtime)

            middlewares: [
                'render' // keep this as last one
            ]
        },

        // https://v2.quasar.dev/quasar-cli-vite/developing-pwa/configuring-pwa
        pwa: {
            workboxMode: 'generateSW', // or 'injectManifest'
            injectPwaMetaTags: true,
            swFilename: 'sw.js',
            manifestFilename: 'manifest.json',
            useCredentialsForManifestTag: false,
            // useFilenameHashes: true,
            // extendGenerateSWOptions (cfg) {}
            // extendInjectManifestOptions (cfg) {},
            // extendManifestJson (json) {}
            // extendPWACustomSWConf (esbuildConf) {}
        },

        // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-cordova-apps/configuring-cordova
        cordova: {
            // noIosLegacyBuildFlag: true, // uncomment only if you know what you are doing
        },

        // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-capacitor-apps/configuring-capacitor
        capacitor: {
            hideSplashscreen: true
        },

        // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-electron-apps/configuring-electron
        electron: {
            // extendElectronMainConf (esbuildConf)
            // extendElectronPreloadConf (esbuildConf)

            inspectPort: 5858,

            bundler: 'packager', // 'packager' or 'builder'

            packager: {
                // https://github.com/electron-userland/electron-packager/blob/master/docs/api.md#options

                // OS X / Mac App Store
                // appBundleId: '',
                // appCategoryType: '',
                // osxSign: '',
                // protocol: 'myapp://path',

                // Windows only
                // win32metadata: { ... }
            },

            builder: {
                // https://www.electron.build/configuration/configuration

                appId: 'im-ui'
            }
        },

        // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-browser-extensions/configuring-bex
        bex: {
            contentScripts: [
                'my-content-script'
            ],

            // extendBexScriptsConf (esbuildConf) {}
            // extendBexManifestJson (json) {}
        }
    }
});

