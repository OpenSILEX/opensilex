<template>
  <div class="col-lg-2 col-md-3 col-sm-4">
    <b-card :img-src="link" img-alt="Image" img-top tag="article" style="max-width: 10rem;" @click="imageClicked" class>
      <b-card-text>{{objectType}}</b-card-text>
      <b-card-text>{{objectAlias}}</b-card-text>
      <b-card-text>{{formatedDateValue}}</b-card-text>
    </b-card>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { EventBus } from "./../event-bus";
import { Image } from "./../image";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { ScientificObjectsService } from "../../../lib/api/scientificObjects.service";
import { ScientificObjectDTO } from "../../../lib/model/scientificObjectDTO";

@Component
export default class ImageSingle extends Vue {
  @Prop()
  image: Image;
  @Prop()
  index: any;
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }
 
  link:string = "";
  objectType:string="";
  objectUri:string="";
  objectAlias:string="";
  formatedDateValue:string = "";

  created() {
    this.link = this.image.uri;
    this.formatedDateValue = this.formatedDate(this.image.date);
    this.objectType=this.image.objectType.split("#")[1];
    this.objectUri=this.image.objectUri;
    this.getObjectAlias();
  }

  getObjectAlias(){
     let service: ScientificObjectsService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
    const result = service
      .getScientificObjectsBySearch(
        this.user.getAuthorizationHeader(),
        1,
        0,
        this.image.objectUri,
        undefined,
        undefined,
        undefined
      )
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ScientificObjectDTO>>>) => {
          const res = http.response.result as any;
          this.image.objectAlias=res.data[0].label;
          this.objectAlias=res.data[0].label;
        }
      )
      .catch(error => {
        console.log(error);
      });
  }


  formatedDate(date) {
    const newDate = new Date(date);
    const options = {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    };
    return newDate.toLocaleDateString("fr-FR", options);
  }
  imageClicked(){
    console.log(this.index);
    EventBus.$emit("imageIsClicked", this.index);
  }
}
</script>

<style scoped lang="scss">
.card .card-body {
    padding: 0;
}
.card .card-text{
  margin-bottom: 0;
}
img {
  width: 100%;
  max-width: 400px;
}
</style>
