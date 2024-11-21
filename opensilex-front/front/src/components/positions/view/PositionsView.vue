<template>
    <div v-if="hasPosition()">
      <opensilex-UriView
        title="Event.targets"
        :uri="getTargetPosition().target"
        :value="positionsUriLabels[getTargetPosition().target]"
        :to="{
                path: positionsUriPaths[getTargetPosition().target]
            }"
      >
      </opensilex-UriView>
        <opensilex-GeometryView label="component.common.geometry"
                                :value="getTargetPosition().position.point"></opensilex-GeometryView>

        <opensilex-StringView label="Position.x" :value="getTargetPosition().position.x"></opensilex-StringView>
        <opensilex-StringView label="Position.y" :value="getTargetPosition().position.y"></opensilex-StringView>
        <opensilex-StringView label="Position.z" :value="getTargetPosition().position.z"></opensilex-StringView>

        <opensilex-TextView v-if="hasPosition()" label="Position.textual-position"
                            :value="getTargetPosition().position.text"></opensilex-TextView>

    </div>

</template>

<script lang="ts">
    import {Component, Prop} from "vue-property-decorator";
    import Vue from "vue";
    import { TargetPositionGetDTO } from 'opensilex-core/index';

    @Component
    export default class PositionsView extends Vue {

        @Prop({default: () => [] })
        positions: Array<TargetPositionGetDTO>;

        @Prop()
        positionsUriLabels;

        @Prop()
        positionsUriPaths;

        hasPosition() : boolean{
            return this.positions && this.positions.length > 0;
        }

        getTargetPosition(): TargetPositionGetDTO{
            return this.positions[0];
        }

        static getEmptyForm(): Array<TargetPositionGetDTO> {
            return [{
                    target: undefined,
                    position: {
                        point: undefined,
                        x: undefined,
                        y: undefined,
                        z: undefined,
                        text: undefined
                    }
            }];
        }

        getCustomCoordinatesTable(){
            let item = this.getTargetPosition();
            return [
                {x: item.position.x, y: item.position.y, z: item.position.z}
            ];
        }

    }



</script>

