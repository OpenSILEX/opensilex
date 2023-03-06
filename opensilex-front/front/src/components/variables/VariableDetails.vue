<template>

  <div class="page">
    <b-row>
      <b-col>
        <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">

          <template v-slot:rightHeader>

            <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID) && displayLocalActions"
                label="VariableDetails.edit" variant="outline-primary"
                @click="showEditForm"
            ></opensilex-EditButton>

            <opensilex-VariableCreate
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                ref="variableForm"
                @onUpdate="$emit('onUpdate', $event)"
            ></opensilex-VariableCreate>

            <opensilex-InteroperabilityButton
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID) && displayLocalActions"
                label="VariableDetails.edit-references"
                @click="skosReferences.show()"
            ></opensilex-InteroperabilityButton>

            <opensilex-ExternalReferencesModalForm
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                ref="skosReferences"
                :references.sync="variable"
                @onUpdate="update"
            ></opensilex-ExternalReferencesModalForm>

            <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID) && displayLocalActions"
                @click="deleteVariable"
                label="component.common.list.buttons.delete"
            ></opensilex-DeleteButton>

          </template>

                    <template v-slot:body>
                        <opensilex-UriView v-if="variable && variable.uri" :uri="variable.uri"
                        ></opensilex-UriView>
                        <opensilex-StringView label="component.common.name"
                                              :value="variable.name"></opensilex-StringView>
                        <opensilex-StringView label="VariableForm.altName"
                                              :value="variable.alternative_name"></opensilex-StringView>
                        <opensilex-TextView label="component.common.description"
                                            :value="variable.description"></opensilex-TextView>
                        <div
                            v-if="variable.from_shared_resource_instance"
                        >
                          <opensilex-UriView
                              title="component.sharedResourceInstances.label"
                              :uri="variable.from_shared_resource_instance.uri"
                              :value="variable.from_shared_resource_instance.label"
                          ></opensilex-UriView>
                          <opensilex-DateView
                              label="component.sharedResourceInstances.import_date.label"
                              :value="variable.last_update_date"
                              :isDatetime="true"
                              :useLocaleFormat="true"
                          ></opensilex-DateView>
                        </div>
                    </template>
                </opensilex-Card>
            </b-col>
            <b-col>
                <opensilex-Card label="VariableDetails.structure" icon="ik#ik-clipboard">
                    <template v-slot:body>
                        <opensilex-UriView title="VariableView.entity" v-if="variable.entity"
                                           :value="variable.entity.name" :uri="variable.entity.uri"
                                           :to="getEntityPath()">
                        </opensilex-UriView>

                        <opensilex-UriView title="VariableForm.interestEntity-label"
                                           :value="variable.entity_of_interest ? variable.entity_of_interest.name: undefined"
                                           :uri="variable.entity_of_interest ? variable.entity_of_interest.uri: undefined"
                                           :to="variable.entity_of_interest ? getInterestEntityPath(): undefined">
                        </opensilex-UriView>

                        <opensilex-UriView title="VariableView.characteristic" v-if="variable.characteristic"
                                           :value="variable.characteristic.name" :uri="variable.characteristic.uri"
                                           :to="getCharacteristicPath()">
                        </opensilex-UriView>
                        <opensilex-UriView title="VariableView.method" v-if="variable.method"
                                           :value="variable.method.name" :uri="variable.method.uri"
                                           :to="getMethodPath()">
                        </opensilex-UriView>
                        <opensilex-UriView title="VariableView.unit" v-if="variable.unit"
                                           :value="variable.unit.name" :uri="variable.unit.uri"
                                           :to="getUnitPath()">
                        </opensilex-UriView>
                    </template>
                </opensilex-Card>
            </b-col>
        </b-row>
        <b-row>
            <b-col>
                <opensilex-Card label="component.skos.ontologies-references-label" icon="fa#globe-americas">
                    <template v-slot:body>
                        <opensilex-ExternalReferencesDetails
                            :skosReferences="variable">
                        </opensilex-ExternalReferencesDetails>
                    </template>
                </opensilex-Card>
            </b-col>
            <b-col>
                <opensilex-Card label="VariableDetails.advanced" icon="ik#ik-clipboard">
                    <template v-slot:body>
                      <opensilex-UriListView
                          v-if="!isGermplasmMenuExcluded"
                          label="GermplasmList.speciesLabel"
                          :list="speciesList"
                      ></opensilex-UriListView>

            <opensilex-StringView label="OntologyPropertyForm.data-type"
                                  :value="$opensilex.getVariableDatatypeLabel(variable.datatype)"></opensilex-StringView>
            <opensilex-StringView label="VariableForm.time-interval"
                                  :value="variable.time_interval"></opensilex-StringView>
            <opensilex-StringView label="VariableForm.sampling-interval"
                                  :value="variable.sampling_interval"></opensilex-StringView>

            <opensilex-UriView v-if="variable && variable.trait" title="VariableForm.trait-uri"
                               :uri="variable.trait" :url="variable.trait"></opensilex-UriView>
            <opensilex-StringView v-if="variable && variable.trait"
                                  label="VariableForm.trait-name"
                                  :value="variable.trait_name"></opensilex-StringView>
          </template>
        </opensilex-Card>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import ExternalReferencesModalForm from "../common/external-references/ExternalReferencesModalForm.vue";
import VariablesView from "./VariablesView.vue";
import VariableCreate from "./form/VariableCreate.vue";
import {VariablesService, VariableDetailsDTO} from "opensilex-core/index";
import VariableForm from "./form/VariableForm.vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {DataService} from "opensilex-core/api/data.service";
import DTOConverter from "../../models/DTOConverter";
import {VariableUpdateDTO} from "opensilex-core/index";
import VueRouter from "vue-router";

@Component
export default class VariableDetails extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  $route: any;
  $router: VueRouter;
  $t: any;
  $i18n: any;
  service: VariablesService;
  dataService: DataService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop({
    default: () => VariableForm.getEmptyForm()
  }) variable: VariableDetailsDTO;

  @Prop() displayLocalActions: boolean;

  @Ref("variableForm") readonly variableForm!: VariableCreate;

  @Ref("skosReferences") skosReferences!: ExternalReferencesModalForm;

  get speciesList() {
    if (!this.variable.species) {
      return [];
    }

    return this.variable.species.map(species => {
      return {
        uri: species.uri,
        value: species.name,
        to: {
          path: "/germplasm/details/" + encodeURIComponent(species.uri),
        }
      };
    });
  }

  get isGermplasmMenuExcluded() {
        return this.$opensilex.getConfig().menuExclusions.includes("germplasm");
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.VariablesService");
    this.dataService = this.$opensilex.getService("opensilex-core.DataService");
  }


  showEditForm() {

    this.getCountDataPromise(this.variable.uri).then(countResult => {
      if (countResult && countResult.response) {

        // make a deep copy of the variable in order to not change the current dto
        // In case a field has been updated into the form without confirmation (by sending update to the server)

        let variableDtoCopy = JSON.parse(JSON.stringify(this.variable));
        this.variableForm.showEditForm(variableDtoCopy, countResult.response.result);
      }
    })

  }

  update(variable) {
    let formattedVariable:VariableUpdateDTO =  DTOConverter.extractURIFromResourceProperties(variable);

    this.service.updateVariable(formattedVariable).then(() => {
      let message = this.$i18n.t("VariableView.name") + " " + formattedVariable.name + " " + this.$i18n.t("component.common.success.update-success-message");
      this.$opensilex.showSuccessToast(message);
      this.$emit("onUpdate", variable);
    }).catch(this.$opensilex.errorHandler);
  }

  deleteVariable() {

    this.getCountDataPromise(this.variable.uri)
        .then((http: HttpResponse<OpenSilexResponse<number>>) => {
          let count = http.response.result;
          if (count > 0) {
            this.$opensilex.showErrorToast(count + " " + this.$i18n.t("VariableView.associated-data-error"));
          } else {
            this.service.deleteVariable(this.variable.uri).then(() => {
              let message = this.$i18n.t("VariableView.name") + " " + this.variable.name + " " + this.$i18n.t("component.common.success.delete-success-message");
              this.$opensilex.showSuccessToast(message);
              this.$router.push({path: "/variables"});
            }).catch(this.$opensilex.errorHandler);
          }
        });
  }

  getCountDataPromise(uri) {
    return this.dataService.countData(
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        [uri],
        undefined,
        undefined,
        undefined,
        undefined);
  }

  getPath(elementType: string, uri: string) {
    if (this.$route.query.sharedResourceInstance) {
      return undefined;
    }
    return {
      path: "/variables/?elementType=" + elementType + "&selected=" + encodeURIComponent(uri)
    };
  }

  getEntityPath() {
    return this.getPath(VariablesView.ENTITY_TYPE, this.variable.entity.uri);
  }

  getInterestEntityPath() {
    return this.getPath(VariablesView.INTEREST_ENTITY_TYPE, this.variable.entity_of_interest.uri);
  }

  getCharacteristicPath() {
    return this.getPath(VariablesView.CHARACTERISTIC_TYPE, this.variable.characteristic.uri);
  }

  getMethodPath() {
    return this.getPath(VariablesView.METHOD_TYPE, this.variable.method.uri);
  }

  getUnitPath() {
    return this.getPath(VariablesView.UNIT_TYPE, this.variable.unit.uri);
  }

}
</script>

<style scoped lang="scss">

.page {
  margin-top : 20px;
}
</style>

<i18n>
en:
  VariableDetails:
    title: Detailled variable view
    entity-name: Entity name
    characteristic-name: Characteristic name
    method-name: Method name
    unit-name: Unit name
    structure: Structure
    advanced: Advanced information
    edit: Edit variable
    edit-references: Edit references
    visualization: Device associated Data Visualization
fr:
  VariableDetails:
    title: Vue détaillée de la variable
    entity-name: Nom d'entité
    characteristic-name: Nom de la caractéristique
    method-name: Nom de méthode
    unit-name: Nom d'unité
    structure: Structure
    advanced: Informations avancées
    edit: Editer la variable
    edit-references: Editer les références
    visualization: Visualisation des données associées à un instrument
</i18n>
