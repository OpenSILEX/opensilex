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
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {VueJsOntologyExtensionService, VueRDFTypeDTO, VueRDFTypePropertyDTO} from "../../../lib";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class ScientificObjectPropertiesSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  vueOntologyService: VueJsOntologyExtensionService

  @Prop()
  type :string;

  @PropSync("props",{ default: () => []})
  selectedProps;
  optionsProps = [];
  private langUnwatcher;
  
  created(){
    this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
    this.loadProps();
    this.$parent.$parent.$on('preSelection', this.preSelection);
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
      let properties: Array<VueRDFTypePropertyDTO> = [];
      this.optionsProps = [];

      this.vueOntologyService
          .getRDFTypeProperties(
              this.type,
              this.type)
          .then((http: HttpResponse<OpenSilexResponse<VueRDFTypeDTO>>) => {
              properties = [...http.response.result.data_properties, ...http.response.result.object_properties];
              //exclude properties : Name, Geometry, Is Hosted(OS)
              let filteredProps :Array<VueRDFTypePropertyDTO> = properties.filter( prop =>
                  prop.uri.toString() !== "vocabulary:isHosted" &&
                  prop.uri.toString() !== "vocabulary:hasGeometry" &&
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
  }

  preSelection(){
    this.optionsProps.forEach(option =>{
      this.selectedProps.push(option.id);
      })
  }
}
</script>

<style scoped>

</style>

<i18n>
en:
  props-label: "Properties"
  props-placeholder: "Select properties to export"

fr:
  props-label: "Propriétés"
  props-placeholder: "Sélectionner des propriétés à exporter"

</i18n>