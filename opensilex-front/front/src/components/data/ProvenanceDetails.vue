<template>
  <div class="container-fluid">
    <div class="provenance-details-row">
      <opensilex-Card
        icon
        :label="t(label)"
        :class="computedClass"
      >
        <template #rightHeader>
          <div class="ml-3"></div>
        </template>

        <template #body>
          <!-- uri -->
          <opensilex-UriView
            label="ProvenanceDetails.uri"
            :uri="provenance.uri"
            :value="provenance.uri"
            :to="{ path: '/provenances/details/' + encodeURIComponent(provenance.uri) }"
          />

          <!-- name -->
          <opensilex-StringView
            :label="t('component.common.name')"
            :value="provenance.name"
            class="provenanceDetails"
          />

          <!-- description -->
          <opensilex-StringView
            :label="t('component.common.description')"
            :value="provenance.description"
          />

          <!-- activity -->
          <opensilex-StringView
            v-if="provenance.prov_activity != null && provenance.prov_activity.length > 0"
            :label="t('ProvenanceDetails.activity')"
            :value="provenance.prov_activity[0].rdf_type"
          />

          <!-- agent -->
          <opensilex-UriListView
            v-if="agentList.length > 0"
            label="ProvenanceDetails.agent"
            :list="agentList"
          />
        </template>
      </opensilex-Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

type ProvenanceAgent = {
  uri: string
}

type ProvenanceActivity = {
  rdf_type: string
}

type Provenance = {
  uri: string | null
  name: string | null
  description: string | null
  experiments: any[]
  prov_activity: ProvenanceActivity[]
  prov_agent: ProvenanceAgent[]
}

const { t } = useI18n()

const emit = defineEmits<{
  (e: 'onUpdate', provenance: any): void
}>()

const props = withDefaults(defineProps<{
  label?: string
  provenance?: Provenance
  dataImportResult?: boolean
}>(), {
  label: 'ProvenanceDetails.title',
  provenance: () => ({
    uri: null,
    name: null,
    description: null,
    experiments: [],
    prov_activity: [],
    prov_agent: []
  }),
  dataImportResult: false
})

const computedClass = computed(() => {
  return props.dataImportResult === true
    ? 'dataImportProvenanceDetails'
    : 'ProvenanceDetailsVisible'
})

const agentList = computed(() => {
  if (props.provenance.prov_agent != null && props.provenance.prov_agent != undefined) {
    return props.provenance.prov_agent.map((item) => ({
      uri: item.uri,
      url: null,
      value: item.uri
    }))
  }
  return []
})

function update(provenance: any) {
  emit('onUpdate', provenance)
}

function successMessage(provenance: any) {
  return t('component.provenance.name') + ' ' + provenance.name
}

defineExpose({
  update,
  successMessage
})
</script>

<style scoped>
.details-actions-row {
  margin-top: -35px;
  margin-left: -15px;
  margin-right: 15px;
}

.provenance-details-row {
  display: flex;
}

.ProvenanceDetailsVisible {
  width: auto;
  min-width: auto;
  max-width: 340px;
}

.dataImportProvenanceDetails {
  width: auto;
  min-width: auto;
  max-width: 500px;
}

@media screen and (min-width: 1200px) {
  .ProvenanceDetailsVisible {
    min-width: 340px;
    max-width: 340px;
    margin-left: -20px;
    overflow: hidden;
  }

  .dataImportProvenanceDetails {
    min-width: 340px;
    max-width: 500px;
    margin-left: 0;
    overflow: hidden;
  }
}
</style>

<i18n>
en:
  ProvenanceDetails:
    activity: Activity type
    uri: Uri
    label: Name
    description: Description
    no-provenance-found: No provenance found
    agent: Agent(s)
    title: Provenance description
fr:
  ProvenanceDetails:
    activity: Type d'activité
    uri: Uri
    label: Nom
    description: Description
    no-provenance-found: Aucune provenance trouvée
    agent: Agent(s)
    title: Description de la provenance
</i18n>