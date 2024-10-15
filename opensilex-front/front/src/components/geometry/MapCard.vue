<!--
  - ******************************************************************************
  -                         MapCard.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 08/10/2024 15:39
  - Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
  - ******************************************************************************
  -->
<template>
  <div>
    <vl-map ref="globalMap" data-projection="EPSG:4326" class="site-map">
      <!-- Zoom and position -->
      <vl-view ref="globalMapView" :min-zoom="2" :max-zoom="22" @update:center="getCurrentExtent"></vl-view>
      <vl-layer-tile>
        <vl-source-osm/>
      </vl-layer-tile>
      <!-- SITE layer -->
      <vl-layer-vector v-if="isMapMounted" id="site-layer-vector" ref="siteLayerVector" render-mode="image">
        <vl-source-vector :features="features" @mounted="focusOnFeatures"></vl-source-vector>
        <vl-style-box>
          <vl-style-circle :radius="8">
            <vl-style-stroke
                :width="4"
                color="red"
            ></vl-style-stroke>
            <vl-style-fill
                color="white"
            ></vl-style-fill>
          </vl-style-circle>
        </vl-style-box>
      </vl-layer-vector>
    </vl-map>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {SiteGetWithGeometryDTO} from "opensilex-core/model/siteGetWithGeometryDTO";
import {Layer} from "vuelayers/src/component/vector-layer";
import {Prop, Ref} from "vue-property-decorator";
import {View} from "vuelayers/src/component/map";
import {transformExtent} from "vuelayers/src/ol-ext/proj";

export interface feature {
  id: string;
  properties: { geometry };
  geometry: object;
  type: "Feature";
}


@Component
export default class MapCard extends Vue {
  //#region Prop
  @Prop()
  private features: feature[];
  //#endregion


  //#region Refs
  @Ref("globalMapView")
  private globalMapView!: View;
  @Ref("siteLayerVector")
  private readonly siteLayerVector!: Layer;
  //#endregion

  //#region computed
  get isMapMounted(){
    return this.features?.length > 0;
  }
  //#endregion


  //#region Private Methods
  private getCurrentExtent() {
    return transformExtent(
        this.globalMapView.$view.calculateExtent(),
        "EPSG:3857",
        "EPSG:4326"
    );
  }

  private focusOnFeatures(){
    setTimeout(()=>{
      if(this.features.length > 0 && this.siteLayerVector.getSource()){
        this.fitViewWithFeaturesExtent(this.siteLayerVector.getSource().getExtent());
      }
    },1)
  }

  private fitViewWithFeaturesExtent(extent){
    this.globalMapView.$view.fit(extent, {maxZoom: 10});
  }
  //#endregion
}
</script>

<style scoped lang="scss">
.site-map {
  height: 20vh;
}
</style>