const UglifyJsPlugin = require('uglifyjs-webpack-plugin');

module.exports = {
     publicPath: "/app/",
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
          }
     }
};