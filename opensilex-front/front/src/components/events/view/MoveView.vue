<template>
    <div v-if="event">

        <br>
        <p class="h5">{{$t("Move.location")}} </p>
        <hr/>

        <opensilex-StringView label="Position.from" :value="event.from ? event.from.name : '' "></opensilex-StringView>
        <opensilex-StringView label="Position.to" :value="event.to ? event.to.name : '' "></opensilex-StringView>

        <div  v-if="hasPosition(event)">
            <br>
            <p class="h5"> {{ $t("Position.title") }}</p>
            <hr/>
            <opensilex-PositionsView :positions="event.targets_positions">
            </opensilex-PositionsView>
        </div>

    </div>

</template>

<script lang="ts">
    import {Component, Prop} from "vue-property-decorator";
    import Vue from "vue";
    import PositionsView from "../../positions/view/PositionsView.vue";
    import {MoveDetailsDTO} from "opensilex-core/model/moveDetailsDTO";

    @Component
    export default class MoveView extends Vue {

        @Prop({default: () => PositionsView.getEmptyForm })
        event: MoveDetailsDTO;

        created() {

        }

        static getEmptyForm(): MoveDetailsDTO {
            return {
                uri: undefined,
                rdf_type: undefined,
                rdf_type_name: undefined,
                start: undefined,
                end: undefined,
                targets: [],
                description: undefined,
                author: undefined,
                is_instant: true,

                // move specific properties
                from: undefined,
                to: undefined,

                // move position(s)
              targets_positions: PositionsView.getEmptyForm()
            };
        }

        hasPosition(event) : boolean{
            return event && event.targets_positions && event.targets_positions.length > 0;
        }

    }



</script>

