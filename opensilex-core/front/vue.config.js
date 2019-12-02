const UglifyJsPlugin = require('uglifyjs-webpack-plugin');

module.exports = {
    configureWebpack: {
        externals: {
            'vue': 'Vue',
            'core-js': 'core-js',
            'vue-class-component': 'vue-class-component',
            'vue-property-decorator': 'vue-property-decorator',
            'vue-router': 'vue-router',
            'vuex': 'vuex',
            'inversify': 'inversify',
            'node-fetch': 'node-fetch',
            'whatwg-fetch': 'whatwg-fetch',
            'reflect-metadata': 'reflect-metadata',
            'rxjs': 'rxjs',
            'rxjs-compat': 'rxjs-compat'
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
    }
};