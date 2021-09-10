<template>
  <div>
    <b-row>
      <b-col>
        <opensilex-Card icon="ik#ik-clipboard" :label="$t('component.common.description')">

          <template v-slot:rightHeader>
            <b-button-group>

              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                label="component.common.edit"
                @click="showEditForm">
              </opensilex-EditButton>

              <opensilex-ModalForm
                  v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                  ref="groupVariablesForm"
                  @onUpdate="$emit('onUpdate', variablesGroup)"
                  modalSize="lg"
                  component="opensilex-GroupVariablesForm"
                  createTitle="GroupVariablesForm.add"
                  editTitle="GroupVariablesForm.edit">
              </opensilex-ModalForm>

              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                label="component.common.delete"
                @click="deleteVariablesGroup">
              </opensilex-DeleteButton>

            </b-button-group>
          </template>

          <template v-slot:body>
            <opensilex-UriView :uri="variablesGroup.uri"></opensilex-UriView>
            <opensilex-StringView label="component.common.name" :value="variablesGroup.name"></opensilex-StringView>
            <opensilex-TextView label="component.common.description" :value="variablesGroup.description"></opensilex-TextView>
          </template>

        </opensilex-Card>

      </b-col>
      
      <b-col>
        
        <opensilex-Card icon="ik#ik-clipboard" :label="$t('VariableView.type')">
        
          <template v-slot:body>

            <opensilex-TableView
              v-if="variablesGroup.variables.length !== 0"
              :items="variablesGroup.variables"
              :fields="relationsFields"
              :globalFilterField="true">

              <template v-slot:cell(uri)="{ data }">
                <opensilex-UriLink
                  :uri="data.item.uri"
                  :value="data.item.uri"
                  :to="{path: '/variable/details/'+ encodeURIComponent(data.item.uri)}">
                </opensilex-UriLink>
              </template> 

            </opensilex-TableView>

            <p v-else>
              <strong>{{$t("GroupVariablesDetails.no-var-provided")}}</strong>
            </p>

          </template>

        </opensilex-Card>     

      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {VariablesService, VariablesGroupGetDTO} from "opensilex-core/index";
import GroupVariablesForm from './GroupVariablesForm.vue'
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";

@Component
export default class GroupVariablesDetails extends Vue {
    $opensilex: any;
    service: VariablesService;
    $route: any;
    $router: any;
    $i18n: any;
    uri: string = null;

    get user() {
      return this.$store.state.user;
    }

    get credentials() {
      return this.$store.state.credentials;
    }

    @Prop({default: () => GroupVariablesForm.getEmptyForm()})
    variablesGroup : VariablesGroupGetDTO;

    @Ref("groupVariablesForm") readonly groupVariablesForm!: any;

    relationsFields: any[] = [
      {
        key: "uri",
        label: "component.common.uri",
        sortable: true,
      }
    ];

    created() {
      this.service = this.$opensilex.getService("opensilex.VariablesService");
    }

    showEditForm(){
      let variablesGroupDtoCopy = JSON.parse(JSON.stringify(this.variablesGroup));
      variablesGroupDtoCopy.variables = variablesGroupDtoCopy.variables.map(variable => variable.uri);
      this.groupVariablesForm.showEditForm(variablesGroupDtoCopy);
    }

    deleteVariablesGroup() {
      this.service.deleteVariablesGroup(this.variablesGroup.uri).then(() => {
        let message = this.$i18n.t(this.variablesGroup.name) + " " + this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
        this.$router.push({path: "/variables?elementType=VariableGroup"});
      }).catch(this.$opensilex.errorHandler);
    }
    
    // loadVariablesGroup() {
    //     this.service
    //     .getVariablesGroup(this.variablesGroup.uri)
    //     .then((http: HttpResponse<OpenSilexResponse<VariablesGroupGetDTO>>) => {
    //         this.variablesGroup = http.response.result;
    //     })
    //     .catch(this.$opensilex.errorHandler);
    // }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  GroupVariablesDetails:
    no-var-provided: No variable provided

fr:
  GroupVariablesDetails:
    no-var-provided: Aucune variable associée
</i18n>
