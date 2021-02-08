<template>
    <b-card v-if="selected && selected.uri">
        <template v-slot:header>
            <h3>
                <opensilex-Icon icon="ik#ik-clipboard" />
                {{$t("component.common.details-label")}}
            </h3>
        </template>
        <div>
            <!-- URI -->
            <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
            <!-- Name -->
            <opensilex-TextView label="component.common.name" :value="selected.name"></opensilex-TextView>
            <!-- Description -->
            <opensilex-TextView :value="selected.description" label="component.common.description"></opensilex-TextView>
        </div>
    </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VariableStructureDetails extends Vue {
    $opensilex: any;

    @Prop()
    selected;
}
</script>

<style scoped lang="scss">
</style>

