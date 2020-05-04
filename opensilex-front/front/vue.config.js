const TerserPlugin = require('terser-webpack-plugin');
var webpack = require('webpack');

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
            minimize: (process.env.NODE_ENV === 'production'),
            minimizer: [new TerserPlugin()]
        },
        plugins: [
            new webpack.DefinePlugin({
                'APPLICATION_VERSION': JSON.stringify(require('./package.json').version),
            })
        ]
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
