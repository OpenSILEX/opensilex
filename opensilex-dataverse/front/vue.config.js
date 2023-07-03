const TerserPlugin = require('terser-webpack-plugin');
const webpack = require('webpack');
const path = require('path');

module.exports = {
    configureWebpack: {
        externals: {
            'opensilex': 'opensilex',
            'vue': 'Vue',
            'core-js': 'core-js',
            'vue-router': 'vue-router',
            'vuex': 'vuex',
            'node-fetch': 'node-fetch',
            'vee-validate': 'vee-validate'
        },
        resolve: {
            extensions: ['.md'],
			alias: {
				'vue$': path.resolve('../../node_modules/vue/dist/vue.esm.js'),
                "opensilex-security": path.resolve(__dirname, "../../opensilex-security/front/src"),
                "opensilex-core": path.resolve(__dirname,"../../opensilex-core/front/src"),
                "opensilex-dataverse": path.resolve(__dirname,"../../opensilex-dataverse/front/src")
            }
		},
        performance: {
            hints: false
        },
        optimization: {
            minimize: (process.env.NODE_ENV === 'production'),
            minimizer: [new TerserPlugin()]
        }
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