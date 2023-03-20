<template>
  <b-modal
      ref="modal"
      size="md"
      centered
      hide-footer
  >
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              {{ $t("FavoritesHelp.title") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>

    {{ $t("FavoritesHelp.body")}}
    <opensilex-Icon icon="fa#star"/>

      <div class="favoritesHelpOkButton">
    <button
      type="button"
      class="btn greenThemeColor"
      v-on:click="hide()"
    >
      {{ $t('component.common.ok') }}
    </button>
    </div>
  </b-modal>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class FavoritesHelp extends Vue {
  $opensilex: OpenSilexVuePlugin;
  @Ref("modal") readonly modal!: any;

  show() {
    this.modal.show();
  }

  hide() {
    this.modal.hide();
  }
}
</script>

<style scoped lang="css">
.favoritesHelpOkButton {
  float: right;
  margin-top: 20px
}
</style>

<i18n>
en:
  FavoritesHelp:
    title: About favorites
    body: You can add a scientific object, an experiment or a device as favorite by going to its description and clicking on the icon

fr:
  FavoritesHelp:
    title: A propos des favoris
    body: Vous pouvez ajouter un objet scientifique, une experimentation ou un dispositif comme favoris en vous rendant sur sa description et en cliquant sur l'icone 

</i18n>
