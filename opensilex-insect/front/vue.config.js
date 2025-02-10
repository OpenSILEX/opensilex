const TerserPlugin = require('terser-webpack-plugin');
const path = require('path');

module.exports = {
    configureWebpack: {
        externals: {
            'vue': 'Vue',
            'vue-router': 'vue-router',
            'vuex': 'vuex',
            'node-fetch': 'node-fetch',
            'vee-validate': 'vee-validate'
        },
        resolve: {
			alias: {
				'vue$': path.resolve('../../node_modules/vue/dist/vue.esm.js')
            }
		},
        performance: {
            hints: false
        },
        optimization: {
            minimize: (process.env.NODE_ENV === 'production'),
            minimizer: [new TerserPlugin()]
        }
    }
};