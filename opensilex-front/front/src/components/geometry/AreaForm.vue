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
        :placeholder="$t('AreaForm.form-rdfType-placeholder')"
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

@Component
export default class AreaForm extends Vue {
  $opensilex: any;
  service: OntologyService;
  $store: any;
  $i18n: any;
  uriGenerated = true;
  optionsArea: { label: string; id: string }[] = [];
  baseType: string = "";
  ontologyService: OntologyService;
  startRequired: Boolean = true;
  endRequired: Boolean = true;

  @Prop()
  editMode;
  @Prop()
  title: string;
  @Prop({
    default: () => {
      let names = {};
      let defaultLang = "en";
      names[defaultLang] = "";
      return {
        uri: null,
        name: null,
        areaType: "perennial-zone",
        rdf_type: null,
        start: null,
        end: null,
        is_instant: false,
        description: "",
        geometry: [],
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

  reset() {
    this.uriGenerated = true;
  }

  resetRdfType(value) {
    this.form.rdfType = null;
  }

  created() {
    this.baseType = this.$opensilex.Oeev.EVENT_TYPE_URI;
  }

  update(form) {
    return this.$opensilex
        .getService("opensilex.AreaService")
        .updateArea(form)
        .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
          let uri = http.response.result;
          console.debug("Area updated", uri);

          return uri;
        })
        .catch(this.$opensilex.errorHandler);
  }

  getEmptyForm() {
    let names = {};
    let lang = this.languageCode;
    let defaultLang = "en";
    names[lang] = "";
    names[defaultLang] = "";
    return {
      uri: null,
      name: null,
      areaType: "perennial-zone",
      rdf_type: null,
      start: null,
      end: null,
      is_instant: false,
      description: "",
      geometry: [],
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

    if (form.areaType == 'perennial-zone') {
      return this.createArea(form);
    } else {
      return this.createTemporalArea(form);
    }
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
