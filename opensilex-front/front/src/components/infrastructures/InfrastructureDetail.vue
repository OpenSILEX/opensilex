<template>
  <b-card v-if="selected">
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
      <opensilex-StringView label="component.common.name" :value="selected.name"></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView :type="selected.rdf_type" :typeLabel="selected.rdf_type_name"></opensilex-TypeView>
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class InfrastructureDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;
}
</script>

<style scoped lang="scss">
</style>

