<template>
    <b-card v-if="selected && selected.uri">
        <template v-slot:header>
            <h3>
                <opensilex-Icon icon="ik#ik-clipboard" />
                {{$t("component.infrastructure.detail")}}
            </h3>
        </template>
        <div>
            <!-- URI -->
            <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
            <!-- Name -->
            <opensilex-StringView label="component.common.name" :value="selected.name"></opensilex-StringView>
            <!-- Description -->
            <opensilex-TextView :value="selected.comment" label="component.common.description"></opensilex-TextView>
            <!-- symbol -->
            <opensilex-StringView label="UnitForm.symbol" :value="selected.symbol"></opensilex-StringView>
            <!-- alternative symbol -->
            <opensilex-StringView label="UnitForm.alternative-symbol" :value="selected.alternativeSymbol"></opensilex-StringView>

        </div>
    </b-card>
</template>

<script lang="ts">
import { Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {UnitGetDTO} from "opensilex-core/model/unitGetDTO";


@Component
export default class UnitDetails extends Vue {
    $opensilex: any;

    @Prop()
    selected : UnitGetDTO;
}
</script>

<style scoped lang="scss">
</style>
