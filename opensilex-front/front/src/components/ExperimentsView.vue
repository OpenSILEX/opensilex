<template>
  <div class="experiments-view-container">
    <div class="experiments-header">
      <h2>{{ t("ExperimentsView.title") }}</h2>
      <p>{{ t("ExperimentsView.description") }}</p>
    </div>

    <div class="experiments-content">
      <opensilex-ExperimentSimpleList :experimentList="mockExperiments" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { ExperimentGetListDTO } from 'opensilex-core/model/experimentGetListDTO'

const { t } = useI18n()

// Mock data for testing - clearly labeled as MOCK DATA
const mockExperiments: ExperimentGetListDTO[] = [
  {
    uri: 'urn:experiment:exp1',
    name: 'Wheat Yield Trial 2025',
    start_date: '2025-03-15',
    end_date: '2025-10-30',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Dupont',
    status: 'ACTIVE',
  },
  {
    uri: 'urn:experiment:exp2',
    name: 'Corn Drought Resistance Study',
    start_date: '2025-04-01',
    end_date: '2025-11-15',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Martin',
    status: 'ACTIVE',
  },
  {
    uri: 'urn:experiment:exp3',
    name: 'Barley Genetic Diversity Analysis',
    start_date: '2025-02-20',
    end_date: '2025-09-30',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Bernard',
    status: 'ACTIVE',
  },
  {
    uri: 'urn:experiment:exp4',
    name: 'Soybean Nitrogen Fixation Trial',
    start_date: '2025-05-10',
    end_date: '2025-12-01',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Petit',
    status: 'ACTIVE',
  },
  {
    uri: 'urn:experiment:exp5',
    name: 'Rice Flood Tolerance Assessment',
    start_date: '2025-06-01',
    end_date: '2026-01-15',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Leroy',
    status: 'ACTIVE',
  },
  {
    uri: 'urn:experiment:exp6',
    name: 'Sunflower Oil Quality Study',
    start_date: '2025-03-01',
    end_date: '2025-10-15',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Moreau',
    status: 'COMPLETED',
  },
  {
    uri: 'urn:experiment:exp7',
    name: 'Rapeseed Disease Resistance',
    start_date: '2025-04-15',
    end_date: '2025-11-30',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Simon',
    status: 'ACTIVE',
  },
  {
    uri: 'urn:experiment:exp8',
    name: 'Potato Blight Prevention Trial',
    start_date: '2025-05-20',
    end_date: '2026-02-28',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Laurent',
    status: 'ACTIVE',
  },
  {
    uri: 'urn:experiment:exp9',
    name: 'Beet Sugar Content Analysis',
    start_date: '2025-03-10',
    end_date: '2025-10-20',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Michel',
    status: 'COMPLETED',
  },
  {
    uri: 'urn:experiment:exp10',
    name: 'Tomato Greenhouse Optimization',
    start_date: '2025-01-15',
    end_date: '2025-08-31',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Grande',
    status: 'COMPLETED',
  },
  {
    uri: 'urn:experiment:exp11',
    name: 'Lettuce Hydroponic System Test',
    start_date: '2025-07-01',
    end_date: '2026-03-31',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Roux',
    status: 'ACTIVE',
  },
  {
    uri: 'urn:experiment:exp12',
    name: 'Strawberry Pest Management',
    start_date: '2025-04-20',
    end_date: '2025-12-15',
    facility: { uri: 'urn:facility:inrae', name: 'INRAE' },
    principalInvestigator: 'Dr. Blanc',
    status: 'ACTIVE',
  },
]
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