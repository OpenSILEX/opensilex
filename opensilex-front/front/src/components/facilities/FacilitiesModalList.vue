<!--
  - ******************************************************************************
  -                         FacilitiesModalList.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 12/06/2024 15:01
  - Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
  - ******************************************************************************
  -->
<template>
  <div>
    <div
        v-if="facilities && facilities.length == 0"
        id="no-facilities"
    >
      {{ $t("FacilitiesModalList.no_facilities") }}
    </div>

    <b-button
        v-else-if="facilities.length == 1"
        id="name"
        @click="showPopup"
        variant="link">
      {{ facilities[0].name }}
    </b-button>

    <b-button
        v-else
        id="name"
        @click="showPopup"
        variant="link">
      {{ facilities[0].name + " ..." }}
      <span class="badge badge-pill greenThemeColor">{{ "+" + (facilities.length - 1) }}</span>
    </b-button>

    <b-modal
        ref="popup"
        :title="$t('FacilitiesModalList.title', [currentSite.name])"
        hide-footer
        centered>
      <div id="facility-view">
        <opensilex-FacilitiesView
            :withActions="true"
            @onUpdate="emitOnCRUD"
            @onCreate="emitOnCRUD"
            @onDelete="emitOnCRUD"
            :facilities="facilities"
            :site="currentSite"
            :isSelectable="false"
            isScrollable
        ></opensilex-FacilitiesView>
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

@Component
export default class FacilitiesModalList extends Vue {
  //
  //#region Props
  @Prop({required: true})
  facilities: Array<NamedResourceDTO>;

  @Prop()
  currentSite: NamedResourceDTO;
  //#endregion

  //#region Refs
  @Ref("popup") readonly popup!: BModal;
  //#endregion

  //#region Events
  emitOnCRUD() {
    this.$emit("onCRUD");
  }

  //#endregion

  //#region Private methods
  private showPopup() {
    this.popup.show()
  }

  //#endregion
}
</script>

<!--not scoped to catch the '.modal-content' class of boostrap-vue-->
<style lang="scss">
#name, #no-facilities {
  padding: 0;
}

#name {
  color: #007bff;
}

#facility-view {
  padding-top: 5%;
}

// class from bootstrap-vue
.modal-content {
  width: fit-content;
}

</style>

<i18n>
en:
  FacilitiesModalList:
    title: "Facilities of {0}"
    no_facilities: "No facilities"
fr:
  FacilitiesModalList:
    title: "Installations de {0}"
    no_facilities: "Aucune installation"
</i18n>

