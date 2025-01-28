<template>
    <div v-if="hasPosition()">
        <opensilex-UriView
                title="Event.targets"
                :uri="getTargetPosition()"
                :value="positionsUriLabels[getTargetPosition()]"
                :to="{
                path: positionsUriPaths[getTargetPosition()]
            }"
        >
        </opensilex-UriView>
        <opensilex-GeometryView label="component.common.geometry"
                                :value="positions.location.geojson"></opensilex-GeometryView>

        <opensilex-StringView label="Position.x" :value="positions.location.x"></opensilex-StringView>
        <opensilex-StringView label="Position.y" :value="positions.location.y"></opensilex-StringView>
        <opensilex-StringView label="Position.z" :value="positions.location.z"></opensilex-StringView>

        <opensilex-TextView v-if="hasPosition()" label="Position.textual-position"
                            :value="positions.location.text"></opensilex-TextView>

    </div>

</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {MoveDetailsDTO} from "opensilex-core/model/moveDetailsDTO";

@Component
export default class PositionsView extends Vue {

    @Prop({default: () => {}})
    positions: MoveDetailsDTO;
    @Prop()
    positionsUriLabels;
    @Prop()
    positionsUriPaths;

    hasPosition(): boolean {
        return this.positions && this.positions.location;
    }

    getTargetPosition() {
        return this.positions.targets[0];
    }
}
</script>

