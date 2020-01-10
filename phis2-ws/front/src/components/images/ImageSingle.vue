<template>
  <div>
    <b-img-lazy :src="link" alt="Image 1"></b-img-lazy>
  </div>
</template>

<script lang="ts">

import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ImageSingle extends Vue {

  @Prop()
  image: any;

  link: any="";
  
  created() {
    this.link="http://localhost:8666/rest/data/file/"+encodeURIComponent(this.image);
   
    //  let vm = this;
    // dataService.getDataFile(this.image).then(
    //       (
    //         http: HttpResponse<OpenSilexResponse<Array<Uri>>>
    //       ) => {
            
    //        vm.link= http.response;
    //       }).catch(this.$opensilex.errorHandler);
   
   
  }
   
  }
}
</script>

<style scoped lang="scss">
</style>
