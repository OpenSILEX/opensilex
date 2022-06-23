<template>
  <div class="col-lg-2 col-md-3 col-sm-4">
    <b-card style="max-width: 20rem">
      <b-card-img
        :src="src"
        class="card-img-top"
        @click="showImage"
      ></b-card-img>

      <b-card-body>
        <b-card-text>Date: {{ date }}</b-card-text>
        <b-card-text>Type: {{ type }}</b-card-text>
        <b-card-text> {{ $t("ImageSingle.target") }} : {{ image.target }}</b-card-text>
        <b-card-text> {{ $t("ImageSingle.filename") }} : {{ image.filename }}</b-card-text>

        <b-card-text v-if="image.metadata">Metadata: {{ image.metadata }}</b-card-text>
        <div class="d-flex justify-content-between">
          <div
            @click="annotate"
            role="button"
            v-b-tooltip.hover
            title="Annotate"
            class="m-2"
          >
          <opensilex-Icon icon="fa#pencil-alt" />
          </div>
          <div
            @click="toogleAdvancedSearch"
            v-b-toggle="'collapse-' + index"
            v-b-tooltip.hover
            title="Detail"
            class="m-2"
          >
            <opensilex-Icon v-if="!detailOpen" icon="ik#ik-plus" />
            <opensilex-Icon v-else icon="ik#ik-minus" />
          </div>
        </div>

        <b-collapse :id="'collapse-' + index" class="mt-2">
         <b-card-text>Provenance:</b-card-text>
          <b-card-text>
          URI: {{ image.provenance.uri }}
          </b-card-text>
            <b-card-text v-if="image.provenance.prov_used">
          USED: {{ image.provenance.prov_used }}
          </b-card-text>
            <b-card-text v-if="image.provenance.prov_was_associated_with">
          ASSOCIATED: {{ image.provenance.prov_was_associated_with }}
          </b-card-text>
            <b-card-text  v-if="image.provenance.settings">
           SETTING: {{ image.provenance.settings }}
          </b-card-text> 
            <b-card-text  v-if="image.provenance.experiments">
           EXPERIMENT: {{ image.provenance.experiments }}
          </b-card-text>

        </b-collapse>
      </b-card-body>
    </b-card>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";

import moment from "moment-timezone";
import Vue from "vue";

@Component
export default class ImageSingle extends Vue {
  @Prop()
  image: any;

  @Prop()
  index: number;

  imgs = [];
  date;
  type;

  src: string = "";

  detailOpen = false;

  toogleAdvancedSearch() {
    this.detailOpen = !this.detailOpen;
  }

  created() {
    this.src = this.image.url;
    this.date = moment.parseZone(this.image.date).format("DD-MM-YYYY HH:mm:ss");
    this.type= this.image.type.split(":")[1];
  }

  showImage() {
    this.$emit("click", this.index);
  }

  annotate() {
  this.$emit("annotate",this.image.uri);
    console.log(this.image.uri);
  }
}
</script>

<style scoped lang="scss">
.card .card-body {
  padding: 0;
}

.card {
  margin-bottom: 5px;
}
p {
  font-size: 9px;
}
.card-text {
  margin-bottom: 0;
}
img {
  width: 100% !important;
  max-width: 400px;
}

img:hover {
  cursor: pointer;
}
</style>

<i18n>
  fr: 
    ImageSingle:
      filename : Nom
      target : Cible

  en: 
    ImageSingle:
     filename : Name
     target : Target
</i18n>

