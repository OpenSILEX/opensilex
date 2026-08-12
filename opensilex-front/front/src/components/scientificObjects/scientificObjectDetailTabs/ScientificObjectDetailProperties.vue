<template>
  <div v-if="selected && selected.uri">
    <card
      label="component.common.informations"
      icon="bi-clipboard"
    >
      <template #rightHeader v-if="globalView">
<!--        <div class="card-header-right">-->

            <FavoriteButton
              :uri="selected.uri"
            ></FavoriteButton>

            <EditButton
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                )
              "
              @click="scientificObjectForm.editScientificObject(selected.uri)"
              label="ExperimentScientificObjects.edit-scientific-object"
              :small="true"
            ></EditButton>

            <DeleteButton
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID
                )
              "
              label="ExperimentScientificObjects.delete-scientific-object"
              @click="deleteScientificObject(selected.uri)"
              :small="true"
            ></DeleteButton>

          <ScientificObjectForm
            ref="scientificObjectForm"
            @onUpdate="$emit('onUpdate', selected.uri)"
            @onCreate="$emit('onCreate', selected.uri)"
          ></ScientificObjectForm>
<!--        </div>-->
      </template>

      <template #body>
        <div class="detailsCard">
          <!-- URI -->
          <UriView
            v-if="withBasicProperties"
            :uri="selected.uri"
          ></UriView>
          <!-- Name -->
          <StringView
            v-if="withBasicProperties"
            :value="selected.name"
            label="component.common.name"
          ></StringView>
          <!-- Type -->
          <TypeView
            v-if="withBasicProperties"
            :type="selected.rdf_type"
            :typeLabel="selected.rdf_type_name"
          ></TypeView>

          <!-- Geometry -->
          <GeometryCopy
            v-if="selected.geometry"
            :value="selected.geometry"
          ></GeometryCopy>

          <!--Last Position-->
          <StringView v-if="withBasicProperties && lastPosition.event" label="component.common.geometry.lastPosition">
            <!-- Position detail -->
            <span>{{new Date(lastPosition.location.endDate).toLocaleString()}}</span>
            <ul>
              <li v-if="lastPosition.location && lastPosition.location.to">{{lastPosition.location.to}}</li>
              <li v-if="lastPosition.location && (lastPosition.location.x || lastPosition.location.y || lastPosition.location.z)">{{getCustomCoordinatesText(lastPosition.location)}}</li>
              <li v-if="lastPosition.location && lastPosition.location.text">{{lastPosition.location.text}}</li>
              <li v-if="lastPosition.location && lastPosition.location.geojson">
                <GeometryCopy label="" :value="lastPosition.location.geojson">
                </GeometryCopy>
              </li>
            </ul>
          </StringView>

          <!-- Relations -->
          <OntologyObjectProperties
            :selected="selected"
            :parentType="oeso.SCIENTIFIC_OBJECT_TYPE_URI"
            :relations="relations"
            :ignoredProperties="[oeso.IS_HOSTED]"
            :additionalFieldProps="{ experiment }"
          ></OntologyObjectProperties>

          <!-- Metadata -->
          <MetadataView
            v-if="selected.publisher && selected.publisher.uri"
            :publisher="selected.publisher"
            :publicationDate="selected.publication_date"
            :lastUpdatedDate="selected.last_updated_date"
          ></MetadataView>
        </div>
      </template>

    </card>


    <card v-for="(value, index) in objectByContext" :key="index">
      <template #header>
        <h3>
          {{ $t("component.experiment.view.title") }}:
          <UriLink
            :uri="value.uri"
            :allowCopy="false"
            :to="{
              path:
                '/experiment/details/' + encodeURIComponent(value.experiment),
            }"
            :value="value.experiment_name"
          ></UriLink>
        </h3>
      </template>
      <template #body>
        <div class="detailsCard">
          <!-- Name -->
          <StringView
            :value="value.name"
            label="component.common.name"
          ></StringView>
          <!-- Type -->
          <TypeView
            v-if="selected.rdf_type !== value.rdf_type"
            :type="value.rdf_type"
            :typeLabel="value.rdf_type_name"
          ></TypeView>

          <!-- Relations -->
          <OntologyObjectProperties
            :selected="selected"
            :parentType="oeso.SCIENTIFIC_OBJECT_TYPE_URI"
            :relations="value.relations"
            :ignoredProperties="[oeso.IS_HOSTED]"
            :additionalFieldProps="{ experiment: value.experiment }"
          ></OntologyObjectProperties>

          <!-- Metadata -->
          <MetadataView
            v-if="value.publisher && value.publisher.uri"
            :publisher="value.publisher"
            :publicationDate="value.publication_date"
            :lastUpdatedDate="value.last_updated_date"
          ></MetadataView>
        </div>
      </template>

    </card>
  </div>
</template>

<script setup lang="ts">

import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import {ScientificObjectsService} from "opensilex-core/api/scientificObjects.service";
import {PositionsService} from "opensilex-core/api/positions.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {computed, inject, onMounted, ref, useTemplateRef, watch} from "vue";
import {useStore} from "vuex";
import ScientificObjectForm from "@/components/scientificObjects/ScientificObjectForm.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {ScientificObjectDetailByExperimentsDTO} from "opensilex-core/model/scientificObjectDetailByExperimentsDTO";
import {PositionGetDTO} from "opensilex-core/model/positionGetDTO";
import {useRouter} from "vue-router";
import {LocationObservationDTO} from "opensilex-core/model/locationObservationDTO";
import {useI18n} from "vue-i18n";
import FavoriteButton from "@/components/common/buttons/FavoriteButton.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import UriView from "@/components/common/views/UriView.vue";
import StringView from "@/components/common/views/StringView.vue";
import TypeView from "@/components/common/views/TypeView.vue";
import GeometryCopy from "@/components/common/views/GeometryCopy.vue";
import MetadataView from "@/components/common/views/MetadataView.vue";
import Card from "@/components/common/views/Card.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import OntologyObjectProperties from "@/components/ontology/OntologyObjectProperties.vue";

//#region Constant values & Services & Injects
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex');
const $store = useStore();
const $router = useRouter();
const { t } = useI18n();
const scientificObjectsService = $opensilex.getService<ScientificObjectsService>("opensilex.ScientificObjectsService");
const positionsService = $opensilex.getService<PositionsService>("opensilex.PositionsService");

//#endregion

//#region Props
interface Props{
  selected: ScientificObjectDetailByExperimentsDTO,
  objectByContext?: ScientificObjectDetailByExperimentsDTO[],
  globalView?: boolean,
  withBasicProperties?: boolean,
  experiment: string
}

//TODO MAX In vue2 experiment had a default value of null, which seems wierd so we'll try without that at first

const props = withDefaults(
  defineProps<Props>(),
  {objectByContext: () => [], globalView: false, withBasicProperties: true}
);
//#endregion

//#region Reactive data
const relations = ref<RDFObjectRelationDTO[]>([]);

const lastPosition = ref<PositionGetDTO>({event: null, location: {}});
//#endregion

//#region Computed
const oeso = computed(() => {
  return $opensilex.Oeso;
})

const user = computed(() => {
  return $store.state.user;
})

const credentials = computed(() => {
  return $store.state.credentials;
})
//#endregion

//#region Template refs
const scientificObjectForm = useTemplateRef<InstanceType<typeof ScientificObjectForm>>('scientificObjectForm');
//#endregion

//#region Watchers
/**
 * Watcher to fetch details and OS positions upon OS selection change
 */
watch(
   props.selected,
   (newSelectedValue) => {
     $opensilex.disableLoader();
     if (props.globalView) {
       return Promise.all([
         scientificObjectsService.getScientificObjectDetail(props.selected.uri, undefined),
         positionsService.getPosition(props.selected.uri).catch(() => null),
       ]).then((result) => {
         $opensilex.enableLoader();
         relations.value = result[0].response.result.relations;

         if (result[1] != null) {
           lastPosition.value = result[1].response.result;
         }
       });
     } else {
       positionsService.getPosition(props.selected.uri)
         .catch($opensilex.errorHandler)
         .then((result:  HttpResponse<OpenSilexResponse<PositionGetDTO>>) => {
           $opensilex.enableLoader();

           if (result[1] != null) {
             lastPosition.value = result[1].response.result;
           }

           relations.value = props.selected.relations;
         });
     }
   },
  {immediate: true}
 );
//#endregion

//#region Functions
function deleteScientificObject(uri): void {
  scientificObjectsService
    .deleteScientificObject(uri)
    .then(() => {
      //I feel like using replace here is better so that we can't navigate back to a deleted OS
      //TODO MAX test that theory
      $router.replace({
        path: "/scientific-objects",
      });
    })
    .catch(this.$opensilex.errorHandler);
}

function getCustomCoordinatesText(location: LocationObservationDTO): string {
  if (!location) {
    return undefined;
  }

  let customCoordinates = "";

  if (location.x) {
    customCoordinates += "X:" + location.x;
  }
  if (location.y) {
    if (customCoordinates.length > 0) {
      customCoordinates += ", ";
    }
    customCoordinates += "Y:" + location.y;
  }
  if (location.z) {
    if (customCoordinates.length > 0) {
      customCoordinates += ", ";
    }
    customCoordinates += "Z:" + location.z;
  }

  if (customCoordinates.length == 0) {
    return undefined;
  }
  return customCoordinates;
}
//#endregion

</script>

<style scoped lang="scss">
//TODO MAX This exact class is duplicated 4 or 5 times, test putting it in main.css or whatever
.detailsCard {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
