<template>
  <div v-if="selected && selected.uri">
    <!-- Name -->
    <span v-if="withBasicProperties" class="field-view-title">{{ $t("component.common.name") }}</span>
    <opensilex-UriLink
      :to="{ path: '/scientific-objects/details/' + encodeURIComponent(selected.uri)}"
      :uri="selected.uri"
      :value=" selected.name + ' (' + ( selected.rdf_type_name.charAt(0).toUpperCase() + selected.rdf_type_name.slice(1) ).bold() + ')'"
      target="_blank"
    >
    </opensilex-UriLink>
    <opensilex-OntologyObjectProperties
      :selected="selected"
      :parentType="oeso.SCIENTIFIC_OBJECT_TYPE_URI"
      :relations="selected.relations"
      :ignoredProperties="[oeso.IS_HOSTED]"
      :additionalFieldProps="{ experiment, target: '_blank' }"
      :showIncoming="false"
    >
    </opensilex-OntologyObjectProperties>
    <!-- Geometry-->
    <opensilex-GeometryCopy
      v-if="selected.geometry && isViewAllInformation"
      :value="selected.geometry"
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
import {VueJsOntologyExtensionService} from "../../lib";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {ScientificObjectDetailDTO} from "opensilex-core/model/scientificObjectDetailDTO";

@Component
export default class ScientificObjectDetailMap extends Vue {
  $opensilex: OpenSilexVuePlugin;
  vueService: VueJsOntologyExtensionService;
  @Prop()
  selected: ScientificObjectDetailDTO;
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

.field-view-title {
  min-width: 120px;
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