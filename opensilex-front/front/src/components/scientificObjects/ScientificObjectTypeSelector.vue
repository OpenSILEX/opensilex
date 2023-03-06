<template>
  <opensilex-SelectForm
    :key="lang"
    :label="label"
    :selected.sync="typesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadTypes"
    placeholder="ScientificObjectTypeSelector.placeholder"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Watch } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
// @ts-ignore
import { ListItemDTO } from "opensilex-core/index";
// @ts-ignore
import { ScientificObjectsService } from "opensilex-core/index";

@Component
export default class ScientificObjectTypeSelector extends Vue {
  $opensilex: any;
  service: ScientificObjectsService;

  @PropSync("types")
  typesURI;

  @Prop()
  experimentURI;

  @Prop()
  label;

  @Prop()
  multiple;

  get lang() {
    return this.$store.getters.language;
  }

  loadTypes(typesURI) {
    if (
      this.experimentURI == null ||
      this.experimentURI == "" ||
      this.experimentURI == undefined
    ) {
      return this.$opensilex
        .getService("opensilex.ScientificObjectsService")
        .getUsedTypes()
        .then((http: HttpResponse<OpenSilexResponse<Array<ListItemDTO>>>) => {
          return http.response.result;
        });
    } else {
      return this.$opensilex
        .getService("opensilex.ScientificObjectsService")
        .getUsedTypes(this.experimentURI)
        .then((http: HttpResponse<OpenSilexResponse<Array<ListItemDTO>>>) => {
          return http.response.result;
        });
    }
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ScientificObjectTypeSelector:
    placeholder: Select a type

fr:
  ScientificObjectTypeSelector:
    placeholder: Sélectionner un type
</i18n>