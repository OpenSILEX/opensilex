<template>
  <opensilex-AgroportalCreateForm
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

@Component({})
export default class AgroportalEntityForm extends Vue {
  $opensilex: OpenSilexVuePlugin
  service: VariablesService;

  created() {
    this.service = this.$opensilex.getService("opensilex-core.VariablesService");
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
</i18n>