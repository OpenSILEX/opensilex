<template>
  <opensilex-AgroportalCreateForm
      ref="createForm"
      @onCreate="$emit('onCreate', $event)"
      @onUpdate="$emit('onUpdate', $event)"
      ontologiesConfig="entityOntologies"
      searchPlaceholder="AgroportalEntityOfInterestForm.name-placeholder"
      createTitle="AgroportalEntityOfInterestForm.add"
      editTitle="AgroportalEntityOfInterestForm.edit"
      :createMethod="service.createInterestEntity.bind(service)"
      :updateMethod="service.updateInterestEntity.bind(service)"
  ></opensilex-AgroportalCreateForm>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {VariablesService} from "opensilex-core/api/variables.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {Ref} from "vue-property-decorator";
import {BaseExternalReferencesForm} from "../../common/external-references/ExternalReferencesTypes";

@Component({})
export default class AgroportalEntityOfInterestForm extends Vue implements BaseExternalReferencesForm {
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

  showEditForm(dto) {
    this.createForm.showEditForm(dto);
  }
}
</script>

<style scoped lang="scss">

</style>

<i18n>
en:
  AgroportalEntityOfInterestForm:
    add: Add entity of interest
    edit: Edit entity of interest
    name-placeholder: Canopy
fr:
  AgroportalEntityOfInterestForm:
    add: Ajouter une entité d'intérêt
    edit: Modifier une entité d'intérêt
    name-placeholder: Canopée
</i18n>