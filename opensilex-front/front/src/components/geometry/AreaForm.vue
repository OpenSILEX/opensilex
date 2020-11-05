<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
        :editMode="editMode"
        :generated.sync="uriGenerated"
        :uri.sync="form.uri"
        helpMessage="component.common.uri.help-message"
        label="AreaForm.uri"
    ></opensilex-UriForm>

    <div class="row">
      <div class="col-lg-6">
        <!-- name -->
        <opensilex-InputForm
            :required="true"
            :value.sync="form.name"
            label="component.experiment.label"
            type="text"
        ></opensilex-InputForm>
      </div>
    </div>
    <!--    <div class="row">-->
    <!--      <div class="col-lg-6">-->
    <!--        &lt;!&ndash; selected area &ndash;&gt;-->
    <!--        <opensilex-RadioGroupForm-->
    <!--            :options="options"-->
    <!--            :required="true"-->
    <!--            :value.sync="form.areaType"-->
    <!--            title="radioForm"-->
    <!--        ></opensilex-RadioGroupForm>-->
    <!--      </div>-->

    <!--      <div class="col-lg-6" v-if="form.areaType === 'event'">-->
    <!--        &lt;!&ndash; confidential &ndash;&gt;-->
    <!--        <opensilex-CheckboxForm-->
    <!--            :value.sync="form.isPublic"-->
    <!--            helpMessage="AreaForm.comment-help"-->
    <!--            title="AreaForm.confidential"-->
    <!--        ></opensilex-CheckboxForm>-->
    <!--      </div>-->
    <!--    </div>-->

    <!--    <div v-if="form.areaType === 'perennial'">-->

    <opensilex-SelectForm
        :label="$t('AreaForm.areaType')"
        :multiple="false"
        :options="optionsArea"
        :required="false"
        :selected.sync="form.optionAreaType"
    ></opensilex-SelectForm>

    <!--    </div>-->

    <!--    <div v-if="form.areaType === 'event'">-->
    <!--      &lt;!&ndash; trial &ndash;&gt;-->
    <!--      <opensilex-InputForm-->
    <!--          :required="true"-->
    <!--          :value.sync="form.trial"-->
    <!--          disabled-->
    <!--          label="AreaForm.trial"-->
    <!--          type="text"-->
    <!--      ></opensilex-InputForm>-->

    <!--      <div class="row">-->
    <!--        <div class="col-lg-6">-->
    <!--          &lt;!&ndash; eventType &ndash;&gt;-->
    <!--          <opensilex-EventType-->
    <!--              :required="true"-->
    <!--              :type.sync="form.eventType"-->
    <!--          ></opensilex-EventType>-->
    <!--        </div>-->

    <!--        <div class="col-lg-6">-->
    <!--          &lt;!&ndash; subject &ndash;&gt;-->
    <!--          <opensilex-Subject-->
    <!--              :baseType="$opensilex.Oeso.EXPERIMENT_TYPE_URI"-->
    <!--              :required="true"-->
    <!--              :type.sync="form.subject"-->
    <!--          ></opensilex-Subject>-->
    <!--        </div>-->
    <!--      </div>-->

    <!--      &lt;!&ndash; period &ndash;&gt;-->
    <!--      <div class="row">-->
    <!--        &lt;!&ndash; start date &ndash;&gt;-->
    <!--        <div class="col-lg-2">-->
    <!--          <opensilex-InputForm-->
    <!--              :max="form.endDate"-->
    <!--              :required="true"-->
    <!--              :value.sync="form.startDate"-->
    <!--              label="component.common.startDate"-->
    <!--              type="date"-->
    <!--          ></opensilex-InputForm>-->
    <!--        </div>-->

    <!--        &lt;!&ndash; end date &ndash;&gt;-->
    <!--        <div class="col-lg-2">-->
    <!--          <opensilex-InputForm-->
    <!--              :min="form.startDate"-->
    <!--              :value.sync="form.endDate"-->
    <!--              label="component.common.endDate"-->
    <!--              type="date"-->
    <!--          ></opensilex-InputForm>-->
    <!--        </div>-->

    <!--        &lt;!&ndash; or &ndash;&gt;-->
    <!--        <div class="col-lg-3">-->
    <!--          <p>{{ $t('AreaForm.or') }}</p>-->
    <!--          {{ calculatesDifferenceDate(form.startDate, form.endDate) }}-->
    <!--        </div>-->

    <!--        &lt;!&ndash; duration &ndash;&gt;-->
    <!--        <div class="col-lg-2">-->
    <!--          <opensilex-InputForm-->
    <!--              :value.sync="form.duration"-->
    <!--              label="component.common.duration"-->
    <!--              type="number"-->
    <!--          ></opensilex-InputForm>-->
    <!--        </div>-->

    <!--        &lt;!&ndash; units of time &ndash;&gt;-->
    <!--        <div class="col-lg-2">-->
    <!--          <opensilex-UnitsOfTime-->
    <!--              :value.sync="form.unitsTime"-->
    <!--              label="component.common.unitsTime"-->
    <!--          ></opensilex-UnitsOfTime>-->

    <!--        </div>-->
    <!--      </div>-->

    <!--      &lt;!&ndash; annotation &ndash;&gt;-->
    <!--      <opensilex-TextAreaForm-->
    <!--          :value.sync="form.annotation"-->
    <!--          helpMessage="AreaForm.area-help"-->
    <!--          label="AreaForm.area"-->
    <!--          placeholder="AreaForm.area-placeholder"-->
    <!--      ></opensilex-TextAreaForm>-->
    <!--    </div>-->

    <!-- file input -->
    <!--    <opensilex-FileInputForm-->
    <!--        :file.sync="form.file"-->
    <!--        label="AreaForm.selectFile"-->
    <!--    ></opensilex-FileInputForm>-->

    <!-- comment -->
    <opensilex-TextAreaForm
        :value.sync="form.comment"
        helpMessage="AreaForm.comment-help"
        label="AreaForm.comment"
        placeholder="AreaForm.comment-placeholder"
    ></opensilex-TextAreaForm>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import {OntologyService} from "opensilex-core/api/ontology.service";

@Component
export default class AreaForm extends Vue {
  $opensilex: any;
  service: OntologyService;
  $store: any;
  $i18n: any;
  uriGenerated = true;
  optionsArea: { label: string; id: string }[] = [];

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
        author: null,
        uri: null,
        name: null,
        subject: null,
        // unitsTime: null,
        // file: null,
        optionAreaType: null,
        // duration: 0,
        // startDate: "",
        // endDate: "",
        // annotation: "",
        comment: "",
        // areaType: "",
        // trial: "",
        type: "",
        geometry: [],
        properties: [],
        exactMatch: [],
        closeMatch: [],
        broader: [],
        narrower: [],
      };
    }
  })
  form;
  // options: { text: string; value: string }[] = [
  //   // eventArea
  //   {text: 'AreaForm.eventArea', value: 'event'},
  //
  //   // perennialArea
  //   {text: 'AreaForm.perennialArea', value: 'perennial'},
  // ];

  get user() {
    return this.$store.state.user;
  }

  get languageCode(): string {
    this.loadTypesArea()
    return this.$opensilex.getLocalLangCode();
  }

  // retrieves zone subclasses in ontology
  loadTypesArea() {
    this.service = this.$opensilex.getService(
        "opensilex.OntologyService"
    );

    let optionsArea: { label: string; id: string }[] = [];

    this.service.getSubClassesOf(
        "http://www.opensilex.org/vocabulary/oeso#PerenialArea",
        true
    )
        .then(
            (http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
              const res = http.response.result;
              res.forEach(({name, uri}) => {
                optionsArea.push({id: uri, label: name});
              });
            }
        )
        .catch(this.$opensilex.errorHandler);
    optionsArea.forEach((id) => {
      console.log(id);
    });

    this.optionsArea = optionsArea;
  }

  reset() {
    this.uriGenerated = true;
  }

  // // Calculates the difference between the start date and the end date
  // calculatesDifferenceDate(startDate, endDate) {
  //   let date1 = new Date(startDate);
  //   let date2 = new Date(endDate);
  //   // difference in hours
  //   let time_diff = date2.getTime() - date1.getTime();
  //
  //   // difference in days
  //   let days_Diff = time_diff / (1000 * 3600 * 24) + 1;
  //   // display the difference
  //   this.form.duration = days_Diff;
  //   this.form.unitsTime = "days";
  //
  //   if (days_Diff < 0)
  //     alert(this.$t('AreaForm.problemDate'));
  // }

  getEmptyForm() {
    let names = {};
    let lang = this.languageCode;
    let defaultLang = "en";
    names[lang] = "";
    names[defaultLang] = "";
    return {
      author: null,
      uri: null,
      name: null,
      subject: null,
      // unitsTime: null,
      // file: null,
      optionAreaType: null,
      // duration: 0,
      // startDate: "",
      // endDate: "",
      // annotation: "",
      comment: "",
      // areaType: "",
      // trial: "",
      type: "",
      geometry: [],
      properties: [],
      exactMatch: [],
      closeMatch: [],
      broader: [],
      narrower: [],
    };
  }

  setUri(uri: string) {
    this.form.uri = uri;
  }

  createGeometry() {
    return new Promise((resolve, reject) => {
      return this.$emit("onCreate", this.form, result => {
        if (result instanceof Promise) {
          return result
              .then(uri => {
                return uri;
              })
              .catch(reject);
        } else {
          return resolve(result);
        }
      });
    });
  }

  create(form) {
    form.geometry = this.$store.state.zone.geometry;
    if (form.optionAreaType !== null)
      form.type = form.optionAreaType;
    else
      form.type = "vocabulary:Zone";

    // let author = {
    //   "rdfType": null,
    //   "relation": "http://purl.org/dc/terms/creator",
    //   "value": this.user.email
    // };
    //
    // form.properties.push(author);

    return this.$opensilex
        .getService("opensilex.AreaService")
        .createArea(form)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          let uri = http.response.metadata.datafiles[0];
          console.debug("Area of Geometry created", uri);
          form.uri = uri;
        })
        .catch(error => {
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
}
</script>

<style lang="scss" scoped>
@import "~vue-tabulator/dist/scss/bootstrap/tabulator_bootstrap4";

P {
  text-align: center;
}
</style>

<i18n>
en:
  AreaForm:
    author: Author
    label: Area
    add-button: Input metadata
    deleteLastAreaNotValidatedButton: Delete the last non-validated area
    add: Draw an area
    update: Update a perennial zone
    uri: Area URI
    comment: comment
    comment-help: Description associated with this geometry (protocol quantity)
    comment-placeholder: Protocol n°1289 - Amount of water 5 ml/Days.
    area: annotation
    area-help: Annotation associated with this geometry
    area-placeholder: Description of the nature of the event.
    eventArea: Event area
    perennialArea: Perennial area
    selectFile: Select a file
    confidential: Confidential
    confidential-help: The data is confidential
    trial: Trial
    eventAnnotation: Would you like to annotate ?
    no: no
    problem: Problem
    management: Management
    or: Or
    problemDate: problem (end date starts before start date)
    choiceTypeGeometriesDrawn: Choice type geometries to be drawn
    areaType: type

fr:
  AreaForm:
    author: Auteur
    label: Zone
    add-button: Saisir métadonnée
    deleteLastAreaNotValidatedButton: Supprimer la dernier zone non validé
    add: Dessiner une zone
    update: Mettre à jour une zone pérenne
    uri: URI de Zone
    comment: description
    comment-help: Description associée à cette géométrie (protocole quantité)
    comment-placeholder : Protocole n°1289 - Apport d'eau de 5 ml/jour.
    area: Annotation
    area-help: Zone associée à cette géométrie
    area-placeholder: Description de la nature de l'événement.
    eventArea: Zone d'événements
    perennialArea: Zone pérenne
    selectFile: Sélectionner un fichier
    confidential: Confidentiel
    confidential-help: Les données sont confidentielles
    trial: Essai
    eventAnnotation: Voulez-vous annoter ?
    no: non
    problem: Problème
    management: Gestion
    or: Ou
    problemDate: problème (la date de fin commence avant la date de début)
    choiceTypeGeometriesDrawn: Choix du type de géométrie à dessiner
    areaType: type
</i18n>
