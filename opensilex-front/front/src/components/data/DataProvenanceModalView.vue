<template>
  <b-modal v-model="modalShow" size="lg" centered :title="$t('DataProvenanceModalView.title')">
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              <opensilex-Icon icon="fa#eye" />
            </h4>
          </i>
        </b-col>
        <b-col cols="2">
          <!-- Emulate built in modal header close button action -->
          <button type="button" class="close" @click="hide" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </b-col>
      </b-row>
    </template>
    <template>
      <h3>{{ $t("DataProvenanceModalView.data") }}</h3>
      <pre>{{ data }}</pre>
      <h3>Provenance</h3>
      <pre>{{ provenance }}</pre>
    </template>
  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Prop } from "vue-property-decorator";

@Component
export default class DataProvenanceModalView extends Vue {
  modalShow: boolean = false;

  info: any = null;
  data: any = null;
  provenance: any = null;
  setProvenance(value) {
    this.data = JSON.stringify(value.data, null, 2);
    this.provenance = JSON.stringify(value.provenance, null, 2);
  }

  show() {
    this.modalShow = true;
  }

  hide() {
    this.modalShow = false;
  }
}
</script>

<i18n>
  fr: 
    DataProvenanceModalView:
      data : Donnnée
      

  en: 
    DataProvenanceModalView:
      data : Data
</i18n>