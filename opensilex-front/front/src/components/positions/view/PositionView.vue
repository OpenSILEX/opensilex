<template>
  <div>
    <opensilex-UriListView label="Event.targets" :list="getUriLinkDescriptions"></opensilex-UriListView>
    <opensilex-GeometryView label="component.common.geometry.geometry-title"
                            :value="positionObject.point"></opensilex-GeometryView>

    <opensilex-StringView label="component.common.geometry.x" :value="positionObject.x"></opensilex-StringView>
    <opensilex-StringView label="component.common.geometry.y" :value="positionObject.y"></opensilex-StringView>
    <opensilex-StringView label="component.common.geometry.z" :value="positionObject.z"></opensilex-StringView>

    <opensilex-TextView v-if="positionObject" label="component.common.geometry.textual-position"
                        :value="positionObject.text"></opensilex-TextView>

  </div>

</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {GeoJsonObject} from "opensilex-core/model/geoJsonObject";

export interface PositionFormObject {
  point: GeoJsonObject,
  x: string,
  y: string,
  z: string,
  text: string,
}

@Component
export default class PositionView extends Vue {

  @Prop()
  positionObject: PositionFormObject;

  @Prop()
  targetUris: Array<string>;

  @Prop({default: () => {} })
  targetLabelsByUri: {[key : string] : string};

  @Prop({default: () => {} })
  targetUriPathsByUri: {[key : string] : string};

  get getUriLinkDescriptions() {
    return this.targetUris.map(targetUri => ({
      uri: targetUri,
      value: this.targetLabelsByUri[targetUri],
      to: this.targetUriPathsByUri[targetUri]
    }));
  }

}

</script>

