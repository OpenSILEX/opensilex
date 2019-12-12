const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const webpack = require("webpack");

module.exports = {
    publicPath: "/app/",
    devServer: {
        progress: false
    },
    configureWebpack: {
        externals: {
            'opensilex': 'opensilex',
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
        },
        plugins: [new webpack.ProvidePlugin({
            "vuePropertyDecorator": "vue-property-decorator",
            "vueClassComponent": "vue-class-component"
        })]
    }
};