<template>
  <div class="container-fluid">
    <b-row>
      <opensilex-Card
        icon label="BatchDetails.title"
        class="dataImportBatchDetails"
      >
        <template v-slot:rightHeader>
          <div class="ml-3"></div>
        </template>
        <template v-slot:body>

          <!-- uri -->
          <opensilex-UriView
            label="BatchDetails.uri"
            :uri="batchDetails.uri"
            :value="batchDetails.uri"
          ></opensilex-UriView>

          <!-- username -->
          <opensilex-StringView
            label="BatchDetails.user-name"
            :value="batchDetails.username"
          ></opensilex-StringView>

          <!-- document uri -->
          <opensilex-UriView
            title="BatchDetails.document-uri"
            :uri="batchDetails.documentUri"
            :value="batchDetails.documentUri"
          ></opensilex-UriView>

        </template>
      </opensilex-Card>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop} from "vue-property-decorator";
import {BatchHistoryGetDTO} from "opensilex-core/model/batchHistoryGetDTO";
import AsyncComputedProp from "vue-async-computed-decorator";
import {DataService} from "opensilex-core/api/data.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component({})
export default class BatchDetails extends Vue {

  $opensilex: OpenSilexVuePlugin;
  dataService: DataService;

  batchDetails = null;

  @Prop()
  batchUri: string;

  async mounted(){
    this.batchDetails = (await this.dataService.getBatchHistory(this.batchUri)).response.result;
  }

  created(){
    this.dataService = this.$opensilex.getService("opensilex.DataService");
  }
}
</script>

<style scoped lang="scss">

.dataImportBatchDetails{
  width: auto;
  min-width: auto;
  max-width: 500px;
}

@media screen and (min-width: 1200px) {
  .dataImportBatchDetails{
    min-width: 340px;
    max-width: 500px;
    margin-left: 0;
    overflow: hidden;
  }
}
</style>

<i18n>
en:
  BatchDetails:
    uri : Uri
    user-name: Username
    document-uri : Document Uri
    title: Batch description
fr:
  BatchDetails:
    uri : Uri
    user-name: Nom d'utilisateur
    document-uri : Uri du Document
    title: Description de Batch

</i18n>