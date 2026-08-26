<template>
  <div class="experiments-view-container">
    <div class="experiments-header">
      <h2>{{ t("ExperimentsView.title") }}</h2>
      <p>{{ t("ExperimentsView.description") }}</p>
    </div>

    <div class="experiments-content">
      <opensilex-ExperimentSimpleList :experimentList="experiments" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted , inject} from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { ExperimentGetListDTO } from 'opensilex-core/model/experimentGetListDTO'
import type { ExperimentsService } from 'opensilex-core/api/experiments.service'
import HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'

const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const experiments = ref<ExperimentGetListDTO[]>([])

async function loadExperiments() {
  try {
    const experimentsService = $opensilex.getService<ExperimentsService>('opensilex.ExperimentsService')
    const response = await experimentsService.searchExperiments()
    const httpResponse = response as HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>
    experiments.value = httpResponse.response?.result ?? []
  }
  catch (error) {
    $opensilex.errorHandler(error)
  }
}

onMounted(() => {
  loadExperiments()
})
</script>

<style scoped lang="scss">
.experiments-view-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.experiments-header {
  margin-bottom: 20px;

  h2 {
    margin: 0 0 8px 0;
    color: #00a38d;
    font-size: 24px;
  }

  p {
    margin: 0;
    color: #666;
    font-size: 14px;
  }
}

.experiments-content {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 15px;
}
</style>

<i18n>
en:
  ExperimentsView:
    title: "Experiments"
    description: "Select an experiment to view its map"
fr:
  ExperimentsView:
    title: "Expériences"
    description: "Sélectionnez une expérience pour voir sa carte"
</i18n>