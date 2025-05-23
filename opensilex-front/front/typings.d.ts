// Declare markdown files as vue components
declare module '*.md' {
  import Vue from 'vue'
  export default Vue
}


declare module '*.vue' {
  import Vue from 'vue'
  export default Vue
}
declare module 'vue-easy-lightbox/dist/external-css/vue-easy-lightbox.esm.min.js' {
  import VueEasyLightbox from 'vue-easy-lightbox'
  export * from 'vue-easy-lightbox'
  export default VueEasyLightbox
}
