<template>
  <opensilex-SelectForm
      ref="selectForm"
      label="props-label"
      :multiple="true"
      placeholder="props-placeholder"
      :selected.sync="selectedProps"
      :options="optionsProps"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, PropSync} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {VueJsOntologyExtensionService, VueRDFTypeDTO, VueRDFTypePropertyDTO} from "../../lib";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class ScientificObjectPropertiesSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  vueOntologyService: VueJsOntologyExtensionService

  @PropSync("props",{ default: () => []})
  selectedProps;
  optionsProps = [];
  private langUnwatcher;
  created(){
    this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
    this.loadProps();
  }

  mounted() {
    this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        lang => {
          this.loadProps();
        }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  loadProps() {
      let soProperties: Array<VueRDFTypePropertyDTO> = [];
      this.vueOntologyService
          .getRDFTypeProperties(
              this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI,
              this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI)
          .then((http: HttpResponse<OpenSilexResponse<VueRDFTypeDTO>>) => {
              soProperties = [...http.response.result.data_properties, ...http.response.result.object_properties];
              //exclude properties : Name, Is Hosted
              let filteredProps :Array<VueRDFTypePropertyDTO> = soProperties.filter( prop =>
                  prop.uri.toString() !== this.$opensilex.getShortUri(this.$opensilex.Oeso.IS_HOSTED) &&
                  prop.uri.toString() !== this.$opensilex.getShortUri(this.$opensilex.Rdfs.LABEL)
              )
              //Formatting for vue-treeSelect + display label in Camel case
              filteredProps.forEach(obj => {
                    this.optionsProps.push({
                      id: obj.uri,
                      label: obj.name.replace(/^./,s => s.toUpperCase())
                    });
                })
          })
          .catch(this.$opensilex.errorHandler)
          .finally(()=>{
              this.optionsProps.forEach(option =>{
                  this.selectedProps.push(option.id);
              })
          })
  }
}
</script>

<style scoped>

</style>

<i18n>
en:
  props-label: "Scientific objects properties"
  props-placeholder: "Select properties to export"

fr:
  props-label: "Propriétés des objets scientifiques"
  props-placeholder: "Sélectionner des propriétés à exporter"

</i18n>