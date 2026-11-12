<template>
  <n-spin :show="loading" class="dataVisuDetails">
    <!-- Clicked data point: its provenance and data, then its annotations -->
    <template v-if="data">
      <div class="dataCards">
        <Card :label="t('DataVisuDetails.provenance')" :noFooter="true" class="detailCard">
          <template #body>
            <pre>{{ provenanceJson }}</pre>
          </template>
        </Card>
        <Card :label="t('DataVisuDetails.data')" :noFooter="true" class="detailCard">
          <template #body>
            <pre>{{ dataJson }}</pre>
          </template>
        </Card>
      </div>
      <Card :label="t('component.annotation.list-title')" :noFooter="true">
        <template #body>
          <AnnotationList
              ref="annotationList"
              :key="data.uri"
              :target="data.uri"
              :displayTargetColumn="false"
              :enableActions="false"
          ></AnnotationList>
        </template>
      </Card>
    </template>

    <!-- Clicked event pin -->
    <Card v-else-if="eventDetails" :label="t('component.events.event')" :noFooter="true">
      <template #body>
        <TypeView :type="eventDetails.rdf_type" :typeLabel="eventDetails.rdf_type_name ?? eventDetails.rdf_type"></TypeView>
        <!-- An instant event only has an end date, displayed as its begin -->
        <DateView
            label="DataVisuGraphic.eventBegin"
            :value="eventDetails.start ?? eventDetails.end"
            :isDatetime="true"
            :useLocaleFormat="true"
        ></DateView>
        <template v-if="eventDetails.start">
          <DateView
              v-if="eventDetails.end"
              label="DataVisuGraphic.eventEnd"
              :value="eventDetails.end"
              :isDatetime="true"
              :useLocaleFormat="true"
          ></DateView>
          <StringView v-else label="DataVisuGraphic.eventEnd" :value="t('DataVisuGraphic.eventOngoing')"></StringView>
        </template>
        <TextView v-if="eventDetails.description" label="component.common.description" :value="eventDetails.description"></TextView>
        <StringView label="component.common.creator" :value="publisherName"></StringView>
      </template>
    </Card>
  </n-spin>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, ref, Ref, useTemplateRef, watch} from "vue";
import {useI18n} from "vue-i18n";
import {NSpin} from "naive-ui";
import {DataGetSearchDTO, DataService, EventDetailsDTO, EventGetDTO, EventsService, ProvenanceGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import Card from "@/components/common/views/Card.vue";
import TypeView from "@/components/common/views/TypeView.vue";
import DateView from "@/components/common/views/DateView.vue";
import StringView from "@/components/common/views/StringView.vue";
import TextView from "@/components/common/views/TextView.vue";
import AnnotationList from "@/components/annotations/list/AnnotationList.vue";

/**
 * Details displayed under a graphic when one of its elements is clicked:
 * - for a data point, the provenance and the content of the data, and the annotations of the data
 * - for an event pin, the type, dates, description and creator of the event
 */

//#region Public
const props = defineProps<{
  /** Data of the clicked point */
  data?: DataGetSearchDTO
  /** Event of the clicked pin, ignored if data is given */
  event?: EventGetDTO
}>();
//#endregion

//#region Private
//#region Plugins & services
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const {t} = useI18n();

const dataService: DataService = $opensilex.getService<DataService>("opensilex.DataService");
const eventsService: EventsService = $opensilex.getService<EventsService>("opensilex.EventsService");
//#endregion

//#region Template refs
const annotationList = useTemplateRef<InstanceType<typeof AnnotationList>>("annotationList");
//#endregion

//#region Data & Computed
const loading: Ref<boolean> = ref<boolean>(false);
const provenance: Ref<ProvenanceGetDTO | undefined> = ref<ProvenanceGetDTO | undefined>(undefined);
const eventDetails: Ref<EventDetailsDTO | undefined> = ref<EventDetailsDTO | undefined>(undefined);

const dataJson: ComputedRef<string> = computed<string>(() => JSON.stringify(props.data, null, 2));
const provenanceJson: ComputedRef<string> = computed<string>(() => JSON.stringify(provenance.value, null, 2));

/** First and last name of the creator of the event, or its URI */
const publisherName: ComputedRef<string> = computed<string>(() => {
  const publisher = eventDetails.value?.publisher;
  if (!publisher?.uri) {
    return "";
  }
  return publisher.first_name && publisher.last_name ? `${publisher.first_name} ${publisher.last_name}` : publisher.uri;
});
//#endregion

//#region Hooks & watchers
watch(() => props.data, loadProvenance, {immediate: true});
watch(() => props.event, loadEventDetails, {immediate: true});
//#endregion

//#region Methods
async function loadProvenance(): Promise<void> {
  provenance.value = undefined;
  if (!props.data?.provenance?.uri) {
    return;
  }
  loading.value = true;
  try {
    provenance.value = (await dataService.getProvenance(props.data.provenance.uri)).response.result;
  } catch (error) {
    $opensilex.errorHandler(error);
  } finally {
    loading.value = false;
  }
}

async function loadEventDetails(): Promise<void> {
  eventDetails.value = undefined;
  if (!props.event?.uri) {
    return;
  }
  loading.value = true;
  try {
    eventDetails.value = (await eventsService.getEventDetails(props.event.uri)).response.result;
  } catch (error) {
    $opensilex.errorHandler(error);
  } finally {
    loading.value = false;
  }
}


/**
 * Reloads the annotations of the data, e.g. after an annotation was created on it
 */
function refreshAnnotations(): void {
  annotationList.value?.refresh();
}
//#endregion
//#endregion

defineExpose({
  refreshAnnotations
});
</script>

<style scoped lang="scss">
.dataVisuDetails {
  margin-top: 12px;
}

.dataCards {
  display: flex;
  gap: 12px;
}

.detailCard {
  flex: 1;
  min-width: 0;
}

pre {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow: auto;
  margin: 0;
}
</style>
