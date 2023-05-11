<template>
  <b-modal ref="modalRef" size="m" :static="true" @close="reset">

    <!-- TODO: Change Title?Icon?? -->
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i>
      {{ $t('component.project.filter-description') }}
    </template>

    <template v-slot:modal-footer>
      <button
          type="button"
          class="btn btn-secondary"
          v-on:click="hide(false)"
      >{{ $t('component.common.close') }}</button>

      <button
          type="button"
          class="btn greenThemeColor"
          v-on:click="hide(true)"
      >{{ $t('component.common.validateSelection') }}</button>
    </template>

    <div>
      <opensilex-ScientificObjectPropertiesSelector
          :props.sync="form.props"
      ></opensilex-ScientificObjectPropertiesSelector>
      <!--TODO: (Add component VariableModalList - standby)-->
      <!-- choose the variables to export - modal
    <opensilex-VariableModalList
        ref="variableModal"
        :objects="exportOS"
        :experiment="[experiment]"
        :withAssociatedData="true"
        :isModalSearch="true"
        :multiple="true"
        @onValidate="addVariableData"
    ></opensilex-VariableModalList>-->
    </div>

  </b-modal>
</template>

<script lang="ts">

import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ExportShapeModalList extends Vue {
  @Ref("modalRef") readonly modalRef!: any;

  form = {
    props : undefined
  };
  showInstruction: boolean = false;
  show() {
    this.modalRef.show();
  }
  hide(validate: boolean) {
    this.modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.form);
    } else {
      this.$emit("onClose");
    }
    this.reset();
  }
  reset(){
    this.form = {
      props : undefined
    };
  }
}
</script>

<style scoped>

</style>

<i18n>

</i18n>