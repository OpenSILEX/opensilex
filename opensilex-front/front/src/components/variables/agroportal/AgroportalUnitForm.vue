<template>
  <opensilex-AgroportalCreateForm
      ref="createForm"
      :requireCreate="true"
      @onCreate="$emit('onCreate', $event)"
      @onUpdate="$emit('onUpdate', $event)"
      ontologiesConfig="unitOntologies"
      searchPlaceholder="AgroportalUnitForm.name-placeholder"
      createTitle="AgroportalUnitForm.add"
      editTitle="AgroportalUnitForm.edit"
      :createMethod="service.createUnit.bind(service)"
      :updateMethod="service.updateUnit.bind(service)"
      :emptyForm="emptyForm"
  >
    <template v-slot:createAdditionalFields="scope">
      <!-- Symbol -->
      <opensilex-InputForm
          :value.sync="scope.form.symbol"
          label="AgroportalUnitForm.symbol"
          type="text"
          :required="true"
          placeholder="AgroportalUnitForm.symbol-placeholder"
      ></opensilex-InputForm>

      <!-- Alternative symbol -->
      <opensilex-InputForm
          :value.sync="scope.form.alternative_symbol"
          label="AgroportalUnitForm.alternative-symbol"
          type="text"
          placeholder="AgroportalUnitForm.alternative-symbol-placeholder"
      ></opensilex-InputForm>
    </template>
  </opensilex-AgroportalCreateForm>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {VariablesService} from "opensilex-core/api/variables.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {Ref} from "vue-property-decorator";
import {
  BaseExternalReferencesForm,
  BaseExternalReferencesDTO
} from "../../common/external-references/ExternalReferencesTypes";
import {UnitCreationDTO} from "opensilex-core/model/unitCreationDTO";

@Component({})
export default class AgroportalUnitForm extends Vue implements BaseExternalReferencesForm {
  //#region Plugins and services
  private readonly $opensilex: OpenSilexVuePlugin
  private service: VariablesService;
  //#endregion

  //#region Refs
  @Ref("createForm")
  private readonly createForm: BaseExternalReferencesForm;
  //#endregion

  //#region Data
  private readonly emptyForm: UnitCreationDTO = {
    uri: null,
    name: null,
    description: null,
    symbol: null,
    alternative_symbol: null,
    exact_match: [],
    close_match: [],
    broad_match: [],
    narrow_match: []
  };
  //#endregion

  //#region Hooks
  private created() {
    this.service = this.$opensilex.getService("opensilex-core.VariablesService");
  }
  //#endregion

  //#region Public methods
  public showCreateForm() {
    this.createForm.showCreateForm();
  }

  public showEditForm(dto: BaseExternalReferencesDTO) {
    this.createForm.showEditForm(dto);
  }
  //#endregion
}
</script>

<style scoped lang="scss">

</style>

<i18n>
en:
  AgroportalUnitForm:
    add: Add unit
    edit: Edit unit
    symbol: Symbol
    alternative-symbol: Alternative symbol
    name-placeholder: Meter per second
    symbol-placeholder: m/s
    alternative-symbol-placeholder: m.s⁻¹
fr:
  AgroportalUnitForm:
    add: Ajouter une unité
    edit: Modifier une unité
    symbol: Symbole
    alternative-symbol: Symbole alternatif
    name-placeholder: Mètre par seconde
    symbol-placeholder: m/s
    alternative-symbol-placeholder: m.s⁻¹
</i18n>