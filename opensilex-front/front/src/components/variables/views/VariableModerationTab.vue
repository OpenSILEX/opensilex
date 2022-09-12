<template>
  <div class="row">
    <div class="col col-xl-12">

      <div class="card">
        <div class="card-header">
          <h3>
            <i class="ik ik-clipboard"></i>
            {{ $t('VariableDetails.moderation') }}
          </h3>

          <b-row>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col></b-col>
            <b-col>
              <opensilex-CreateButton
                  v-show="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                  @click="showValidationForm"
                  :label="'Valider la variable'"
              ></opensilex-CreateButton>
            </b-col>
          </b-row>


        </div>

        <div class="card-body">

          <opensilex-PageContent
              v-if="renderComponent">

            <template v-slot>

              <opensilex-TableAsyncView
                  ref="tableRef"
                  :searchMethod="search"
                  :fields="fields"
                  :isSelectable="isSelectable"
              >

                <template v-slot:cell(status)="{data}">
                  <div v-if="data.item.moderationActionType == 'Validated declaration'">
                    <opensilex-Icon icon="fa#check-circle" />
                  </div>
                </template>

                <template v-slot:cell(date)="{data}">
                  <opensilex-TextView
                      :value="data.item.date">
                  </opensilex-TextView>
                </template>

                <template v-slot:cell(moderated-action)="{data}">
                  <opensilex-TextView v-if="data.item.moderationActionType" :value="data.item.moderationActionType">
                  </opensilex-TextView>
                </template>

                <template v-slot:cell(moderator)="{data}">
                  <opensilex-TextView v-if="data.item.moderator" :value="data.item.moderator.firstName">
                  </opensilex-TextView>
                </template>

                <template v-slot:cell(email)="{data}">
                  <opensilex-TextView v-if="data.item" :value="data.item.moderator.email.address">
                  </opensilex-TextView>
                </template>

                <template v-slot:cell(comment)="{data}">
                  <opensilex-TextView v-if="data" :value="''">
                  </opensilex-TextView>
                </template>

                <template v-if="displayTargetColumn" v-slot:cell(targets)="{data}">
                  <opensilex-TextView :value="data.item.targets[0]">
                  </opensilex-TextView>
                </template>

                <template v-slot:cell(actions)="{data}">
                  <b-button-group size="sm">
                    <opensilex-DeleteButton
                        v-if="! deleteCredentialId || user.hasCredential(deleteCredentialId)"
                        @click="deleteModeration(data.item.uri)"
                        label="Moderation.delete"
                        :small="true"
                    ></opensilex-DeleteButton>
                  </b-button-group>
                </template>
              </opensilex-TableAsyncView>
            </template>
          </opensilex-PageContent>

        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Ref, Prop, Watch} from "vue-property-decorator";
import Vue from "vue";

// @ts-ignore
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
// @ts-ignore
import {AnnotationGetDTO} from "opensilex-core/model/annotationGetDTO";
import AnnotationModalForm from "../form/AnnotationModalForm.vue";
// @ts-ignore
import {UserGetDTO} from "opensilex-security/model/userGetDTO";
// @ts-ignore
import {SecurityService} from "opensilex-security/api/security.service";
import {VariablesService} from "opensilex-core/api/variables.service";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";

@Component
export default class VariableModerationTab extends Vue {

  $opensilex: OpenSilexVuePlugin;
  $service: VariablesService
  $securityService: SecurityService;

  $i18n: any;
  $store: any;

  @Prop({
    default: false
  })
  isSelectable;

  @Prop()
  modificationCredentialId;

  @Prop()
  deleteCredentialId;

  @Prop({default: true})
  enableActions;

  @Prop({default: true})
  displayTargetColumn: boolean;

  @Prop({default: VariableModerationTab.getDefaultColumns})
  columnsToDisplay: Set<string>;

  usersByUri: Map<string,UserGetDTO>;
  renderComponent = true;

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: AnnotationModalForm;


  static getDefaultColumns(){
    return new Set(["Status", "Date","Moderated action","Moderator","Email","Comment"]);
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  refresh() {
    this.tableRef.refresh();
  }

  @Prop({default: VariableModerationTab.newFilter})
  filter;

  @Prop()
  target;

  @Prop()
  variable;

  @Watch("target")
  onTargetChange() {
    this.renderComponent = false;

    this.$nextTick(() => {
      // Add the component back in
      this.renderComponent = true;
    });
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        () => {
          this.refresh();
        }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  reset() {
    this.filter = VariableModerationTab.newFilter();
    this.refresh();
  }


  created() {
    this.$service = this.$opensilex.getService("opensilex.VariablesService")
    this.$securityService = this.$opensilex.getService("opensilex.SecurityService")
  }

  search() {
    return this.$service.getVariableModerationAction(this.variable);
  }

  static newFilter() {
    return {
      bodyValue: undefined,
      motivation: undefined,
      created: undefined,
      author: undefined
    };
  }

  get fields() {

    let tableFields = [];

    if(this.columnsToDisplay.has("Status")){
      tableFields.push({key: "status", label: "Moderation.status", sortable: true});
    }
    if(this.columnsToDisplay.has("Date")){
      tableFields.push({key: "date", label: "Moderation.date", sortable: true});
    }
    if(this.columnsToDisplay.has("Moderated action")){
      tableFields.push({key: "moderated-action", label: "Moderation.moderated-action", sortable: true});
    }
    if(this.columnsToDisplay.has("Moderator")){
      tableFields.push({key: "moderator", label: "Moderation.moderator", sortable: true});
    }
    if(this.columnsToDisplay.has("Email")){
      tableFields.push({key: "email", label: "Moderation.email", sortable: false});
    }

    if(this.columnsToDisplay.has("Comment")){
      tableFields.push({key: "comment", label: "Moderation.comment", sortable: false});
    }
    if(this.columnsToDisplay.has("targets")){
      tableFields.push({key: "targets", label: "Moderation.targets", sortable: true});
    }

    if (this.enableActions) {
      tableFields.push({key: "actions", label: "component.common.actions", sortable: false});
    }

    return tableFields;
  }

  deleteModeration(uri: string) {
    this.$service.deleteVariableModerationAction(uri).then(() => {
      this.$nextTick(() => {
        this.refresh();
      });
      let message = this.$i18n.t("Moderation.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
      this.$opensilex.showSuccessToast(message);
      this.$emit("onDelete", uri);
    }).catch(this.$opensilex.errorHandler);
  }

}
</script>


<i18n>
en:
  Moderation:
    name: The moderation
    add: Add moderation
    edit: Edit moderation
    delete: Delete moderation
    status: Status
    comment: Comment
    moderator: Moderator
    moderated-action: Moderated action
    email: Email
    date: Date
    target: Target
    already-exist: the moderation already exist
fr:
  Moderation:
    name: La modération
    add: Ajouter une modération
    edit: Éditer la modération
    status: Statut
    delete: Supprimer la modération
    comment: Commentaire
    moderator: Modérateur
    date: Date
    moderated-action: Action modérée
    email: Email
    target: Cible
    already-exist: la modération existe déjà
</i18n>
