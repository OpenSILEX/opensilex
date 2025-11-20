<template>
    <div v-if="event" class="card-body">

        <br>
        <p class="h5">{{$t("Move.location")}} </p>
        <hr/>

        <opensilex-StringView label="MoveView.from" :value="event.location.from ? facilitiesUriLabels[event.location.from] : '' "></opensilex-StringView>
        <opensilex-StringView label="MoveView.to" :value="event.location.to ? facilitiesUriLabels[event.location.to] : '' "></opensilex-StringView>

        <div  v-if="hasPosition(event)">
            <br>
            <p class="h5"> {{ $t("MoveView.positionTitle") }}</p>
            <hr/>
          <opensilex-PositionView
            :positionObject="positionObjectFromLocation"
            :targetUris="event.targets"
            :targetLabelsByUri="targetLabelsByUri"
            :targetUriPathsByUri="targetUriPathsByUri"
          >
          </opensilex-PositionView>
        </div>

    </div>

</template>

<script lang="ts">
    import {Component, Prop} from "vue-property-decorator";
    import Vue from "vue";
    import { MoveDetailsDTO } from 'opensilex-core/index';
    import {PositionFormObject} from "../../positions/view/PositionView.vue";

    @Component
    export default class MoveView extends Vue {

        @Prop()
        event: MoveDetailsDTO;

        @Prop({default: () => {} })
        targetLabelsByUri: {[key : string] : string};

        @Prop({default: () => {} })
        targetUriPathsByUri: {[key : string] : string};

        @Prop({default: () => {} })
        facilitiesUriLabels: {[key : string] : string};

        static getEmptyForm(): MoveDetailsDTO {
            return {
                uri: undefined,
                rdf_type: undefined,
                rdf_type_name: undefined,
                start: undefined,
                end: undefined,
                targets: [],
                description: undefined,
                publisher: undefined,
                is_instant: true,
                //Extra location prop
                location: {},
            };
        }

        hasPosition(event) : boolean{
            return event && event.location
        }

        get positionObjectFromLocation() : PositionFormObject{
          let location = this.event.location;
          return ({
            point: location.geojson,
            x: location.x,
            y: location.y,
            z: location.z,
            text: location.text,
          });
        }

    }
</script>

<i18n>
en:
  MoveView:
    from: From
    to: To
    positionTitle: Position

fr:
  MoveView:
    from: De
    to: Vers
    positionTitle: Position

</i18n>
