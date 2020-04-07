const TerserPlugin = require('terser-webpack-plugin');

module.exports = {
    configureWebpack: {
        externals: {
            'vue': 'Vue',
            'core-js': 'core-js',
            'vue-router': 'vue-router',
            'vuex': 'vuex',
            'node-fetch': 'node-fetch'
        },
        performance: {
            hints: false
        },
        optimization: {
            minimize: true,
            minimizer: [new TerserPlugin()]
        }
    }
};