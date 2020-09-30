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
        </div>
    </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class EntityDetails extends Vue {
    $opensilex: any;

    @Prop()
    selected;
}
</script>

<style scoped lang="scss">
</style>

