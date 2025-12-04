<template>
  <ValidationObserver ref="validatorRef">
    <!-- URI -->
    <opensilex-UriForm
        :uri.sync="form.uri"
        label="component.group.group-uri"
        helpMessage="component.common.uri-help-message"
        :editMode="editMode"
        :generated.sync="uriGenerated">
    </opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
        :value.sync="form.name"
        label="component.common.name"
        type="text"
        :required="true"
        placeholder="component.group.form-name-placeholder">
    </opensilex-InputForm>

    <!-- Description -->
    <opensilex-TextAreaForm
        :value.sync="form.description"
        label="component.common.description"
        :required="false"
        placeholder="component.group.form-description-placeholder">
    </opensilex-TextAreaForm>

    <!-- geneticResource -->
    <opensilex-GeneticResourceSelectorWithFilter
        ref="geneticResourceSelector"
        :geneticResourcesUris.sync="form.geneticResource_list"
        :geneticResources="geneticResourcesWithLabels"
        :editMode="this.editMode"
        @hideSelector='$emit("hideSelector")'
        @shownSelector='$emit("shownSelector")'
    ></opensilex-GeneticResourceSelectorWithFilter>
  </ValidationObserver>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import GeneticResourceSelectorWithFilter from '../geneticResource/GeneticResourceSelectorWithFilter.vue';
import {GeneticResourceGroupUpdateDTO} from "../../../../../opensilex-core/front/src/lib";
import {SelectableItem} from "../common/forms/FormSelector.vue";
import {GeneticResourceGetAllDTO} from "opensilex-core/model/geneticResourceGetAllDTO";


@Component
export default class GroupGeneticResourceForm extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;

  @Prop()
  editMode;

  @Prop({ default: true })
  uriGenerated;

  @Ref("validatorRef") readonly validatorRef!: any;

  @Ref("geneticResourceSelector") readonly geneticResourceSelector!: GeneticResourceSelectorWithFilter;

  geneticResourcesWithLabels : Array<GeneticResourceGetAllDTO>;

  setSelectorsToFirstTimeOpenAndSetLabels(geneticResourcesWithLabels){
    this.geneticResourceSelector.setGeneticResourceSelectorToFirstTimeOpen();
    this.geneticResourcesWithLabels = geneticResourcesWithLabels;
  }

  get user() {
    return this.$store.state.user;
  }

  @Prop()
  form: GeneticResourceGroupUpdateDTO;

  static getEmptyForm(): GeneticResourceGroupUpdateDTO {
    return {
      uri: null,
      name: null,
      description: null,
      geneticResource_list: [],
    };
  }

  getEmptyForm(){
    return GroupGeneticResourceForm.getEmptyForm();
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  GroupGeneticResourceForm:
    add: Add genetic resource group
    edit: Edit genetic resource group
fr:
  GroupGeneticResourceForm:
    add: Ajouter un groupe de ressources génétiques
    edit: Éditer un groupe de ressources génétiques
</i18n>