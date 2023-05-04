<template>
  <div v-if="selected && selected.uri">
    <b-card>
      <template v-slot:header v-if="globalView">
        <h3>{{ $t("ScientificObjectDetail.generalInformation") }}:</h3>
        <div class="card-header-right">
          <b-button-group>
            <opensilex-FavoriteButton
                :uri="selected.uri"
            ></opensilex-FavoriteButton>
            <opensilex-EditButton
                v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                )
              "
              @click="soForm.editScientificObject(selected.uri)"
              label="ExperimentScientificObjects.edit-scientific-object"
              :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID
                )
              "
              label="ExperimentScientificObjects.delete-scientific-object"
              @click="deleteScientificObject(selected.uri)"
              :small="true"
            ></opensilex-DeleteButton>
          </b-button-group>
          <opensilex-ScientificObjectForm
            ref="soForm"
            @onUpdate="$emit('onUpdate', selected.uri)"
            @onCreate="$emit('onCreate', selected.uri)"
          ></opensilex-ScientificObjectForm>
        </div>
      </template>

      <!-- URI -->
      <opensilex-UriView
        v-if="withBasicProperties"
        :uri="selected.uri"
      ></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView
        v-if="withBasicProperties"
        :value="selected.name"
        label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
        v-if="withBasicProperties"
        :type="selected.rdf_type"
        :typeLabel="selected.rdf_type_name"
      ></opensilex-TypeView>

      <!-- Geometry -->
      <opensilex-GeometryCopy
              v-if="selected.geometry"
              :value="selected.geometry"
      ></opensilex-GeometryCopy>

      <!--Last Position-->
      <opensilex-StringView v-if="withBasicProperties && lastPosition.event" label="Event.lastPosition">
          <!-- Position detail -->
          <span>{{new Date(lastPosition.move_time).toLocaleString()}}</span>
          <ul>
              <li v-if="lastPosition.to">{{lastPosition.to.name}}</li>
              <li v-if="lastPosition.position && (lastPosition.position.x || lastPosition.position.y || lastPosition.position.z)">{{customCoordinatesText(lastPosition.position)}}</li>
              <li v-if="lastPosition.position && lastPosition.position.text">{{lastPosition.position.text}}</li>
              <li v-if="lastPosition.position && lastPosition.position.point">
                  <opensilex-GeometryCopy label="" :value="lastPosition.position.point">
                  </opensilex-GeometryCopy>
              </li>
          </ul>
      </opensilex-StringView>

      <!-- Relations -->
      <opensilex-OntologyObjectProperties
        :selected="selected"
        :parentType="oeso.SCIENTIFIC_OBJECT_TYPE_URI"
        :relations="relations"
        :ignoredProperties="[oeso.IS_HOSTED]"
        :additionalFieldProps="{ experiment }"
      ></opensilex-OntologyObjectProperties>
    </b-card>

    <b-card v-for="(value, index) in objectByContext" :key="index">
      <template v-slot:header>
        <h3>
          {{ $t("component.experiment.view.title") }}:
          <opensilex-UriLink
            :allowCopy="false"
            :to="{
              path:
                '/experiment/details/' + encodeURIComponent(value.experiment),
            }"
            :value="value.experiment_name"
          ></opensilex-UriLink>
        </h3>
      </template>
      <!-- Name -->
      <opensilex-StringView
        :value="value.name"
        label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
        v-if="selected.rdf_type !== value.rdf_type"
        :type="value.rdf_type"
        :typeLabel="value.rdf_type_name"
      ></opensilex-TypeView>

        <!-- Relations -->
      <opensilex-OntologyObjectProperties
              :selected="selected"
              :parentType="oeso.SCIENTIFIC_OBJECT_TYPE_URI"
              :relations="value.relations"
              :ignoredProperties="[oeso.IS_HOSTED]"
              :additionalFieldProps="{ experiment: value.experiment }"
      ></opensilex-OntologyObjectProperties>
    </b-card>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {PositionGetDTO} from "../../../../../opensilex-core/front/src/lib";
import {ScientificObjectDetailByExperimentsDTO} from "opensilex-core/model/scientificObjectDetailByExperimentsDTO";
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {ScientificObjectsService} from "opensilex-core/api/scientificObjects.service";
import {PositionsService} from "opensilex-core/api/positions.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

@Component
export default class ScientificObjectDetailProperties extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  selected: ScientificObjectDetailByExperimentsDTO;

  relations: Array<RDFObjectRelationDTO> = [];

  @Prop({
    default: () => [],
  })
  objectByContext: Array<ScientificObjectDetailByExperimentsDTO>;

  @Prop({
    default: false,
  })
  globalView;

  @Prop({
    default: true,
  })
  withBasicProperties;

  @Prop({
    default: null,
  })
  experiment;
  lastPosition:PositionGetDTO = {
    event: null,
    from: null,
    position: {
      point: null,
      text: null,
      x:null,
      y:null,
      z:null
    },
    to: null
  };
  mounted() {
    if (this.selected) {
      this.onSelectionChange();
    }
  }

  get oeso() {
      return this.$opensilex.Oeso;
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("soForm") readonly soForm!: any;

  @Watch("selected")
  onSelectionChange() {
    this.$opensilex.disableLoader();
    if (this.globalView) {
      return Promise.all([
        this.$opensilex
          .getService<ScientificObjectsService>("opensilex.ScientificObjectsService")
          .getScientificObjectDetail(this.selected.uri, undefined),
        this.$opensilex
          .getService<PositionsService>("opensilex.PositionsService")
          .getPosition(this.selected.uri)
          .catch(() => null),
      ]).then((result) => {
        this.$opensilex.enableLoader();

        this.relations = result[0].response.result.relations;

        if (result[1] != null) {
          this.lastPosition = result[1].response.result;
        }
      });
    } else {
        this.$opensilex
          .getService<PositionsService>("opensilex.PositionsService")
          .getPosition(this.selected.uri)
          .catch(this.$opensilex.errorHandler)
            .then((result:  HttpResponse<OpenSilexResponse<PositionGetDTO>>) => {
        this.$opensilex.enableLoader();

        if (result[1] != null) {
          this.lastPosition = result[1].response.result;
        }

        this.relations = this.selected.relations;
      });
    }
  }

  deleteScientificObject(uri) {
    let scientificObjectsService = this.$opensilex.getService<ScientificObjectsService>(
      "opensilex.ScientificObjectsService"
    );
    scientificObjectsService
      .deleteScientificObject(uri)
      .then(() => {
        this.$router.push({
          path: "/scientific-objects",
        });
      })
      .catch(this.$opensilex.errorHandler);
  }

  customCoordinatesText(position: any): string {

    if (!position) {
      return undefined;
    }

    let customCoordinates = "";

    if (position.x) {
      customCoordinates += "X:" + position.x;
    }
    if (position.y) {
      if (customCoordinates.length > 0) {
        customCoordinates += ", ";
      }
      customCoordinates += "Y:" + position.y;
    }
    if (position.z) {
      if (customCoordinates.length > 0) {
        customCoordinates += ", ";
      }
      customCoordinates += "Z:" + position.z;
    }

    if (customCoordinates.length == 0) {
      return undefined;
    }
    return customCoordinates;
  }
}
</script>

<i18n>
en:
  Event:
    lastPosition: Last position
fr:
  Event:
    lastPosition: Dernière position
</i18n>