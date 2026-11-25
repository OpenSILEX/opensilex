<template>
  <div class="container-fluid">
    <div>
      <Card
          v-if="batchDetails"
          :label="t('component.data.batchDetails.title')"
          class="dataImportBatchDetails"
      >
        <template #rightHeader>
          <div class="ml-3"></div>
        </template>
        <template #body>

          <!-- uri -->
          <UriView
              :uri="batchDetails.uri"
              :value="batchDetails.uri"
          />

          <!-- username -->
          <StringView
              :label="t('component.data.batchDetails.user-name')"
              :value="batchDetails.username"
          />

          <!-- document uri -->
          <UriView
              :title="t('component.data.batchDetails.document-uri')"
              :uri="batchDetails.documentUri"
              :value="batchDetails.documentUri"
          />

        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {inject, onMounted, ref} from "vue";
import {useI18n} from "vue-i18n";
import {BatchHistoryGetDTO, DataService} from "../../../../../opensilex-core/front/src/lib";
import Card from "@/components/common/views/Card.vue";
import UriView from "@/components/common/views/UriView.vue";
import StringView from "@/components/common/views/StringView.vue";

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const dataService = opensilex.getService<DataService>("opensilex.DataService");
const batchDetails = ref<BatchHistoryGetDTO>(null)

const props = defineProps<{
  batchUri: string
}>()

onMounted(async () => {
  batchDetails.value = (await dataService.getBatchHistory(props.batchUri)).response.result;
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
