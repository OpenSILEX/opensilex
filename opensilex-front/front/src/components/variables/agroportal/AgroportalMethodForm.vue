<template>
  <opensilex-AgroportalCreateForm
      ref="createForm"
      @onCreate="$emit('onCreate', $event)"
      @onUpdate="$emit('onUpdate', $event)"
      ontologiesConfig="entityOntologies"
      searchPlaceholder="AgroportalMethodForm.name-placeholder"
      createTitle="AgroportalMethodForm.add"
      editTitle="AgroportalMethodForm.edit"
      :createMethod="service.createMethod.bind(service)"
      :updateMethod="service.updateMethod.bind(service)"
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
export default class AgroportalMethodForm extends Vue implements BaseExternalReferencesForm {
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
  AgroportalMethodForm:
    add: Add method
    edit: Edit method
    name-placeholder: Image analysis
fr:
  AgroportalMethodForm:
    add: Ajouter une méthode
    edit: Modifier une méthode
    name-placeholder: Analyse d'image
</i18n>