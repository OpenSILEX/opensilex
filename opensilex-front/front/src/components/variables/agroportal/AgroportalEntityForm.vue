<template>
  <opensilex-AgroportalCreateForm
      ref="createForm"
      @onCreate="$emit('onCreate', $event)"
      @onUpdate="$emit('onUpdate', $event)"
      ontologiesConfig="entityOntologies"
      searchPlaceholder="AgroportalEntityForm.name-placeholder"
      createTitle="AgroportalEntityForm.add"
      editTitle="AgroportalEntityForm.edit"
      :createMethod="service.createEntity.bind(service)"
      :updateMethod="service.updateEntity.bind(service)"
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
export default class AgroportalEntityForm extends Vue implements BaseExternalReferencesForm {
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
  AgroportalEntityForm:
    add: Add entity
    edit: Edit entity
    name-placeholder: Plant
fr:
  AgroportalEntityForm:
    add: Ajouter une entité
    edit: Modifier une entité
    name-placeholder: Plant
</i18n>