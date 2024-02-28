<template>
  <opensilex-SelectForm
      :label="label"
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
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";

@Component
export default class BasicUriAndLabelDTOSelectForm extends Vue {

  //#region Props
  @PropSync("selectedURI")
  private selectedDtoUri: string

  @Prop({required: true, default: () => []})
  private readonly DTOs: Array<NamedResourceDTO>

  @Prop()
  private readonly label: string
  //#endregion

  //#region Computed
  private get DTOsAsOptions(): Array<SelectableItem> {
    return this.DTOs.map((dto: NamedResourceDTO) => {
      return {
        id: dto.uri,
        label: dto.name
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