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
                currentRelation relation:
                <strong>{{ currentRelation }}</strong>
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
                :rules="{ regex: /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/ }" 
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
              <b-button @click="addRelations" variant="success">{{$t('component.skos.add')}}</b-button>
            </b-form-group> 
          </b-form-group>
        </b-card>
      </b-form>
    </ValidationObserver>
    <div>
      <b-table striped hover :items="getRelations" :fields="fields">
        <template v-slot:head(relation)="data">{{$t(data.label)}}</template>
        <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
        <template v-slot:cell(actions)="data">
        <b-button-group size="sm">
          <b-button
            size="sm"
            @click="removeRelations(data.item.id)"
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
  
  relations: any[] = [];

  
  _skosReferences : any = {
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: []
  };

  get skosReferences(){
    return this._skosReferences;
  }

  set skosReferences(skosReferences : any){
    this._skosReferences = skosReferences;
  }

  get getRelations(){
    return this.relations;
  }

  currentRelation: string = "";
  currentExternalUri: string = "";

  options: any[] = [
    {
      value: null,
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

  updateRefs(){
    let skosReferences = {
      exactMatch: [],
      closeMatch: [],
      broader: [],
      narrower: []
    };
    
    for (let index = 0; index < this.relations.length; index++) {
      const element = this.relations[index];
      skosReferences[element.relation].push(element.relationURI);
    }
    this.skosReferences = skosReferences;
    return this.actionOnUpdateSkos();
  }


  validateForm() {
    let validatorRef: any = this.$refs.validatorRef;
    return validatorRef.validate();
  }
  

  addRelations(){
    this.validateForm().then(isValid => {
      if(isValid){
        this.relations.push({
          id: new Date().getTime(),
          relation: this.currentRelation,
          relationURI: this.currentExternalUri
        });
        this.updateRefs();
      }
    });
  }
  
  removeRelations(id : number){
    this.relations = this.relations.filter(function(value, index, arr){ return value.id != id;});
    this.updateRefs();
  }
  

  actionOnUpdateSkos(){
   return new Promise((resolve, reject) => {
      return this.$emit("onUpdateSkos", this.skosReferences, result => {
        console.log(result)
          if (result instanceof Promise) {
            result.then(resolve).catch(reject);
          } else {
            resolve(result);
          }
      });
    });
  }
}
</script>

<style scoped lang="scss">
</style>

