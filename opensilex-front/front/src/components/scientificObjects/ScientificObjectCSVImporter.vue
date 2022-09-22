<template>
  <div>
    <opensilex-OntologyCsvImporter
      ref="importForm"
      :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
      :validateCSV="validateCSV"
      :uploadCSV="uploadCSV"
      @csvImported="displayReport($event)"
    >
      <template v-slot:icon>
        <opensilex-Icon icon="ik#ik-target" class="icon-title" />
      </template>
      <template v-slot:help>
        <opensilex-ScientificObjectImportHelp></opensilex-ScientificObjectImportHelp>
      </template>
      <template v-slot:generator>
        <b-col cols="2">
          <opensilex-Button
            class="mr-2 greenThemeColor"
            :small="false"
            @click="templateGenerator.show()"
            icon
            label="DataView.buttons.generate-template"
          ></opensilex-Button>
          <opensilex-ScientificObjectCSVTemplateGenerator
            ref="templateGenerator"
          ></opensilex-ScientificObjectCSVTemplateGenerator>
        </b-col>
      </template>
    </opensilex-OntologyCsvImporter>
    <b-modal
      ref="resultModal"
      :title="$t('ScientificObjectCSVImporter.result-title')"
      ok-only
      @ok="$emit('csvImported')"
    >
      <template v-slot:modal-header>
        <b-row class="mt-1" style="width: 100%">
          <b-col cols="10">
            <i>
              <h4>
                <opensilex-Icon icon="fa#list" />
                {{ $t("ScientificObjectCSVImporter.result-title") }}
              </h4>
            </i>
          </b-col>
        </b-row>
      </template>
      <template>
        <p class="validation-confirm-container">
          {{
            $tc(
              "ScientificObjectCSVImporter.objects-imported",
              nbLinesImported,
              { count: nbLinesImported }
            )
          }}
        </p>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";

@Component
export default class ScientificObjectCSVImporter extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  @Ref("importForm") readonly importForm!: any;
  @Ref("templateGenerator") readonly templateGenerator!: any;
  @Ref("resultModal") readonly resultModal!: any;

  @Prop({
      default: undefined
  })
  experimentURI;

  get user() {
    return this.$store.state.user;
  }

  get lang() {
    return this.$store.state.lang;
  }

  show() {
    this.importForm.show();
  }

  nbLinesImported = 0;

  validateCSV(csvFile) {
    return this.$opensilex.uploadFileToService(
      "/core/scientific_objects/import_validation",
      {
        description: {
            experiment: this.experimentURI
        },
        file: csvFile,
      }
    );
  }

  uploadCSV(validationToken, csvFile) {
    return this.$opensilex.uploadFileToService(
      "/core/scientific_objects/import",
      {
        description: {
            experiment: this.experimentURI
        },
        file: csvFile,
      }
    );
  }

  displayReport(uploadResponse) {
    this.nbLinesImported = uploadResponse.result.nb_lines_imported;
    this.resultModal.show();
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    ScientificObjectCSVImporter:
        result-title: Insertion report
        objects-imported: No scientific objects imported | 1 scientific object imported | {count} scientific objects imported
fr:
    ScientificObjectCSVImporter:
        result-title: Rapport de l'insertion
        objects-imported: Aucun objet scientifique importé | 1 objet scientifique importé | {count} objets scientifiques importés
</i18n>