<template>
  <div>
    <!-- All main info (non metadata stuff) -->
    <div class="main-info-style">
      <!-- URI -->
      <opensilex-UriView
        v-if="!hasNoDetailsPage"
        class="uriLinkGlobalUriSearchRes"
        :uri="uri"
        :value="shortUri"
        :to="detailsPath"
        @linkClicked="$emit('hideUriSearch')"
      />

      <span v-else class="data-uri-details">
        <opensilex-UriView :uri="shortUri" :value="shortUri" />
        <opensilex-Button
          v-if="dataDto.uri"
          :small="true"
          @click="handleSeeDetails"
          label="GlobalUriSearch.seeDetails"
          class="data-uri-details-item"
        >
          <template v-slot:icon>
            <opensilex-Icon icon="fa#eye" />
          </template>
        </opensilex-Button>
      </span>

      <!-- Name -->
      <opensilex-StringView
        v-if="!hasNoDetailsPage" 
        :value="name" 
        label="component.common.name" 
       />

      <!-- Type -->
      <opensilex-TypeView
        v-if="!isData"
        :type="type"
        :typeLabel="typeName"
        :copyableTypeUri="true"
      />
      <opensilex-TypeView v-else :typeLabel="$t('GlobalUriSearch.dataTypeName')" />

      <!-- rdfsComment -->
      <opensilex-TextView
        v-if="rdfsComment"
        label="GlobalUriSearch.comment"
        :value="rdfsComment"
      />
    </div>

    <!-- Metadata -->
    <opensilex-MetadataView
      v-if="publisher"
      :publisher="publisher"
      :publicationDate="publicationDate"
      :lastUpdatedDate="updatedDate"
    />

    <!-- Data details -->
    <!-- <opensilex-DataProvenanceModalView
      v-if="hasNoDetailsPage"
      ref="dataProvenanceModalView"
    /> -->

    <!-- Event details -->
    <!-- <opensilex-EventModalView
      modalSize="lg"
      ref="eventModalView"
      :static="false"
    /> -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, inject } from 'vue';
import { useI18n } from 'vue-i18n';
import {DataService} from "opensilex-core/api/data.service";
import {EventsService} from "opensilex-core/api/events.service";
import {UriSearchService} from "opensilex-core/api/uriSearch.service";
import {
  URIGlobalSearchDTO,
  DataGetSearchDTO,
  DataFileGetDTO,
  ProvenanceGetDTO,
  UserGetDTO,
  EventDetailsDTO,
} from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

const props = defineProps({
  searchResult: Object as () => URIGlobalSearchDTO,
});
const { t } = useI18n();
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;

const uri = computed(() => props.searchResult.uri);
const shortUri = computed(() => uri.value ? $opensilex.getShortUri(uri.value) : null);
const name = computed(() => props.searchResult.name);
const typeName = computed(() => props.searchResult.rdf_type_name);
const type = computed(() => props.searchResult.rdf_type);
const rdfsComment = computed(() => props.searchResult.rdfs_comment);
const publisher = computed(() => props.searchResult.publisher);
const publicationDate = computed(() => props.searchResult.publication_date);
const updatedDate = computed(() => props.searchResult.last_updated_date);
const dataDto = computed(() => props.searchResult.data_dto || props.searchResult.datafile_dto);
const hasNoDetailsPage = computed(() => dataDto.value !== null);
const isData = computed(() => props.searchResult.data_dto !== null);

const dataService: DataService = $opensilex.getService("opensilex.DataService");
const eventsService: EventsService = $opensilex.getService("opensilex.EventsService");
const uriSearchService: UriSearchService = $opensilex.getService("opensilex.UriSearchService");



const dataProvenanceModalView = ref(null);
const eventModalView = ref(null);


const detailsPath = computed(() => {
  if (!props.searchResult) return "";
  console.log("props.searchResult ", props.searchResult)

  // Si le type est un groupe de germoplasm
  if ($opensilex.checkURIs(type.value, $opensilex.Oeso.GERMPLASM_GROUP_TYPE_URI)) {
    return "/germplasm/group?selected=" + encodeURIComponent(uri.value);
  }

  let formattedPath = "";

  // Si super_types est défini
  if (props.searchResult.super_types !== null) {
    const rdfTypes = props.searchResult.super_types.rdf_types;
    const basePath = $opensilex.getPathFromUriTypes(rdfTypes);

    const isFactorOrLevel = $opensilex.checkURIs(type.value, $opensilex.Oeso.FACTOR_LEVEL_URI)
      || $opensilex.checkURIs(type.value, $opensilex.Oeso.FACTOR_URI);

    const context = isFactorOrLevel ? props.searchResult.context : undefined;
    const targetUri = $opensilex.checkURIs(type.value, $opensilex.Oeso.FACTOR_LEVEL_URI)
      ? props.searchResult.factor_uri
      : uri.value;

    formattedPath = $opensilex.getTargetPath(targetUri, context, basePath);
    return formattedPath;
  }

  // Sinon, fallback sur la logique vocabulaire
  if (props.searchResult.root_class !== null) {
    return $opensilex.getVocabularyPath(
      uri.value,
      props.searchResult.root_class,
      props.searchResult.is_property
    );
  }

  return "";
});





const handleSeeDetails = async () => {
  if (props.searchResult.super_types) {
    const isEvent = props.searchResult.super_types.rdf_types.some((type) =>
      $opensilex.Oeev.checkURIs(type, $opensilex.Oeev.EVENT_TYPE_URI)
    );
    if (isEvent) {
      const http = await eventsService.getEventDetails(uri.value);
      await eventModalView.value.show(http);
      return;
    }
  }

  if (dataDto.value?.provenance?.uri) {
    const result = await dataService.getProvenance(dataDto.value.provenance.uri);
    dataProvenanceModalView.value.setProvenance({ provenance: result, data: dataDto.value });
    dataProvenanceModalView.value.show();
  }
};

</script>

<style scoped>
.main-info-style {
  margin-bottom: 5px;
}
.data-uri-details {
  display: flex;
  align-items: center;
}
.data-uri-details-item {
  background: none;
  border: none;
  color: #00A38D;
}
.data-uri-details-item:hover {
  color: #02c5ab;
}
</style>

<i18n>
en:
  GlobalUriSearch:
    seeDetails: See details
    dataTypeName: Data
    comment: Description

fr:
  GlobalUriSearch:
    seeDetails: Voir détails
    dataTypeName: Donnée
    comment: Description

</i18n>
