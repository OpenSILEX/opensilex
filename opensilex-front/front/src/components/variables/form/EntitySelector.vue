<template>
  <opensilex-FormSelector
    ref="formSelector"
    :label="label"
    :selected.sync="entityURI"
    :multiple="multiple"
    :searchMethod="searchEntities"
    :itemLoadingMethod="loadEntities"
    :placeholder="placeholder"
    :conversionMethod="conversionMethod"
    :actionHandler="actionHandler"
    :required="required"
    :helpMessage="helpMessage"
    noResultsText="component.entity.form.selector.filter-search-no-result"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {EntityGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import FormSelector from "../../common/forms/FormSelector.vue";

@Component
export default class EntitySelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  pageSize = 10;
  page = 0;

  @PropSync("selected")
  entityURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  required;

  @Prop()
  actionHandler;

  @Prop()
  helpMessage;

  @Prop()
  conversionMethod;

  @Prop()
  sharedResourceInstance?: string;

  @Ref("formSelector") readonly formSelector!: FormSelector;

  setSelectedNode(node: { id: string; label: string }) {
    this.tutorialLabels[node.id] = node.label;
    this.formSelector.select(node);
  }

  private tutorialLabels: Record<string, string> = {};


  refresh() {
    this.formSelector.refresh();
  }

  @Watch("sharedResourceInstance")
  onSriChange() {
    this.formSelector.refresh()
  }

  get placeholder() {
    return this.multiple
      ? "component.entity.form.selector.placeholder-multiple"
      : "component.entity.form.selector.placeholder";
  }

  loadEntities(entities): Promise<Array<EntityGetDTO>> {
    if (Array.isArray(entities) && entities.length === 1 && String(entities[0]).startsWith("__tutorial__:")) {
      const uri = String(entities[0]);
      return Promise.resolve([{ uri, name: this.tutorialLabels[uri] || "" } as any]);
    }

    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
      .getEntitiesByURIs(entities, this.sharedResourceInstance)
      .then(http => http.response.result)
      .catch(this.$opensilex.errorHandler);
  }

  searchEntities(name, page, pageSize): Promise<HttpResponse<OpenSilexResponse<Array<EntityGetDTO>>>> {
    return this.$opensilex.getService<VariablesService>("opensilex.VariablesService")
    .searchEntities(name, ["name=asc"], page, pageSize, this.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<Array<EntityGetDTO>>>) => {
        return http;
    });
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
