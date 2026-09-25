<template>
  <div class="container-fluid">
    <b-row>
      <Card
          icon label="BatchDetails.title"
          class="dataImportBatchDetails"
      >
        <template v-slot:rightHeader>
          <div class="ml-3"></div>
        </template>
        <template v-slot:body>

          <!-- uri -->
          <UriView
              label="BatchDetails.uri"
              :uri="batchDetails.uri"
              :value="batchDetails.uri"
          ></UriView>

          <!-- document uri -->
          <UriView
              title="BatchDetails.document-uri"
              :uri="batchDetails.documentUri"
              :value="batchDetails.documentUri"
          ></UriView>

        </template>
      </Card>
    </b-row>
  </div>
</template>

<script setup lang="ts">
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {inject, onMounted, ref} from "vue";
import {BatchHistoryGetDTO, DataService} from "../../../../../opensilex-core/front/src/lib";
import UriView from "@/components/common/views/UriView.vue";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const dataService = opensilex.getService<DataService>("opensilex.DataService");
const batchDetails = ref<BatchHistoryGetDTO>(null)

const props = defineProps<{
  batchUri: string
}>()

onMounted( async (batchUri) => {
  batchDetails.value = (await dataService.getBatchHistory(batchUri)).response.result;
})

</script>

<style scoped lang="scss">

.dataImportBatchDetails {
  width: auto;
  min-width: auto;
  max-width: 500px;
}

@media screen and (min-width: 1200px) {
  .dataImportBatchDetails {
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
    uri: Uri
    user-name: Username
    document-uri: Document Uri
    title: Batch description
fr:
  BatchDetails:
    uri: Uri
    user-name: Nom d'utilisateur
    document-uri: Uri du Document
    title: Description de Batch

</i18n>