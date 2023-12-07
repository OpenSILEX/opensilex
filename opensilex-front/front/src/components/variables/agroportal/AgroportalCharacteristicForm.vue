<template>
  <opensilex-AgroportalCreateForm
      ref="createForm"
      @onCreate="$emit('onCreate', $event)"
      @onUpdate="$emit('onUpdate', $event)"
      ontologiesConfig="entityOntologies"
      searchPlaceholder="AgroportalCharacteristicForm.name-placeholder"
      createTitle="AgroportalCharacteristicForm.add"
      editTitle="AgroportalCharacteristicForm.edit"
      :createMethod="service.createCharacteristic.bind(service)"
      :updateMethod="service.updateCharacteristic.bind(service)"
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
export default class AgroportalCharacteristicForm extends Vue implements BaseExternalReferencesForm {
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
  AgroportalCharacteristicForm:
    add: Add characteristic
    edit: Edit characteristic
    name-placeholder: Height
fr:
  AgroportalCharacteristicForm:
    add: Ajouter une caractéristique
    edit: Modifier une caractéristique
    name-placeholder: Hauteur
</i18n>