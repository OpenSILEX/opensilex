<template>
  <b-modal ref="modalRef" size="m" :static="true" @close="reset">

    <template v-slot:modal-title>
      <opensilex-Icon icon="fa#list" />
      {{ $t('title') }}
    </template>

    <template v-slot:modal-footer>
      <button
          type="button"
          class="btn btn-secondary"
          v-on:click="hide(false, true)"
      >{{ $t('component.common.close') }}</button>

      <button
          type="button"
          class="btn greenThemeColor"
          v-on:click="hide(true, true)"
      >Shapefile</button>

      <button
          type="button"
          class="btn greenThemeColor"
          v-on:click="hide(true, false)"
      >GeoJson</button>
    </template>

    <!-- help button -->
    <opensilex-HelpButton
        class="helpButton"
        label="component.common.help-button"
        @click="showInstruction=true"
    ></opensilex-HelpButton>
    <div>
      <br>
      <b-alert v-model="showInstruction" dismissible>
        <p v-html="$t('instruction')"></p>
      </b-alert>
    </div>

    <!-- SO section -->
    <h6 class="mb-3">
      <strong>{{ $t("component.menu.scientificObjects") }}</strong>
    </h6>

    <div>
      <opensilex-ItemsPropertiesSelector
          :props.sync="form.SO.props"
          :type="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
          :class="isDisabled.SO ? 'disabled-input' : ''"
      ></opensilex-ItemsPropertiesSelector>
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

    <!-- Devices section -->
    <h6 class="mb-3">
      <strong>{{ $t("component.menu.devices") }}</strong>
    </h6>

    <opensilex-ItemsPropertiesSelector
        :props.sync="form.devices.props"
        :type="$opensilex.Oeso.DEVICE_TYPE_URI"
        :class="isDisabled.devices ? 'disabled-input' : ''"
    ></opensilex-ItemsPropertiesSelector>

    <!-- Area section -->
    <h6 class="mb-3">
      <strong>{{ $t("areas") }}</strong>
    </h6>

    <opensilex-ItemsPropertiesSelector
        :props.sync="form.areas.props"
        :type="$opensilex.Oeso.AREA_TYPE_URI"
        :class="isDisabled.areas ? 'disabled-input' : ''"
    ></opensilex-ItemsPropertiesSelector>

  </b-modal>
</template>

<script lang="ts">

import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class ExportShapeModalList extends Vue {
  @Ref("modalRef") readonly modalRef!: any;
  $opensilex: OpenSilexVuePlugin;
  form = {
    SO:{
      props : undefined,
    },
    devices:{
      props : undefined,
    },
    areas:{
      props : undefined,
    },
    format: undefined
  };
  showInstruction: boolean = false;
  isDisabled = {SO:false, devices: false, areas: false};

  show(isDisabled) {
    //disabled input if there is no items to export
    this.isDisabled = isDisabled;
    this.$emit('preSelection');
    this.modalRef.show();
  }

  hide(validate: boolean, shapefile: boolean) {
    this.modalRef.hide();

    if (validate) {
      if(shapefile){
        this.form.format = "shp";
        this.$emit("onValidate", this.form);
      } else {
        this.form.format = "geojson";
        this.$emit("onValidate", this.form);
      }
    } else {
      this.$emit("onClose");
    }
    this.reset();
  }

  reset(){
    this.form = {
      SO:{
        props : undefined,
      },
      devices:{
        props : undefined,
      },
      areas:{
        props : undefined,
      },
      format: undefined
    };
  }
}
</script>

<style scoped>

.disabled-input, ::v-deep .disabled-input .vue-treeselect div,::v-deep .disabled-input .vue-treeselect span {
  pointer-events: none;
  color: grey;
}

</style>

<i18n>
en:
  title: Data export
  areas: Areas
  instruction: Export the selected items on the map. Without prior selection, visible items in the "Map Panel" are exported. The export is limited to 10000 objects.<br> By default, the names and URIs of the element and its type are exported. The choice of exported properties is only activated if one or more elements of its type (scientific objects, devices, areas) are selected for export.
fr:
  title: Export des données
  areas: Zones
  instruction: Exporte les éléments sélectionnés sur la carte. Sans sélection préalable, les éléments visibles dans le menu "Données" sont exportés. L'export est limité à 10000 objets.<br> Par défault, le nom et l'URI de l'élément et de son type sont exportés. Le choix des propriétés exportées est activé uniquement si un ou plusieurs éléments de son genre sont sélectionnés pour être exporter.
</i18n>