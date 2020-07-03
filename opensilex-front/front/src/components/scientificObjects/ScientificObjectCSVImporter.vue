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
              v-model="this.content"
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
  OntologyService
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
    columns: []
  };

  requiredHeaders = [];

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
    this.content = [];

    this.tabulatorOptions.columns = [
      {
        title: "URI",
        field: "uri",
        editor: true
      },
      {
        title: "Name",
        field: "name",
        editor: true
      },
      {
        title: "Parent URI",
        field: "parent",
        editor: true
      }
    ];

    this.requiredHeaders = [];
    this.tabulatorOptions.columns.forEach(colDefinition => {
      this.requiredHeaders.push(colDefinition.field);
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
