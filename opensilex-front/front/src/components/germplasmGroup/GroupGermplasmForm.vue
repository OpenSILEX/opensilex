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

    <!-- germplasm -->
    <opensilex-GermplasmSelectorWithFilter
        ref="germplasmSelector"
        :germplasmsUris.sync="form.germplasm_list"
        :germplasms="germplasmsWithLabels"
        :editMode="this.editMode"
        @hideSelector='$emit("hideSelector")'
        @shownSelector='$emit("shownSelector")'
    ></opensilex-GermplasmSelectorWithFilter>


  </ValidationObserver>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import GermplasmSelectorWithFilter from '../germplasm/GermplasmSelectorWithFilter.vue';
import {GermplasmGroupUpdateDTO} from "../../../../../opensilex-core/front/src/lib";
import {GermplasmGetAllDTO} from "opensilex-core/model/germplasmGetAllDTO";


@Component
export default class GroupGermplasmForm extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;

  @Prop()
  editMode;

  @Prop({ default: true })
  uriGenerated;

  @Ref("validatorRef") readonly validatorRef!: any;

  @Ref("germplasmSelector") readonly germplasmSelector!: GermplasmSelectorWithFilter;

  germplasmsWithLabels : Array<GermplasmGetAllDTO>;

  setSelectorsToFirstTimeOpenAndSetLabels(germplasmsWithLabels){
    this.germplasmSelector.setGermplasmSelectorToFirstTimeOpen();
    this.germplasmsWithLabels = germplasmsWithLabels;
  }

  get user() {
    return this.$store.state.user;
  }

  @Prop()
  form: GermplasmGroupUpdateDTO;

  static getEmptyForm(): GermplasmGroupUpdateDTO {
    return {
      uri: null,
      name: null,
      description: null,
      germplasm_list: [],
    };
  }

  getEmptyForm(){
    return GroupGermplasmForm.getEmptyForm();
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  GroupGermplasmForm:
    add: Add germplasm group
    edit: Edit germplasm group
fr:
  GroupGermplasmForm:
    add: Ajouter un groupe de ressources génétiques
    edit: Éditer un groupe de ressources génétiques
</i18n>