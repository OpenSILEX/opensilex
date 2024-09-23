<!--
  - ******************************************************************************
  -                         ExperimentsModalList.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 21/08/2024 10:22
  - Contact: yvan.roux@inrae.fr
  - ******************************************************************************
  -->
<template>
  <div>
    <div
        v-if="!experiments || experiments.length == 0"
        id="no-experiment"
    >
      {{ $t("ExperimentsModalList.no_experiments") }}
    </div>

    <b-button
        v-else
        id="number"
        @click="showPopup"
        variant="link">
      {{ experiments.length }}
    </b-button>

    <b-modal
        ref="popup"
        :title="$t('ExperimentsModalList.title', [currentFacility.name])"
        hide-footer
        centered>
      <div id="facility-view">
        <opensilex-ExperimentSimpleList
            :experimentList="experiments"
        />
      </div>

    </b-modal>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop, Ref} from "vue-property-decorator";
import {BModal} from "bootstrap-vue";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {ExperimentGetListDTO} from "opensilex-core/model/experimentGetListDTO";

@Component
export default class ExperimentsModalList extends Vue {
  //
  //#region Props
  @Prop({required: true})
  experiments: Array<ExperimentGetListDTO>;

  @Prop()
  currentFacility: NamedResourceDTO;
  //#endregion

  //#region Refs
  @Ref("popup") readonly popup!: BModal;
  //#endregion

  //#region Private methods
  private showPopup() {
    this.popup.show()
  }

  //#endregion
}
</script>

<style scoped lang="scss">
#number, #no-experiment {
  padding: 0;
}

#number {
  color: #007bff;
}

#facility-view {
  padding-top: 5%;
}

</style>

<i18n>
en:
  ExperimentsModalList:
    title: "Current experiments hosted in {0}"
    no_experiments: "No current experiments"
fr:
  ExperimentsModalList:
    title: "Expériences en cours hébergées dans {0}"
    no_experiments: "Pas d'expériences en cours"
</i18n>