const TerserPlugin = require('terser-webpack-plugin');
const webpack = require('webpack');
const path = require('path');

let publicPath = "./osfront";

if (process.env.NODE_ENV !== 'production') {
    publicPath = "/app"
}

module.exports = {
    publicPath: publicPath,
    devServer: {
        progress: false
    },
    configureWebpack: {
        externals: {
            'opensilex': 'opensilex'
        },
        resolve: {
            extensions: ['.md'],
            alias: {
                'vue$': path.resolve('../../node_modules/vue/dist/vue.esm.js'),
                "opensilex-security": path.resolve("../../opensilex-security/front/src"),
                "opensilex-core": path.resolve("../../opensilex-core/front/src"),
                "opensilex-phis": path.resolve("../../opensilex-phis/front/src")
            }
        },
        performance: {
            hints: false
        },
        optimization: {
            minimize: (process.env.NODE_ENV === 'production'),
            minimizer: [new TerserPlugin()]
        },
        plugins: [
            new webpack.DefinePlugin({
                'APPLICATION_VERSION': JSON.stringify(require('./package.json').version),
            })
        ],
        devtool: 'source-map'
    },
    chainWebpack: config => {
        config.module
            .rule("i18n")
            .resourceQuery(/blockType=i18n/)
            .type('javascript/auto')
            .use("i18n")
            .loader("@kazupon/vue-i18n-loader")
            .end()
            .use('yaml')
            .loader('yaml-loader')
            .end()

        // Add markdown file parsing
        config.module.rule('md')
            .test(/\.md$/)
            .use('vue-loader')
            .loader('vue-loader')
            .end()
            .use('vue-markdown-loader')
            .loader('vue-markdown-loader/lib/markdown-compiler')
            .options({
                raw: true
            })

        if (process.env.NODE_ENV === 'production') {
            config.module.rule('vue').uses.delete('cache-loader');
            config.module.rule('js').uses.delete('cache-loader');
            config.module.rule('ts').uses.delete('cache-loader');
            config.module.rule('tsx').uses.delete('cache-loader');
        }
    }
};
