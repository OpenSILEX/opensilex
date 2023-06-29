<template>
  <opensilex-SelectForm
    :label="label"
    :helpMessage="helpMessage"
    :selected.sync="personsURI"
    :multiple="multiple"
    :itemLoadingMethod="loadPersons"
    :searchMethod="searchPersons"
    :conversionMethod="personToSelectNode"
    placeholder="component.person.filter-placeholder"
    noResultsText="component.person.filter-search-no-result"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import {SecurityService, PersonDTO} from "opensilex-security/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class PersonSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  service: SecurityService;

  @PropSync("persons")
  personsURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  helpMessage: string;

  @Prop()
  getOnlyPersonsWithoutAccount: boolean;

  @Prop()
  personPropertyExistsCondition : string;

  loadPersons(personsURI) {
    return this.$opensilex
      .getService<SecurityService>("opensilex.SecurityService")
      .getPersonsByURI(personsURI)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<PersonDTO>>>) =>
          http.response.result
      );
  }

  searchPersons(searchQuery, page) {
    return this.$opensilex
      .getService<SecurityService>("opensilex.SecurityService")
      .searchPersons(searchQuery, this.getOnlyPersonsWithoutAccount, undefined, page, 0)
      .then( (http: HttpResponse<OpenSilexResponse<Array<PersonDTO>>>) =>{
      if (this.personPropertyExistsCondition){
          let tmp = http.response.result.map(value => {
              if (!value[this.personPropertyExistsCondition]){
                  return {
                      email : value.email,
                      uri : value.uri,
                      account : value.account,
                      first_name : value.first_name,
                      last_name : value.last_name,
                      isDisabled : true
                  }
              } else {
                  return {
                      email : value.email,
                      uri : value.uri,
                      account : value.account,
                      first_name : value.first_name,
                      last_name : value.last_name,
                      isDisabled : false
                  }
              }
          })
          http.response.result = tmp
          return http;
      } else {
          return http
      }
      });
  }

  personToSelectNode(dto) {
    let personLabel = dto.first_name + " " + dto.last_name;
    if ( dto.email !== null ){
      personLabel += " <" + dto.email + ">";
    }
    return {
      label: personLabel,
      id: dto.uri,
      isDisabled: dto.isDisabled
    };
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
</script>

<style scoped lang="scss">
</style>
