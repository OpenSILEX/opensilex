<template>
  <div class="modal fade" ref="modalRef" tabindex="-1" :class="{ show: isVisible }" style="display: block;" v-if="isVisible">
    <div class="modal-dialog" :class="'modal-' + modalSize">
      <div class="modal-content">

        <div class="modal-header">
          <h5 class="modal-title">{{ t('EventModalView.event') }}</h5>
        </div>

        <div class="modal-body">
          <div class="card-body" v-if="event">
            <opensilex-UriView :uri="event.uri" />
            <opensilex-TypeView :type="event.rdf_type" :typeLabel="event.rdf_type_name" />
            <opensilex-TextView label="component.common.description" :value="event.description" /><br>

            <opensilex-StringView v-if="event.start" label="EventModalView.start" :value="formatDate(event.start)" />
            <opensilex-StringView v-if="event.end" :label="t('EventModalView.end')" :value="formatDate(event.end)" />
            <opensilex-StringView :label="t('EventModalView.publisher')" :value="displayPublisher(event.publisher)" />
            <opensilex-StringView :label="t('EventModalView.datePublication')" :value="event.publication_date" />
            <opensilex-StringView :label="t('EventModalView.lastUpdateDate')" :value="event.last_updated_date" />
            <br>
            <opensilex-StringView class="overflow-auto" style="height: 100px" :label="t('EventModalView.targets')" :uri="event.targets">
              <div v-for="targetURI in event.targets" :key="targetURI">
                <opensilex-UriLink :uri="targetURI" :value="uriLabels[targetURI]" :to="{ path: uriPaths[targetURI] }" />
              </div>
            </opensilex-StringView>
          </div>

          <div v-if="event && isMove()">
            <opensilex-MoveView
              :event="event"
              :positionsUriLabels="positionsUriLabels"
              :positionsUriPaths="positionsUriPaths"
            />
          </div>

          <div v-if="hasRelations(event)" class="card-body">
            <br>
            <p><b>{{ t('EventModalView.specific-properties') }} ({{ event.rdf_type_name }})</b></p>
            <hr />
            <div v-for="(relation, index) in event.relations" :key="index">
              <opensilex-UriView
                :uri="relation.value"
                :value="specificPropertiesLabels[relation.value] || relation.value"
                :title="getPropertyName(relation.property)"
                :to="specificPropertiesPaths[relation.value] ? { path: specificPropertiesPaths[relation.value] } : null"
                customClass="specificProperties"
              />
            </div>
          </div>
          <br>

          <opensilex-DocumentTabList
            :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
            :uri="event.uri"
            :search="false"
          />
        </div>

        <div class="modal-footer">
          <button type="button" class="btn greenThemeColor" @click="hide()">
            {{ t('component.common.ok') }}
          </button>
        </div>

      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, nextTick, inject } from 'vue';
import { useStore } from 'vuex';
import { EventDetailsDTO, MoveDetailsDTO } from 'opensilex-core/index';
import { VueJsOntologyExtensionService, VueRDFTypeDTO } from '../../../lib';
import { OntologyService } from 'opensilex-core/api/ontology.service';
import OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin';
import { useI18n } from 'vue-i18n'

const props = defineProps({
  modalSize: { type: String, default: 'lg' },
  static: { type: Boolean, default: true }
});

const modalRef = ref<HTMLDivElement | null>(null);
const isVisible = ref(false);
const store = useStore();

const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const $vueJsOntologyService = $opensilex.getService("opensilex.VueJsOntologyExtensionService") as VueJsOntologyExtensionService;
const ontologyService = $opensilex.getService("opensilex.OntologyService") as OntologyService;

const uriLabels = reactive<{ [key: string]: string }>({});
const uriPaths = reactive<{ [key: string]: string }>({});
const specificPropertiesLabels = reactive<{ [key: string]: string }>({});
const specificPropertiesPaths = reactive<{ [key: string]: string }>({});
const positionsUriLabels = reactive<{ [key: string]: string }>({});
const positionsUriPaths = reactive<{ [key: string]: string }>({});
const event = ref<EventDetailsDTO | null>(null);
const type = ref<string | undefined>(undefined);
const eventPropertyByUri = reactive<Map<string, VueRDFTypeDTO>>(new Map());

const credentials = computed(() => store.state.credentials);

function formatDate(date: string) {
  return new Date(date).toLocaleString();
}

function isMove(evt = event.value): boolean {
  return evt?.rdf_type ? $opensilex.Oeev.checkURIs(evt.rdf_type, $opensilex.Oeev.MOVE_TYPE_URI) : false;
}

function hasRelations(evt: EventDetailsDTO | null): boolean {
  return !!(evt && evt.relations && evt.relations.length > 0);
}

function displayPublisher(publisher: any) {
  if (!publisher?.uri) return undefined;
  return publisher.first_name && publisher.last_name
    ? `${publisher.first_name} ${publisher.last_name}`
    : publisher.uri;
}

function getPropertyName(propertyUri: string) {
  return propertyUri ? eventPropertyByUri.get(propertyUri)?.name : undefined;
}

async function show(getEventPromiseHttpResult: any) {
  const ev: EventDetailsDTO = getEventPromiseHttpResult.response.result;

  if (ev.targets?.length) {
    const [targetLabels, targetTypes] = await Promise.all([
      ontologyService.getURILabelsList(ev.targets),
      ontologyService.getURITypes(ev.targets),
    ]);
    targetLabels.response.result.forEach(el => uriLabels[el.uri] = el.name);
    targetTypes.response.result.forEach(el => {
      uriPaths[el.uri] = $opensilex.getTargetPath(el.uri, null, $opensilex.getPathFromUriTypes(el.rdf_types));
    });
  }

  if (ev.relations?.length) {
    const relationURIs = ev.relations.map(r => r.value);
    const [labels, types] = await Promise.all([
      ontologyService.getURILabelsList(relationURIs),
      ontologyService.getURITypes(relationURIs),
    ]);
    labels.response.result.forEach(el => specificPropertiesLabels[el.uri] = el.name);
    types.response.result.forEach(el => {
      specificPropertiesPaths[el.uri] = $opensilex.getTargetPath(el.uri, null, $opensilex.getPathFromUriTypes(el.rdf_types));
    });
  }

  if (isMove(ev) && (ev as MoveDetailsDTO).targets_positions) {
    const targetUris = (ev as MoveDetailsDTO).targets_positions.map(p => p.target);
    try {
      const [labelsRes, typesRes] = await Promise.all([
        ontologyService.getURILabelsList(targetUris),
        ontologyService.getURITypes(targetUris),
      ]);
      labelsRes.response.result.forEach(el => positionsUriLabels[el.uri] = el.name);
      typesRes.response.result.forEach(el => positionsUriPaths[el.uri] =
        $opensilex.getTargetPath(el.uri, null, $opensilex.getPathFromUriTypes(el.rdf_types))
      );
    } catch (error) {
      console.error("Error processing move positions:", error);
    }
  }

  event.value = ev;
  type.value = ev.rdf_type;

  buildPropertyMap();
  await nextTick();
  buildPropertyMap();
  isVisible.value = true;
}

function buildPropertyMap() {
  if (!type.value || $opensilex.Oeev.checkURIs(type.value, $opensilex.Oeev.EVENT_TYPE_URI)) return;
  $vueJsOntologyService.getRDFTypeProperties(type.value, $opensilex.Oeev.EVENT_TYPE_URI)
    .then(http => {
      eventPropertyByUri.clear();
      http.response.result.data_properties.forEach(p => eventPropertyByUri.set(p.uri, p));
      http.response.result.object_properties.forEach(p => eventPropertyByUri.set(p.uri, p));
    })
    .catch($opensilex.errorHandler);
}

function hide() {
  isVisible.value = false;
}

defineExpose({
  show,
  hide
})
</script>

<style scoped lang="scss">
.modal.show {
  display: block;
}
.overflow-auto {
  overflow: auto;
}
</style>

<i18n>
en:
  EventModalView:
    start: Begin
    targets: Targets
    end: End
    event: Event
    publisher: Publisher
    datePublication: Issued
    lastUpdateDate: Modified
    specific-properties: Specific properties

fr:
  EventModalView:
    start: Début
    targets: Concerne
    end: Fin
    event: Évenement
    publisher: Publieur
    datePublication: Publié
    lastUpdateDate: Modifié
    specific-properties: Propriétés spécifiques
</i18n>
