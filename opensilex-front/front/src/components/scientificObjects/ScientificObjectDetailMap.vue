<template>
  <div v-if="propertiesOfSelected && geometryOfSelected && propertiesOfSelected.uri">
    <!-- Name -->
    <span v-if="withBasicProperties" class="field-view-title">{{ $t("component.common.name") }}</span>
    <opensilex-UriLink
      :to="{ path: '/scientific-objects/details/' + encodeURIComponent(propertiesOfSelected.uri)}"
      :uri="propertiesOfSelected.uri"
      :value=" propertiesOfSelected.name + ' (' + ( propertiesOfSelected.rdf_type_name.charAt(0).toUpperCase() + propertiesOfSelected.rdf_type_name.slice(1) ).bold() + ')'"
      target="_blank"
    >
    </opensilex-UriLink>
    <opensilex-OntologyObjectProperties
      :selected="propertiesOfSelected.OS"
      :parentType="oeso.SCIENTIFIC_OBJECT_TYPE_URI"
      :relations="propertiesOfSelected.OS.relations"
      :ignoredProperties="[oeso.IS_HOSTED]"
      :additionalFieldProps="{ experiment, target: '_blank' }"
      :showIncoming="false"
    >
    </opensilex-OntologyObjectProperties>
    <!-- Geometry-->
    <opensilex-GeometryCopy
      v-if="isViewAllInformation"
      :value="geometryOfSelected"
    ></opensilex-GeometryCopy>
    <p>
      <a v-on:click="isViewAllInformation = !isViewAllInformation">{{
        $t(
          isViewAllInformation
            ? "ScientificObjectDetailMap.seeMoreInformation"
            : "ScientificObjectDetailMap.viewAllInformation"
        )
      }}</a>
    </p>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {VueJsOntologyExtensionService} from "opensilex-core/api/vueJsOntologyExtension.service";
import {VueRDFTypePropertyDTO} from "opensilex-core/model/vueRDFTypePropertyDTO"
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {GeoJsonObject} from "opensilex-core/model/geoJsonObject";

@Component
export default class ScientificObjectDetailMap extends Vue {
  $opensilex: OpenSilexVuePlugin;
  vueService: VueJsOntologyExtensionService;

  @Prop()
  propertiesOfSelected: VueRDFTypePropertyDTO[];

  @Prop()
  geometryOfSelected: GeoJsonObject;

  isViewAllInformation: boolean = false;

  @Prop({
    default: true,
  })
  withBasicProperties;

  @Prop({
    default: null,
  })
  experiment;
  @Ref("soForm") readonly soForm!: any;

  get oeso() {
      return this.$opensilex.Oeso;
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created(){
    this.vueService = this.$opensilex.getService("VueJsOntologyExtensionService");
  }
}
</script>
<style lang="scss" scoped>
ol, ul, dl {
  margin-bottom: 0;
  line-height: 1em;
}

#vl-overlay-detailItem > div > div > div > div > span.field-view-title,
#vl-overlay-detailItem > div > div > div > div > span > a > span,
#vl-overlay-detailItem > div > div > div > div > div > span,
#vl-overlay-detailItem > div > div > div > div > div > span.field-view-title {
  line-height: 1em !important;
}

#vl-overlay-detailItem > div > div > div > div {
  padding-top: 2%;
  padding-left: 2%;
}

#vl-overlay-detailItem > div > div > div > div > p > a {
  color: #007bff;
  cursor: pointer;
}

#vl-overlay-detailItem > div > div > div > div > div > span.static-field-line {
  display: block !important;
}

::v-deep .capitalize-first-letter {
  display: block;
}

::v-deep a {
  color: #007bff;
}
</style>

<i18n>
en:
  ScientificObjectDetailMap:
    viewAllInformation: Show more
    seeMoreInformation: Show less
fr:
  ScientificObjectDetailMap:
    viewAllInformation: Voir plus
    seeMoreInformation: Voir moins
</i18n>