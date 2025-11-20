<template>
  <div>
    <opensilex-UriListView label="Event.targets" :list="getUriLinkDescriptions"></opensilex-UriListView>
    <opensilex-GeometryView label="component.common.geometry"
                            :value="positionObject.point"></opensilex-GeometryView>

    <opensilex-StringView label="PositionView.x" :value="positionObject.x"></opensilex-StringView>
    <opensilex-StringView label="PositionView.y" :value="positionObject.y"></opensilex-StringView>
    <opensilex-StringView label="PositionView.z" :value="positionObject.z"></opensilex-StringView>

    <opensilex-TextView v-if="positionObject" label="PositionView.textual-position"
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
<i18n>
en:
    PositionView:
        textual-position: Textual position
        x: X
        y: Y
        z: Z

fr:
    PositionView:
        textual-position: Position textuelle
        x: X
        y: Y
        z: Z

</i18n>
