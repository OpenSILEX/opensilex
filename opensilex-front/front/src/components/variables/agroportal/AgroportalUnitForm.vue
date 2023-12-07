<template>
  <opensilex-AgroportalCreateForm
      ref="createForm"
      @onCreate="$emit('onCreate', $event)"
      @onUpdate="$emit('onUpdate', $event)"
      ontologiesConfig="unitOntologies"
      searchPlaceholder="AgroportalUnitForm.name-placeholder"
      createTitle="AgroportalUnitForm.add"
      editTitle="AgroportalUnitForm.edit"
      :createMethod="service.createUnit.bind(service)"
      :updateMethod="service.updateUnit.bind(service)"
  ></opensilex-AgroportalCreateForm>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {VariablesService} from "opensilex-core/api/variables.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {Ref} from "vue-property-decorator";
import {BaseExternalReferencesForm, BaseExternalReferencesDTO} from "../../common/external-references/ExternalReferencesTypes";

@Component({})
export default class AgroportalUnitForm extends Vue implements BaseExternalReferencesForm {
  $opensilex: OpenSilexVuePlugin
  service: VariablesService;

  @Ref("createForm")
  createForm: BaseExternalReferencesForm;

  created() {
    this.service = this.$opensilex.getService("opensilex-core.VariablesService");
  }

  showCreateForm() {
    this.createForm.showCreateForm();
  }

  showEditForm(dto: BaseExternalReferencesDTO) {
    this.createForm.showEditForm(dto);
  }
}
</script>

<style scoped lang="scss">

</style>

<i18n>
en:
  AgroportalUnitForm:
    add: Add unit
    edit: Edit unit
    name-placeholder: Centimeter
fr:
  AgroportalUnitForm:
    add: Ajouter une unité
    edit: Modifier une unité
    name-placeholder: Centimètre
</i18n>