<template>
  <opensilex-SelectForm
    :key="lang"
    ref="selectForm"
    :label="label"
    :selected.sync="germplasmAttributeSelected"
    :multiple="false"
    :required="required"
    :optionsLoadingMethod="loadOptions"
    :conversionMethod="convertGermplasmAttribute"
    :placeholder="placeholder"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { SecurityService } from "opensilex-security/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
// @ts-ignore
import { GermplasmGetSingleDTO } from "opensilex-core/index";

@Component
export default class GermplasmAttributesSelector extends Vue {
  $opensilex: any;
  $store: any;

  service: SecurityService;

  @PropSync("germplasmAttribute")
  germplasmAttributeSelected;

  @Prop({
    default: "GermplasmAttributesSelector.title",
  })
  label;

  @Prop({
    default: "GermplasmAttributesSelector.placeholder",
  })
  placeholder;

  @Prop()
  required;
 
  get lang() {
    return this.$store.getters.language;
  }

  @Ref("selectForm") readonly selectForm!: any;
 
  loadOptions(query, page, pageSize) { 
    return this.$opensilex
      .getService("opensilex.GermplasmService")
      .getGermplasmAttributes()
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<GermplasmGetSingleDTO>>>) =>
          http.response.result
      );
  }

  convertGermplasmAttribute(germplasmAttribute) { 
    return {
      id: germplasmAttribute,
      label: germplasmAttribute,
    };
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  GermplasmAttributesSelector:
    title: Attribut
    placeholder: Select an germplasm attribute

fr:
  GermplasmAttributesSelector:
    title: Attribute
    placeholder: Sélectionner l'attribut d'une resource génétique

</i18n>