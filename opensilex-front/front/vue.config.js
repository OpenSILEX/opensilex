const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const webpack = require("webpack");

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
            minimizer: [
                new UglifyJsPlugin({
                    uglifyOptions: {
                        output: {
                            comments: false
                        }
                    }
                })
            ]
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