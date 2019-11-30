module.exports = {
  chainWebpack: config => {
    config.externals({
      ...config.get('externals'),
      'opensilex-front-lib': 'opensilex-front-lib'
    })
  }
}