<template>
  <b-modal
    ref="modal"
    :title="$t('DeleteByBatchModal.title')"
    no-close-on-backdrop
    no-close-on-esc
  >
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              <opensilex-Icon icon="fa#trash-alt" />
              {{ $t("DeleteByBatchModal.title") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>
    <template>
      <!-- Batch URI -->
      <opensilex-InputForm
        :value.sync="batchUri"
        label="DeleteByBatchModal.batch-uri"
        type="text"
        :required="true"
        placeholder="DeleteByBatchModal.batch-placeholder"
      ></opensilex-InputForm>

    </template>

    <template v-slot:modal-footer>
      <button
        type="button"
        class="btn btn-secondary"
        v-on:click="hide"
      >{{ $t('component.common.close') }}</button>

      <button
        type="button"
        class="btn btn-danger"
        v-on:click="deleteData()"
      >{{ $t('component.common.delete') }}</button>
    </template>

  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Prop, Ref } from "vue-property-decorator";
import {DataService} from "opensilex-core/api/data.service";

@Component
export default class DeleteByBatchModal extends Vue {
  //#region: Props
  @Prop()
  experimentUri: string;
  //#endregion

  //#region: Data
  $opensilex: any;
  $t: any;
  $store: any;
  dataService: DataService;

  batchUri: string = "";

  //#endregion
  //#region: Refs
  @Ref("modal") readonly modal!: any;

  //#endregion
  //#region: hooks

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
  }

  //#endregion
  //#region: Methods

  show() {
    this.modal.show();
  }

  hide() {
    this.modal.hide();
  }

  private async deleteData() {
    let result = (await this.dataService.deleteDataOnSearch(
      this.experimentUri,
      undefined,
      undefined,
      undefined,
      this.batchUri
    )).response.result as any;
    this.hide();
    let deletedCount : number = 0;
    if(result.deletedCount){
      deletedCount = result.deletedCount;
    }
    if(deletedCount > 0){
      this.$opensilex.showSuccessToast(deletedCount + "  " + this.$t("DeleteByBatchModal.success-message"));
      this.emitDeleted();
    }else{
      this.$opensilex.showErrorToast(this.$t("DeleteByBatchModal.error-message"));
    }

  }

  //#endregion
  //#region event emits
  private emitDeleted(){
    this.$emit("deleted");
  }

  //#endregion
}
</script>

<style>

</style>

<i18n>
en:
  DeleteByBatchModal:
    title: Delete by Batch
    batch-uri: Batch URI
    batch-placeholder: Enter URI
    success-message: data have been deleted.
    error-message: No data found.

fr:
  DeleteByBatchModal:
    title: Suppression par batch
    batch-uri: URI de Batch
    batch-placeholder: Entrez URI
    success-message: données ont étés supprimés.
    error-message: Pas de données trouvées
</i18n>