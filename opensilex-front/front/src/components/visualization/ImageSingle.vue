<template>
  <div class="col-md-3 col-sm-4 cardContainer">
    <b-card style="max-width: 20rem">
      <b-card-img
        :src="src"
        class="card-img-top"
        @click="showImage"
      ></b-card-img>

      <b-card-body>
        <b-card-text><span class="imageAttribut">Date:</span> {{ date }}</b-card-text>
        <b-card-text><span class="imageAttribut">Type:</span> {{ type }}</b-card-text>
        <b-card-text><span class="imageAttribut">{{ $t("ImageSingle.target") }}</span> : {{ image.target }}</b-card-text>
        <b-card-text><span class="imageAttribut"> {{ $t("ImageSingle.filename") }}</span> : {{ image.filename }}</b-card-text>

        <b-card-text v-if="image.metadata">{{ $t("component.common.metadata") }}: {{ image.metadata }}</b-card-text>
        <div class="d-flex justify-content-between">
          <div
            @click="annotate"
            role="button"
            v-b-tooltip.hover.top="$t('ImageSingle.annotate')"
            class="m-2 cardAnnotationIcon"
          >
          <opensilex-Icon icon="fa#pencil-alt" />
          </div>
          <div
            @click="toogleAdvancedSearch"
            v-b-toggle="'collapse-' + index"
            v-b-tooltip.hover.top="$t('ImageSingle.provenance')"
            class="m-2 cardAdvancedSearchIcon"
          >
            <opensilex-Icon v-if="!detailOpen" icon="fa#eye" />
            <opensilex-Icon v-else icon="ik#ik-minus" />
          </div>
        </div>

        <!-- Image details on click -->
        <b-collapse :id="'collapse-' + index" class="mt-2 provenanceInformations">
         <b-card-text><span class="imageAttribut">{{ $t("DataView.filter.provenance") }}:</span></b-card-text>
          <b-card-text>
            <span class="imageAttribut">{{ $t("component.common.uri") }}:</span> {{ image.provenance.uri }}
          </b-card-text>
            <b-card-text v-if="image.provenance.prov_used">
          <span class="imageAttribut">{{ $t("ImageSingle.used") }}:</span> {{ image.provenance.prov_used }}
          </b-card-text>
            <b-card-text v-if="image.provenance.prov_was_associated_with">
          <span class="imageAttribut">{{ $t("ImageSingle.associated") }}:</span> {{ image.provenance.prov_was_associated_with }}
          </b-card-text>
            <b-card-text  v-if="image.provenance.settings">
          <span class="imageAttribut">{{ $t("ImageSingle.setting") }}:</span> {{ image.provenance.settings }}
          </b-card-text> 
            <b-card-text  v-if="image.provenance.experiments">
          <span class="imageAttribut">{{ $t("DataView.filter.experiments") }}:</span> {{ image.provenance.experiments }}
          </b-card-text>

        </b-collapse>
      </b-card-body>
    </b-card>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";

import Vue from "vue";
import {DataFileImageDTO} from "../data/DataFileImageDTO";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class ImageSingle extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  image: DataFileImageDTO;

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
    this.date = this.$opensilex.$dateTimeFormatter.formatISODateTime(this.image.date);
    this.type = this.image.rdf_type.split(":")[1];
  }

  showImage() {
    this.$emit("click", this.index);
  }

  annotate() {
  this.$emit("annotate",this.image.uri);
  }
}
</script>

<style scoped lang="scss">
.cardContainer {
  padding: 0 10px 10px 0;
}

.card .card-body {
  padding: 0;
  background-color: #FFFFFF;
}

.provenanceInformations{
  margin-bottom: 10px;
}

.card-img-top {
  margin-bottom: 10px;
}

p {
  font-size: 9px;
}

.imageAttribut{
  font-weight: bold;
}

.cardAdvancedSearchIcon, .cardAnnotationIcon {
  padding: 1px 5px 1px 5px;
  border-radius: 50%;
  font-size: 1.2em
}

.card-text {
  margin-bottom: 0;
  font-size: 1em;
  margin: 0 5px 0 5px
}

img {
  width: 100% !important;
  max-width: 400px;
}

img:hover {
  cursor: pointer;
}

@media(min-width: 992px){
  .cardContainer {
    -webkit-box-flex: 0;
    flex: 0 0 16.5%;
    max-width: 16.5%
  }
}
</style>

<i18n>
  en:
    ImageSingle:
      filename: Name
      target: Target
      used: 'Used by'
      associated: 'Associated to'
      setting: 'Settings'
      annotate: Annotate
      provenance: Provenance

  fr:
    ImageSingle:
      filename: Nom
      target: Cible
      used: 'Utilisée par'
      associated: 'Asssociée à'
      setting: 'Paramètres'
      annotate: Annoter
      provenance: Provenance
</i18n>

