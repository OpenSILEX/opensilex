<template>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="entityURI"
    :multiple="multiple"
    :searchMethod="searchEntities"
    :itemLoadingMethod="loadEntities"
    :conversionMethod="entityToSelectNode"
    :clearable="clearable"
    :placeholder="placeholder"
    noResultsText="component.entity.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
// @ts-ignore
import {EntityGetDTO} from "opensilex-core/index";

@Component
export default class EntitySelector extends Vue {
  $opensilex: any;

  @PropSync("entity")
  entityURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  clearable;

  get placeholder() {
    return this.multiple
      ? "component.entity.form.selector.placeholder-multiple"
      : "component.entity.form.selector.placeholder";
  }

  loadEntities(entities) {
    return this.$opensilex.getService("opensilex.VariablesService")
      .getEntitiesByURIs(entities)
      .then((http: HttpResponse<OpenSilexResponse<EntityGetDTO>>) => {
        return http.response.result;
      })
      .catch(this.$opensilex.errorHandler); 
  }

  searchEntities(name) {
    return this.$opensilex.getService("opensilex.VariablesService")
    .searchEntities(name, ["name=asc"], 0, 10)    
    .then((http: HttpResponse<OpenSilexResponse<Array<EntityGetDTO>>>) => {
        return http;
    });
  }

  entityToSelectNode(dto) {
    return {
      id: dto.uri,
      label: dto.name,
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
  component: 
    entity: 
        form:
          selector:
            placeholder : Select one entity
            placeholder-multiple : Select one or more entities
            filter-search-no-result : No entities found
    
            
fr:
  component: 
    entity: 
        form: 
          selector:
            placeholder : Sélectionner une entité
            placeholder-multiple : Sélectionner une ou plusieurs entités
            filter-search-no-result : Aucune entité trouvée

</i18n>
