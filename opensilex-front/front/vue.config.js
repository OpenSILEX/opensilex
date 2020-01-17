const TerserPlugin = require('terser-webpack-plugin');

module.exports = {
    publicPath: "/app/",
    devServer: {
        progress: false
    },
    configureWebpack: {
        externals: {
            'opensilex': 'opensilex'
        },
        performance: {
            hints: false
        },
        optimization: {
            minimize: true,
            minimizer: [new TerserPlugin()]
        }
    },
    chainWebpack: config => {
        config.module
          .rule('i18n')
          .resourceQuery(/blockType=i18n/)
          .type('javascript/auto')
          .use('i18n')
          .loader('@kazupon/vue-i18n-loader')
    }
};