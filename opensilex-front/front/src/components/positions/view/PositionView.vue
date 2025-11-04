<template>
  <div v-if="hasPosition()">
    <opensilex-UriListView label="Event.targets" :list="getUriLinkDescriptions"></opensilex-UriListView>
    <opensilex-GeometryView label="component.common.geometry"
                            :value="positionObject.point"></opensilex-GeometryView>

    <opensilex-StringView label="Position.x" :value="positionObject.x"></opensilex-StringView>
    <opensilex-StringView label="Position.y" :value="positionObject.y"></opensilex-StringView>
    <opensilex-StringView label="Position.z" :value="positionObject.z"></opensilex-StringView>

    <opensilex-TextView v-if="positionObject" label="Position.textual-position"
                        :value="positionObject.text"></opensilex-TextView>

  </div>

</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {PositionFormObject} from "../form/PositionForm.vue";

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

