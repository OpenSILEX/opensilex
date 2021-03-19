<template>
  <opensilex-StringView label="component.geometry">
    <div class="static-field">
      <b-badge
          :title="$t('component.copyGeometryWKT')"
          class="uri-copy mr-2 copy"
          variant="secondary"
          v-on:click.prevent.stop="copyGeometry(wktValue(value), 'WKT')"
      >
        {{ $t("component.geometryWKT") }}
        <opensilex-Icon icon="ik#ik-copy"/>
      </b-badge>
      <b-badge
          :title="$t('component.copyGeometryGeoJSON')"
          class="uri-copy copy"
          variant="secondary"
          v-on:click.prevent.stop="copyGeometry(JSON.stringify(value), 'GeoJSON')"
      >
        {{ $t("component.geometryGeoJSON") }}
        <opensilex-Icon icon="ik#ik-copy"/>
      </b-badge>
    </div>
  </opensilex-StringView>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {stringify} from "wkt";
import copy from "copy-to-clipboard";

@Component
export default class GeometryCopy extends Vue {
  @Prop()
  value: string;

  $opensilex: any;
  $t: any;
  $i18n: any;

  wktValue(geometry) {
    try {
      return stringify(geometry);
    } catch (error) {
      return "";
    }
  }

  copyGeometry(address, standard) {
    copy(address);

    if (standard == "GeoJSON") {
      this.$opensilex.showSuccessToast(this.$t("component.copiedGeometryGeoJSON"));
    } else {
      this.$opensilex.showSuccessToast(this.$t("component.copiedGeometryWKT"));
    }
  }
}
</script>

<style lang="scss" scoped>
  .copy            { cursor: copy; }
</style>
<i18n>
en:
  component:
    geometry: Geospatial coordinates
    copyGeometryGeoJSON: Copy geospatial coordinates to GeoJSON format
    copiedGeometryGeoJSON: Geospatial coordinates have been copied to GeoJSON format.
    geometryGeoJSON: GeoJSON
    copyGeometryWKT: Copy geospatial coordinates to WKT format
    copiedGeometryWKT: Geospatial coordinates have been copied to  WKT format.
    geometryWKT: WKT
fr:
  component:
    geometry: Coordonnées géospatiales
    copyGeometryGeoJSON: Copier les coordonnées géospatiales au format GeoJSON
    copiedGeometryGeoJSON: Les coordonnées géospatiales ont été copiées au format GeoJSON.
    geometryGeoJSON: GeoJSON
    copyGeometryWKT: Copier les coordonnées géospatiales au format WKT
    copiedGeometryWKT: Les coordonnées géospatiales ont été copiées au format WKT.
    geometryWKT: WKT
</i18n>
