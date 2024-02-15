<template>
  <opensilex-SelectForm
      label="VariableForm.time-interval"
      :selected.sync="selectedDtoUri"
      :options="DTOsAsOptions"
      placeholder="VariableForm.time-interval-placeholder"
      helpMessage="VariableForm.time-interval-help"
      @keyup.enter.native="emitHandlingEnterKey"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import {SelectableItem} from "./SelectForm.vue";
import {BasicURIAndLabelDTO} from "opensilex-core/model/basicURIAndLabelDTO";

@Component
export default class BasicUriAndLabelDTOSelectForm extends Vue {
  //#endregion

  //#region Props
  @PropSync("selectedURI")
  private selectedDtoUri: string

  @Prop({required: true, default: () => []})
  private readonly DTOs: Array<BasicURIAndLabelDTO>
  //#endregion

  //#region Computed
  private get DTOsAsOptions(): Array<SelectableItem> {
    return this.DTOs.map((dto: BasicURIAndLabelDTO) => {
      return {
        id: dto.uri,
        label: dto.label
      }
    })
  }

  //#endregion

  //#region Events
  private emitHandlingEnterKey() {
    this.$emit("handlingEnterKey")
  }

  //#endregion

}
</script>