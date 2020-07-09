import Component from "vue-class-component";

 <template>
  <div>
    <opensilex-CreateButton @click="show" label="ScientificObjectCSVImporter.import"></opensilex-CreateButton>
    <b-modal ref="ScientificObjectCSVImporter" @ok.prevent="importCSV" size="xl" :static="true">
      <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
      <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

      <template v-slot:modal-title>
        <i>
          <slot name="icon">
            <opensilex-Icon icon="fa#eye" class="icon-title" />
          </slot>
          <span>{{$t("ScientificObjectCSVImporter.import")}}</span>
        </i>
      </template>
      <!-- Type -->
      <opensilex-TypeForm
        :type.sync="scientificObjectType"
        @update:type="typeSwitch"
        :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
        :required="true"
        label="ScientificObjectCSVImporter.object-type"
        placeholder="ScientificObjectCSVImporter.object-type-placeholder"
      ></opensilex-TypeForm>

      <div v-if="scientificObjectType">
        <div class="row">
          <div class="col-md-4">
            <b-form-file
              size="sm"
              ref="inputFile"
              accept="text/csv, .csv"
              @change="csvUploaded"
              placeholder="Drop or select CSV file here..."
              drop-placeholder="Drop file here..."
            ></b-form-file>
          </div>
          <div class="col">
            <b-button>Download CSV template</b-button>
          </div>
        </div>
        <div class="row row-tabulator">
          <div class="col">
            <VueTabulator
              class="table-light table-bordered"
              v-model="content"
              :options="tabulatorOptions"
              placeholder="Import a CSV file "
            />
          </div>
        </div>
      </div>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  ExperimentsService,
  InfrastructuresService,
  ResourceTreeDTO,
  OntologyService,
  RDFClassDTO
} from "opensilex-core/index";
@Component
export default class ScientificObjectCSVImporter extends Vue {
  $opensilex: any;
  ontologyService: OntologyService;
  infraService: InfrastructuresService;

  @Ref("ScientificObjectCSVImporter")
  readonly ScientificObjectCSVImporter!: any;

  content = [];

  tabulatorOptions = {
    reactiveData:true,
    columns: []
  };

  scientificObjectType = null;

  @Prop()
  experimentUri;

  show() {
    this.ScientificObjectCSVImporter.show();
  }

  hide() {
    this.ScientificObjectCSVImporter.hide();
  }

  mounted() {
    this.ontologyService = this.$opensilex.getService(
      "opensilex-core.OntologyService"
    );
  }

  importCSV() {
    // this.xpService
    //   .setFacilities(this.uri, this.selected)
    //   .then(result => {
    //     this.$nextTick(() => {
    //       this.$emit("csvImported");
    //       this.hide();
    //     });
    //   })
    //   .catch(console.error);
  }

  typeSwitch() {

    this.ontologyService.getClass(this.scientificObjectType).then(http => {
      let classModel: RDFClassDTO = http.response.result;

      let columns = [
        {
          title: "URI",
          field: "uri",
          editor: true
        }
      ];

      for (let i in classModel.properties) {
        let property = classModel.properties[i];
        columns.push({
          title: property.label,
          field: property.uri,
          editor: true
        });
      }

      this.tabulatorOptions.columns = columns;
          this.content = [];

      console.error( this.tabulatorOptions );
    });
  }

  csvUploaded() {}
}
</script>

<style scoped lang="scss">
.row-tabulator {
  margin-top: 15px;
}
</style>


<i18n>
en:
  ScientificObjectCSVImporter:
    import: CSV Import
    object-type: object type
    object-type-placeholder: Type here to search in object types

fr:
  ScientificObjectCSVImporter:
    import: Import CSV
    object-type: type d'objet
    object-type-placeholder: Utiliser cette zone pour rechercher un type d'objet
</i18n>
