<template>
  <div class="container-fluid">

    <opensilex-PageContent>
      <!-- type -->
      <opensilex-TypeForm
          :type.sync="form.rdf_type"
          :baseType="$opensilex.Oeso.DATAFILE_TYPE_URI"
          helpMessage="DataFileForm.type-help"    
          :required="true"
      ></opensilex-TypeForm>

      
      <b-form v-if="form.rdf_type">
                
        <!-- format -->
        <b-form-group label="Format">
          <b-form-radio-group v-model="selectedFormat">
            <b-form-radio
              v-for="option in formats"
              :key="option.id"
              :value="option.id"
            > {{ option.label }}
            </b-form-radio>
          </b-form-radio-group>
        </b-form-group>


        <!-- provenance -->
        <opensilex-ProvenanceSelector
          ref="provenanceSelector"
          :provenances.sync="form.provenance"
          label="ExperimentData.provenance"
          :multiple="false"
          @clear="reset()"
          @select="(selected) => form.provenance.id = selected"
          :actionHandler="user.hasCredential(credentials.CREDENTIAL_PROVENANCE_MODIFICATION_ID)
            ? showProvenanceCreateForm
            : undefined"
          :required="true"
        ></opensilex-ProvenanceSelector>

        <!-- experiment -->
        <opensilex-ExperimentSelector
          v-if="!tabExperimentMode"
          label="DataFileForm.experiment"
          :experiments.sync="form.experiments"
          :required="true"
        ></opensilex-ExperimentSelector>

        <!-- File source -->
        <b-form-group label="Source">
          <b-form-radio-group v-model="selectedSource">
            <b-form-radio
              v-for="option in sources"
              :key="option.id"
              :value="option.id"
            > {{ option.label }}
            </b-form-radio>
          </b-form-radio-group>
        </b-form-group>

        <!-- File -->
        <opensilex-FileInputForm
          v-if="!editMode && selectedSource == 'file'"
          :file.sync="form.file"
          label="DataFileForm.fileDX"
          type="file"
          browse-text="DataFileForm.browse"
          :required="true"
          rules="size:100000"
          :helpMessage="helpMessageFile"
        ></opensilex-FileInputForm>

        <!-- Source -->
        <opensilex-InputForm
          v-if="!editMode && selectedSource == 'external'"
          label="DataFileForm.external-source"
          type="text"
          :value.sync="form.file"
          :required="true"
        >
        </opensilex-InputForm>
        <!-- date -->
        <opensilex-DateForm
            v-if="this.selectedFormat == 'all' || selectedSource == 'external'"
            :value.sync="form.date"
            helpMessage="DocumentForm.date-help"
            label="DocumentForm.date"
            :required="true"
        ></opensilex-DateForm>

        <!-- targets -->
        <opensilex-InputForm
          v-if="this.selectedFormat == 'all' || selectedSource == 'external'"
          :value.sync="form.target"
          :baseType="$opensilex.Oeso.targets"
          label="DocumentForm.targets"
          helpMessage="DocumentForm.targets-help"
          type="text"
        ></opensilex-InputForm>
      </b-form>

      <opensilex-ModalForm
        ref="provenanceForm"
        component="opensilex-ProvenanceForm"
        createTitle="ProvenanceView.add"
        editTitle="ProvenanceView.update"
        icon="fa#seedling"
        modalSize="lg"
        @hide="validateProvenanceForm='true'"
        :initForm="initForm"
        :successMessage="successMessage"
        :validationDisabled="validateProvenanceForm"
        @onCreate="afterCreateProvenance"
      ></opensilex-ModalForm>

    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import { ScientificObjectsService } from "opensilex-core/index";
import Vue from "vue";
// @ts-ignore
import {DataService} from "opensilex-core/api/data.service";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import {OpenSilexStore} from "../../../../../../opensilex-front/front/src/models/Store";
import VueI18n from "vue-i18n";

@Component
export default class DataFileForm extends Vue {
  $opensilex: any;
  service: DataService;
  $store: OpenSilexStore;
  serviceOS: ScientificObjectsService;
  $t: typeof VueI18n.prototype.t;
  file;
  uriGenerated = true;
  selectedProvenance: any = null;
  selectedFormat: string = "DX";
  selectedSource: string = "file";
  validateProvenanceForm: boolean = true;
  provenance = null;
  @Ref("provenanceForm") readonly provenanceForm!: any;
  @Ref("provenanceSelector") readonly provenanceSelector!: any;

  @Prop() data!: any;
  get tabExperimentMode(): boolean {
    return this.data?.tabExperimentMode === true;
  }

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
       rdf_type: null,
        provenance: null,
        experiments: [],
        file: null
      };
    }
  })
  form: any;

  created() {
    this.serviceOS = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );

  }

  formats = [
    {"id":"all", "label":"All file formats"},
    {"id":"DX", "label":"JCAMP-DX"}, 
    {"id":"CSV", "label":"CSV for Spectra"}, 
  ];

  sources = [
    {"id":"file", "label":"File source"}, 
    {"id":"external", "label":"External source"}
  ]

  get user() {
    return this.$store.state.user;
  } 

  getEmptyForm() {
    return {
       rdf_type: null,
        provenance: null,
        experiments: [],
        file: null
      };
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  reset() {
    this.uriGenerated = true;
  }

  async create(form) {
    const canCheckTarget = this.selectedSource === 'external' || this.selectedFormat === 'all';
    if (canCheckTarget && this.form.target) {
      const valid = await this.checkOSinExperiment();
      if (!valid) return;
    }

    // DX FILE CREATION
    if (this.selectedFormat == 'DX' && this.selectedSource == 'file') {
      return this.$opensilex.uploadFileToService(
          "/core/datafiles/upload-dx",
          {
              rdf_type: this.form.rdf_type,
              provenance: this.form.provenance,
              experiments: [this.form.experiments],
              file: this.form.file
          },
          null,
          false
      )
      .then(uploadResponse => {
          if (!uploadResponse || !uploadResponse.result) {
              throw new Error("File upload failed.");
          }
          return this.$opensilex.getService("opensilex.DataService")
              .postDataFilePaths(uploadResponse.result);
      })
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        form.uri = uri;
        console.debug("experiment created", uri);
        this.$emit("onCreate", form);
        this.$opensilex.showSuccessToast("File uploaded and processed successfully.");
        console.debug("Datafile created", uri);
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("DataFile already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("DataFileForm.error.datafile-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
    }

    // CSV SPECTRA FILE CREATION
    if (this.selectedFormat == 'CSV' && this.selectedSource == 'file') {
      return this.$opensilex.uploadFileToService(
          "/core/datafiles/upload-spectra-csv",
          {
              rdf_type: this.form.rdf_type,
              provenance: this.form.provenance,
              experiments: [this.form.experiments],
              file: this.form.file
          },
          null,
          false
      )
      .then(uploadResponse => {
          if (!uploadResponse || !uploadResponse.result) {
              throw new Error("File upload failed.");
          }
          return this.$opensilex.getService("opensilex.DataService")
              .postDataFilePaths(uploadResponse.result);
      })
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        form.uri = uri;
        console.debug("experiment created", uri);
        this.$emit("onCreate", form);
        this.$opensilex.showSuccessToast("File uploaded and processed successfully.");
        console.debug("Datafile created", uri);
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("DataFile already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("DataFileForm.error.datafile-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
    }

    // ALL TYPE FILE CREATION
    if (this.selectedFormat == 'all' && this.selectedSource == 'file') {
      return this.$opensilex.uploadFileToService(
          "/core/datafiles",
          {

            description: {
              rdf_type: this.form.rdf_type,
              provenance: {"uri": this.form.provenance, "experiments": [this.form.experiments] },
              date: this.form.date,
              target: this.form.target
            },
            file: this.form.file
          },
          null,
          false
      )
      .then((http: any) => {
        let uri = http.result;
        console.debug("Datafile created", uri);
        form.uri = uri;
        return form;
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("DataFile already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("DataFileForm.error.datafile-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
    }

    // ALL TYPE EXTERNAL SOURCE CREATION
    if (this.selectedSource == 'external') {
      return this.$opensilex.getService("opensilex.DataService")
              .postDataFilePaths(
                [{
                  rdf_type: this.form.rdf_type,
                  provenance: {"uri": this.form.provenance, "experiments": [this.form.experiments] },
                  date: this.form.date,
                  target: this.form.target,
                  relative_path: this.form.file
                }]
              )
              .then((http: any) => {
                let uri = http.result;
                console.debug("Datafile created", uri);
                form.uri = uri;
                return form;
              })
              .catch((error) => {
                if (error.status == 409) {
                  console.error("DataFile already exists", error);
                  this.$opensilex.errorHandler(
                    error,
                    this.$t("DataFileForm.error.datafile-already-exists")
                  );
                } else {
                  this.$opensilex.errorHandler(error);
                }
              });
            }
  }

  get helpMessageFile(){
    let helpMessageFile = null;
    if (this.selectedFormat == 'DX') {
      helpMessageFile = "DataFileForm.fileDX-help";
    }
    if (this.selectedFormat == 'CSV') {
      helpMessageFile = "DataFileForm.fileSpectraCSV-help";
    }

    return helpMessageFile
    
  }

  showProvenanceCreateForm() {
    this.validateProvenanceForm = false;
    this.provenanceForm.showCreateForm();
  }

  afterCreateProvenance(data) {
    this.provenance = data;
    this.provenanceSelector.select(data.uri);
  }

  async checkOSinExperiment() {
    let contextURI = this.form.experiments;
    let target = this.form.target;

    try {
      const http = await this.serviceOS.getScientificObjectsListByUris(contextURI, [target]);

      const foundObjects = http?.response?.result || [];

      if (foundObjects.length === 0) {
        this.$opensilex.showErrorToast(
          this.$t("DataFileForm.error.object-not-in-experiment")
        );
        return false;
      }
      return true;

    } catch (error) {
      console.error("Error checking the object :", error);
      this.$opensilex.errorHandler(error);
      return false;
    }
  }
}

</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  DataFileForm:
    browse: Browse
    error:
      datafile-already-exists: Datafile already exists
      file-name-too-long: File name is too long
      object-not-in-experiment: Scientific Object is not in this experiment
    experiment: Experiment
    external-source: External source
    fileDX: File
    fileDX-help : Insert a file in JCAMP-DX, JDX, DX format. A file can contain an infinite number of samples, provided you have created the OS in envibis beforehand.
    fileSpectraCSV-help: Insert a spectra file in CSV format with tabulation separator. A file can contain an infinite number of samples, provided you have created the OS in envibis beforehand.
    form-selectFormat-placeholder: Select datafile format to insert
    type-help: Datafile type
    
fr:
  DataFileForm:
    browse: Parcourir
    error:
      datafiles-already-exists: Le fichier de données existe déjà
      file-name-too-long: Le nom du fichier est trop long
      object-not-in-experiment: L'objet scientifique n'est pas inclus dans cette expérience
    experiment: Expérimentation
    external-source: Source externe
    fileDX: Fichier
    fileDX-help: Insérer un fichier au format JCAMP-DX, JDX, DX Un fichier peut comporter une infinité d'échantillons à conditions d'avoir créé au préalable les OS dans envibis.
    fileSpectraCSV-help: Insérer un fichier de spectres au format CSV avec le séparateur tabulation. Un fichier peut comporter une infinité d'échantillons à conditions d'avoir créé au préalable les OS dans envibis.
    form-selectFormat-placeholder: Sélectionner le format du fichier de données à insérer
    type-help: Type de datafile
    
</i18n>
