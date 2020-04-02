<template>
  <div>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <b-card bg-variant="light">
          <b-form-group
            label-cols-lg="6"
            label="Ontologies References"
            label-size="lg"
            label-class="font-weight-bold pt-0"
            class="mb-0"
          >
            <b-form-group label-align-sm="right">
              <opensilex-FormInputLabelHelper
                label="component.skos.relation"
                helpMessage="component.skos.relation-help"
              ></opensilex-FormInputLabelHelper>
              <b-form-select
                v-model="currentRelation"
                :options="options"
                :placeholder="$t('component.skos.relation-placeholder')"
              ></b-form-select>
              <div class="mt-3">
                 current selected relation:
                <strong>{{ (currentRelation == "") ? 'component.skos.no-relation-selected' : currentRelation}}</strong>
              </div>
            </b-form-group>
            <!-- URI -->
            <b-form-group label-align-sm="right">
              <opensilex-FormInputLabelHelper
                label="component.skos.uri"
                helpMessage="component.skos.-help"
              ></opensilex-FormInputLabelHelper>
               <ValidationProvider
                :name="$t('component.skos.uri')"
                :rules="{ 
                  required: true, 
                  regex: /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/ 
                }" 
                v-slot="{ errors }"
              >
              <b-form-input
                id="externalUri"
                v-model.trim="currentExternalUri"
                type="text"
                required
                :placeholder="$t('component.skos.uri-placeholder')"
              ></b-form-input>
                <div class="error-message alert alert-danger">{{ errors[0] }}</div>
              </ValidationProvider>
            </b-form-group>
             <b-form-group  label-align-sm="right">
              <b-button @click="addRelationsToSkosReferences" variant="info">{{$t('component.skos.add')}}</b-button>
            </b-form-group> 
          </b-form-group>
        </b-card>
      </b-form>
    </ValidationObserver>
    <div>
      <b-table striped hover :items="relations" :fields="fields">
        <template v-slot:head(relation)="data">{{$t(data.label)}}</template>
        <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
        <template v-slot:cell(actions)="data">
        <b-button-group size="sm">
          <b-button
            size="sm"
            @click="removeRelationsToSkosReferences(data.item.relation,data.item.id)"
            variant="danger"
          >
            <font-awesome-icon icon="trash-alt" size="sm" />
          </b-button>
        </b-button-group>
      </template>
      </b-table>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Multiselect from "vue-multiselect";
// import { OntologyReferenceDTO } from "opensilex-core/index";
import Vue from "vue";

@Component
export default class ExternalReferencesForm extends Vue {
  $opensilex: any;
  $store: any;
  
  currentRelation: string = "";
  currentExternalUri: string = "";

   @Prop()
  skosReferences : any ;

  relationsInternal: any[] = []; 

  options: any[] = [
    {
      value: "",
      text: "Please select an relation",
      disabled: true
    },
    { value: "exactMatch", text: "exactMatch" },
    { value: "closeMatch", text: "closeMatch" },
    { value: "narrower", text: "narrower" },
    { value: "broader", text: "broader" }
  ];

  fields = [
    {
      key: "relation",
      label: "component.skos.relation",
      sortable: true
    },
    {
      key: "relationURI",
      label: "component.skos.uri",
      sortable: false
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  relations() {
    this.relationsInternal = [];
    this.updateRelations("narrower", this.skosReferences.narrower);
    this.updateRelations("broader", this.skosReferences.broader);
    this.updateRelations("closeMatch", this.skosReferences.closeMatch);
    this.updateRelations("exactMatch", this.skosReferences.exactMatch);
    return this.relationsInternal;
  } 

  updateRelations(relation : string, references : string[]){
    for (let index = 0; index < references.length; index++) {
      const element = references[index];
      this.addRelation(relation,element) 
    } 
  } 

  addRelation(currentRelation : string, currentExternalUri : string){ 
    this.relationsInternal.push({
      relation: currentRelation,
      relationURI: currentExternalUri
    }); 
  }

  validateForm() {
    let validatorRef: any = this.$refs.validatorRef;
    return validatorRef.validate();
  }
  

  addRelationsToSkosReferences(){
    this.validateForm().then(isValid => {
      if(isValid){
        this.addRelationToSkosReferences(this.currentRelation,this.currentExternalUri)
       }
    });
  }
  addRelationToSkosReferences(currentRelation : string, currentExternalUri : string){
    console.log(this.skosReferences,this.skosReferences[currentRelation] )
    if(!this.skosReferences[currentRelation].includes(currentExternalUri)){
      this.skosReferences[currentRelation].push(currentExternalUri);
    }
  }
  
  removeRelationsToSkosReferences(relation : string, externalUri : string){
    console.log(this.skosReferences[relation] )
    this.skosReferences[relation] = this.skosReferences[relation].
      filter(function(value, index, arr){ return value != externalUri;});
  } 
}
</script>

<style scoped lang="scss">
</style>

