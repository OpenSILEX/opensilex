<template>
  <div class="row">
    <div class="col-md-6 mb-3">
      <!-- General Informations Card -->
      <opensilex-Card label="component.common.informations" icon="bi-clipboard">
        <template #rightHeader>
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_MODIFICATION_ID)"
            @click="onEdit?.()"
            label="component.project.update"
            :small="true"
          />
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_DELETE_ID)"
            @click="onDelete?.()"
            label="component.project.delete"
            :small="true"
          />
        </template>

        <template #body>
          <div class="detailsCard">
            <opensilex-UriView
              :label="t('component.common.uri')"
              :uri="provenance.uri"
            />

            <opensilex-StringView
              :label="t('component.common.name')"
              :value="provenance.name"
            />

            <opensilex-TextView
              :label="t('component.common.description')"
              :value="provenance.description"
            />

            <opensilex-StringView
              v-if="hasActivity"
              :label="t('ProvenanceDescription.activity')"
              :value="provenance.prov_activity[0].name"
            />

            <opensilex-StringView
              v-if="hasActivity"
              :label="t('ProvenanceDescription.activity_start_date')"
              :value="provenance.prov_activity[0].start_date"
            />

            <opensilex-StringView
              v-if="hasActivity"
              :label="t('ProvenanceDescription.activity_end_date')"
              :value="provenance.prov_activity[0].end_date"
            />

            <opensilex-UriView
              v-if="hasActivity && provenance.prov_activity[0].uri != null"
              :title="t('ProvenanceDescription.activity_external_link')"
              :value="provenance.prov_activity[0].uri"
              :uri="provenance.prov_activity[0].uri"
            />

            <opensilex-MetadataView
              v-if="provenance.publisher && provenance.publisher.uri"
              :publisher="provenance.publisher"
              :publicationDate="provenance.issued"
              :lastUpdatedDate="provenance.modified"
            />
          </div>
        </template>
      </opensilex-Card>
    </div>

    <!-- Provenance Agents Card -->
    <div class="col-md-6 mb-3">
      <opensilex-Card :label="t('ProvenanceDescription.agents')" icon="bi-clipboard" :no-footer="true">
        <template #body>
          <opensilex-TableView
            :items="provenance.prov_agent"
            defaultSortBy="name"
            :fields="tableFields"
          >
            <template #cell(name)="{ data }">
              <opensilex-PersonContact
                v-if="opensilex.Oeso.checkURIs(opensilex.Oeso.OPERATOR_TYPE_URI, data.item.rdf_type)"
                :personContact="data.item.operator"
              />
              <opensilex-UriLink
                v-else
                :uri="data.item.uri"
                :value="data.item.name"
                :to="{ path: '/device/details/' + encodeURIComponent(data.item.uri) }"
              />
            </template>
          </opensilex-TableView>
        </template>
      </opensilex-Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { useStore } from 'vuex'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { ProvenanceGetDTO } from 'opensilex-core/index'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  provenance: ProvenanceGetDTO
  credentials: any
  onEdit?: () => void
  onDelete?: () => void
}>()

const store = useStore()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const user = computed(() => store.state.user)
const { t } = useI18n()

const hasActivity = computed(
  () => props.provenance?.prov_activity != null && props.provenance.prov_activity.length > 0
)

const tableFields = [
  {
    key: 'name',
    label: t('component.common.name'),
    sortable: true
  },
  {
    key: 'rdf_type',
    label: t('component.common.type')
  }
]
</script>

<style lang="scss">
.detailsCard {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>

<i18n>
en:
  ProvenanceDescription:
    activity: Activity type
    activity_start_date: Start Date
    activity_end_date: End Date
    activity_external_link: External link
    agents: Provenance agents

fr:
  ProvenanceDescription:
    activity: Type d'activité
    activity_start_date: Date de début
    activity_end_date: Date de fin
    activity_external_link: Lien externe
    agents: Agents de la provenance
</i18n>