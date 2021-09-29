<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
        :editMode="editMode"
        :generated.sync="uriGenerated"
        :uri.sync="form.uri"
        helpMessage="component.common.uri-help-message"
        label="AreaForm.uri"
    ></opensilex-UriForm>

    <div class="row">
      <div class="col-lg-6">
        <!-- name -->
        <opensilex-InputForm
            :required="true"
            :value.sync="form.name"
            label="component.experiment.label"
            placeholder="AreaForm.name-placeholder"
            type="text"
        ></opensilex-InputForm>
      </div>
    </div>

    <!-- area type -->
    <b-form-group v-if="toggleAreaType" :label="$t('AreaForm.form-areaType')" v-slot="{ ariaDescribedby }">
      <b-form-radio v-model="toggleAreaType" :aria-describedby="ariaDescribedby" name="perennial-zone" value="perennial-zone">{{ $t('AreaForm.form-areaType-perennial-zone') }}</b-form-radio>
      <b-form-radio v-model="toggleAreaType" :aria-describedby="ariaDescribedby" name="temporal-zone" value="temporal-zone">{{ $t('AreaForm.form-areaType-temporal-zone') }}</b-form-radio>
    </b-form-group>


    <!-- rdf type -->
    <opensilex-TypeForm
        v-show="toggleAreaType == 'perennial-zone'"
        :baseType="$opensilex.Oeso.STRUCTURAL_AREA_TYPE_URI"
        :placeholder="$t('AreaForm.form-rdfType-placeholder')"
        :required="true"
        :ignoreRoot="false"
        :type.sync="form.rdf_type"
    ></opensilex-TypeForm>
    <opensilex-TypeForm
        v-show="toggleAreaType == 'temporal-zone'"
        :baseType="baseType"
        :type.sync="form.rdf_type"
        :required="true"
        :ignoreRoot="false"
        :placeholder="$t('AreaForm.form-rdfType-placeholder')"
        @update:type="typeSwitch"
    ></opensilex-TypeForm>

    <div v-if="toggleAreaType == 'temporal-zone'">
      <!-- instant -->
      <div class="row">
          <div class="col">

              <opensilex-FormField
                  label="Event.is-instant"
                  helpMessage="Event.is-instant-help"
              >
                  <template v-slot:field="field">
                      <b-form-checkbox v-model="form.is_instant" switch>
                      </b-form-checkbox>
                  </template>
              </opensilex-FormField>
          </div>
      </div>
      <!-- date -->
      <div class="row">
          <div class="col" v-if="! form.is_instant">
              <opensilex-DateTimeForm
                  :value.sync="form.start"
                  label="Event.start"
                  :minDate="form.minDate"
                  :maxDate="form.end"
                  :required="startRequired"
                  @update:value="updateRequiredProps"
                  helpMessage="Event.start-help"
              ></opensilex-DateTimeForm>
          </div>

          <div class="col">
              <opensilex-DateTimeForm
                  :value.sync="form.end"
                  label="Event.end"
                  :minDate="form.start"
                  :maxDate="form.maxDate"
                  :required="endRequired"
                  @update:value="updateRequiredProps"
                  helpMessage="Event.end-help"
              ></opensilex-DateTimeForm>
          </div>

      </div>

    </div>

    <!-- description -->
    <opensilex-TextAreaForm
        :value.sync="form.description"
        helpMessage="AreaForm.description-help"
        label="AreaForm.description"
        placeholder="AreaForm.description-placeholder"
    ></opensilex-TextAreaForm>

    <div v-if="toggleAreaType == 'temporal-zone'">

      <slot v-bind:form="form"></slot>

      <div v-for="(relation, index) in typeRelations" v-bind:key="index">
          <component
              :is="getInputComponent(relation.property)"
              :property="relation.property"
              :label="relation.property.name"
              :required="relation.property.is_required"
              :multiple="relation.property.is_list"
              :value.sync="relation.value"
              @update:value="updateRelation($event,relation.property)"
          ></component>
      </div>
      <opensilex-MoveForm v-if="isMove()" :form.sync="form"></opensilex-MoveForm>
    </div>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
// @ts-ignore
import {OntologyService} from "opensilex-core/api/ontology.service";
// @ts-ignore
import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
// @ts-ignore
import { EventGetDTO } from 'opensilex-core/model/eventGetDTO';
import { VueJsOntologyExtensionService } from "../../lib";

@Component
export default class AreaForm extends Vue {
  $opensilex: any;
  service: OntologyService;
  $store: any;
  $i18n: any;
  uriGenerated = true;
  baseType: string = "";
  ontologyService: OntologyService;
  vueOntologyService: VueJsOntologyExtensionService;
  startRequired: Boolean = true;
  endRequired: Boolean = true;
  typeModel = null;

  @Prop()
  editMode;
  @Prop()
  title: string;
  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        relations: [],
        targets: [],
        areaType: "perennial-zone",
        rdf_type: null,
        start: null,
        end: null,
        is_instant: false,
        description: "",
        geometry: [],
        targets_positions: [{
          target: undefined,
          position: {
            point: undefined,
            x: undefined,
            y: undefined,
            z: undefined,
            text: undefined,
          }
        }],
        minDate:null,
        maxDate:null
      };
    },
  })
  form;

  get user() {
    return this.$store.state.user;
  }

  get languageCode(): string {
    return this.$opensilex.getLocalLangCode();
  }

  get toggleAreaType() {
    return this.form.areaType;
  }

  set toggleAreaType(value: String) {
    this.form.areaType = value;
    this.form.rdf_type = null;
  }

  get typeRelations() {

    let internalTypeProperties = [];

    if (this.typeModel) {
        for (let i in this.typeModel.data_properties) {
            let dataProperty = this.typeModel.data_properties[i];
            if (dataProperty.property != "rdfs:label") {

                let relation = this.form.relations.find(relation => relation.property == dataProperty.property);

                internalTypeProperties.push({
                    property: dataProperty,
                    value: relation.value
                });
            }
        }

        for (let i in this.typeModel.object_properties) {

            let objectProperty = this.typeModel.object_properties[i];
            let relation = this.form.relations.find(relation => relation.property == objectProperty.property);

            internalTypeProperties.push({
                property: objectProperty,
                value: relation.value
            });
        }
    }
    return internalTypeProperties;
  }

  reset() {
    this.startRequired = true;
    this.endRequired = true;
    this.uriGenerated = true;
    this.typeModel = null;
    this.form = this.getEmptyForm();
  }

  resetRdfType(value) {
    this.form.rdfType = null;
  }

  created() {
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
    this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
    this.baseType = this.$opensilex.Oeev.EVENT_TYPE_URI;
  }

  isMove(): boolean {
    if (!this.form) {
      return false;
    }
    return this.form.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_URI
    || this.form.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_PREFIXED_URI
  }

  private getEventFromUri(uri) {
    const eventsService = "EventsService";
    return new Promise((resolve) => {
      this.$opensilex
      .getService(eventsService)
      .searchEvents(undefined, undefined, undefined, uri)
      .then((http: HttpResponse<OpenSilexResponse<EventGetDTO>>) => {
        const res = http.response.result[0] as any;
        resolve(res);
      })
      .catch(this.$opensilex.errorHandler);
    });
  }

  update(form) {
    console.debug(form);
    return this.$opensilex
        .getService("opensilex.AreaService")
        .updateArea(form)
        .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
          let uri = http.response.result;
          if (form.rdf_type == 'vocabulary:TemporalArea') {
            return this.getEventFromUri(uri)
            .then((event: any) => {
              event.name = form.name;
              event.description = form.description;
              event.targets = [uri];
              console.debug('Event', event);
              return this.$opensilex
              .getService('EventsService')
              .updateEvent(event)
              .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                let uriTemporalArea = http.response.result;
                console.debug("Temporal Area updated", uriTemporalArea);
                return uri;
              })
            })
          } else {
            console.debug("Area updated", uri);
            return uri;
          }
        })
        .catch(this.$opensilex.errorHandler);
  }

  getEmptyForm() { 
    return {
      uri: null,
      name: null,
      relations: [],
      targets: [],
      areaType: "perennial-zone",
      rdf_type: null,
      start: null,
      end: null,
      is_instant: false,
      description: "",
      geometry: [],
      targets_positions: [{
          target: undefined,
          position: {
            point: undefined,
            x: undefined,
            y: undefined,
            z: undefined,
            text: undefined,
          }
        }],
      minDate:null,
      maxDate:null
    };
  }

  updateRequiredProps() {
        if (this.form.is_instant) {
            this.endRequired = true;
        } else {
            if (this.form.start) {
                this.startRequired = true;
                this.endRequired = true;
            } else {
                this.startRequired = true;
                this.endRequired = true;
            }
        }
    }

  getInputComponent(property) {
    if (property.input_components_by_property && property.input_components_by_property[property.property]) {
        return property.input_components_by_property[property.property];
    }
    return property.input_component;
  }

  updateRelation(newValue,property){
    let relation = this.form.relations.find(relation =>
        relation.property == property.property
    );

    relation.value = newValue;
  }

  setUri(uri: string) {
    this.form.uri = uri;
  }

  createArea(form) {
    return this.$opensilex
      .getService("opensilex.AreaService")
      .createArea(form)
      .then((http: HttpResponse<OpenSilexResponse>) => {
        let uri = http.response.metadata.datafiles[0];
        console.debug("Area of Geometry created", uri);
        form.uri = uri;
        return uri;
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("Area of Geometry already exists", error);
          this.$opensilex.errorHandler(
              error,
              this.$i18n.t("component.user.errors.user-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  createTemporalArea(form) {
    if (form.is_instant) {
      form.start = null
    }
    let eventRdf = form.rdf_type;
    form.rdf_type = "vocabulary:TemporalArea";
    return this.$opensilex
      .getService("opensilex.AreaService")
      .createArea(form)
      .then((http: HttpResponse<OpenSilexResponse>) => {
        let uri = http.response.metadata.datafiles[0];
        console.debug("Area of Geometry created", uri);
        form.targets = [uri];
        form.rdf_type = eventRdf;
        form.uri = null;
        return this.$opensilex
          .getService("EventsService")
          .createEvents([form])
          .then((http: HttpResponse<OpenSilexResponse>) => {
            uri = http.response.result.toString()
            console.debug("Events created", uri);
            return form.targets[0];
          }).catch((error) => {
            this.$opensilex
            .getService("opensilex.AreaService")
            .deleteArea(uri);
              if (error.status == 409) {
                  this.$opensilex.errorHandler(error, this.$i18n.t("component.user.errors.user-already-exists"));
              } else {
                  this.$opensilex.errorHandler(error,error.response.result.message);
              }
          });
      }).catch((error) => {
        if (error.status == 409) {
          console.error("Area of Geometry already exists", error);
          this.$opensilex.errorHandler(
              error,
              this.$i18n.t("component.user.errors.user-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      })
  }

  create(form) {
    form.geometry = this.$store.state.zone.geometry;

    if (form.description.length == 0) {
      form.description = null;
    }
    if (form.areaType == 'perennial-zone') {
      return this.createArea(form);
    } else {
      return this.createTemporalArea(form);
    }
  }

  typeSwitch(type) {

    if (!type || this.toggleAreaType == 'perennial-zone' || type == 'vocabulary:TemporalArea') {
        return;
    }
    this.vueOntologyService
      .getRDFTypeProperties(this.form.rdf_type, this.baseType)
      .then(http => {
          this.typeModel = http.response.result;
          if (!this.editMode) {
              let relations = [];
              for (let i in this.typeModel.data_properties) {
                  let dataProperty = this.typeModel.data_properties[i];
                  if (dataProperty.is_list) {
                      relations.push({
                          value: [],
                          property: dataProperty.property
                      });
                  } else {
                      relations.push({
                          value: undefined,
                          property: dataProperty.property
                      });
                  }
              }

              for (let i in this.typeModel.object_properties) {
                  let objectProperty = this.typeModel.object_properties[i];
                  if (objectProperty.is_list) {
                      relations.push({
                          value: [],
                          property: objectProperty.property
                      });
                  } else {
                      relations.push({
                          value: undefined,
                          property: objectProperty.property
                      });
                  }
              }

              this.form.relations = relations;
          }
      });
    }

}
</script>

<style lang="scss" scoped>
P {
  text-align: center;
}
</style>

<i18n>
en:
  AreaForm:
    uri: Area URI
    description: description
    description-help: Description associated with this geometry (protocol quantity)
    description-placeholder: Protocol n°1289 - Amount of water 5 ml/Days.
    name-placeholder: Limestone Mineral Zone, Southern Clay Zone, ...
    area: annotation
    area-help: Annotation associated with this geometry
    area-placeholder: Description of the nature of the event.
    eventArea: Event area
    structuralArea: Structural area
    selectFile: Select a file
    confidential: Confidential
    confidential-help: The data is confidential
    trial: Trial
    or: Or
    problemDate: problem (end date starts before start date)
    form-rdfType-placeholder: Please select a type
    form-areaType-perennial-zone: Perennial zone
    form-areaType-temporal-zone: Temporal zone
    form-areaType: Type of area

fr:
  AreaForm:
    uri: URI de Zone
    description: description
    description-help: Description associée à cette géométrie (protocole quantité)
    description-placeholder: Protocole n°1289 - Apport d'eau de 5 ml/jour.
    name-placeholder: Zone minérale calcaire, zone argileuse méridionale, ...
    area: Annotation
    area-help: Zone associée à cette géométrie
    area-placeholder: Description de la nature de l'événement.
    eventArea: Zone d'événements
    structuralArea: Zone structurelle
    selectFile: Sélectionner un fichier
    confidential: Confidentiel
    confidential-help: Les données sont confidentielles
    trial: Essai
    or: Ou
    problemDate: problème (la date de fin commence avant la date de début)
    form-rdfType-placeholder: Veuillez sélectionner un type de zone
    form-areaType-perennial-zone: Zone pérenne
    form-areaType-temporal-zone: Zone temporaire
    form-areaType: Type de zone
</i18n>
