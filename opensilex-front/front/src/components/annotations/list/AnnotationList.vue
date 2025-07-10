<template>
  <div>
    <div class="pageActionsBtns">
      <opensilex-CreateButton
          v-if="user.hasCredential(modificationCredentialId)"
          label="Annotation.add"
          @click="annotationModalForm.showCreateForm([target])"
          class="createButton greenThemeColor"
      ></opensilex-CreateButton>
    </div>
    <div class="card">
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

              <template v-slot:cell(published)="{data}">
                <opensilex-TextView
                    :value="formatDate(data.item.published)">
                </opensilex-TextView>
              </template>

              <template v-slot:cell(publisher)="{data}">
                <opensilex-PersonContact
                    v-if="data.item.publisher && accountsByUri.get(data.item.publisher)"
                    :personContact="accountsByUri.get(data.item.publisher)"
                    :customDisplayableName="getAccountNames(data.item.publisher)"
                />
              </template>

              <template v-slot:cell(description)="{data}">
                <opensilex-TextView v-if="data.item.description" :value="data.item.description">
                </opensilex-TextView>
              </template>

              <template v-if="displayTargetColumn" v-slot:cell(targets)="{data}">
                <opensilex-TextView :value="data.item.targets[0]">
                </opensilex-TextView>
              </template>

              <template v-slot:cell(actions)="{data}">
                <b-button-group size="sm">
                  <opensilex-DetailButton
                      v-if="!modificationCredentialId || user.hasCredential(modificationCredentialId)"
                      @click="showDetails(data.item)"
                      label="Annotation.details"
                      :title="$t('Annotation.details')"
                      :small="true"
                  />
                  <opensilex-EditButton
                      v-if="! modificationCredentialId || user.hasCredential(modificationCredentialId)"
                      @click="editAnnotation(data.item)"
                      label="Annotation.edit"
                      :small="true"
                  ></opensilex-EditButton>
                  <opensilex-DeleteButton
                      v-if="! deleteCredentialId || user.hasCredential(deleteCredentialId)"
                      @click="deleteAnnotation(data.item.uri)"
                      label="Annotation.delete"
                      :small="true"
                  ></opensilex-DeleteButton>
                </b-button-group>
              </template>
            </opensilex-TableAsyncView>
          </template>
        </opensilex-PageContent>
      </div>
    </div>
    <!-- Modal pour afficher les détails de l'annotation -->
    <opensilex-AnnotationDetails
        v-if="selectedAnnotation"
        :value="isModalVisible"
        :annotationDetails="selectedAnnotation"
        @close="isModalVisible = false"
    />
    <opensilex-AnnotationModalForm
        ref="annotationModalForm"
        @onCreate="refresh"
        @onUpdate="refresh"
    ></opensilex-AnnotationModalForm>

  </div>
</template>

<script lang="ts">
import {Component, Ref, Prop, Watch} from "vue-property-decorator";
import Vue from "vue";

import {AnnotationsService} from "opensilex-core/api/annotations.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import AnnotationModalForm from "../form/AnnotationModalForm.vue";
import {SecurityService} from "opensilex-security/api/security.service";
import {UserGetDTO} from 'opensilex-security/index';
import {AnnotationGetDTO} from 'opensilex-core/index';
import {AccountGetDTO} from "opensilex-security/model/accountGetDTO";

@Component
export default class AnnotationList extends Vue {

  private selectedAnnotation: AnnotationGetDTO | null = null;
  private isModalVisible = false;

  $opensilex: OpenSilexVuePlugin;
  $service: AnnotationsService
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

  @Prop({default: AnnotationList.getDefaultColumns})
  columnsToDisplay: Set<string>;

  accountsByUri: Map<string, AccountGetDTO>;
  renderComponent = true;

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: AnnotationModalForm;

  formatDate(dateStr: string): string {
    const localeString = new Date(dateStr).toLocaleString();

    return localeString.replace(/\//g, '-');
  }

  static getDefaultColumns() {
    return new Set(["published", "description", "publisher"]);
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

  @Prop({default: AnnotationList.newFilter})
  filter;

  @Prop()
  target;

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
    this.filter = AnnotationList.newFilter();
    this.refresh();
  }


  created() {
    this.$service = this.$opensilex.getService("opensilex.AnnotationsService")
    this.$securityService = this.$opensilex.getService("opensilex.SecurityService")
  }

  search(options) {
    return new Promise((resolve, reject) => {
      this.$service.searchAnnotations(
          this.filter.bodyValue,
          this.target,
          this.filter.motivation,
          this.filter.publisher,
          options.orderBy,
          options.currentPage,
          options.pageSize
      ).then((http: HttpResponse<OpenSilexResponse<Array<AnnotationGetDTO>>>) => {

        let results = http.response.result;
        if (results.length == 0) {
          resolve(http);
        } else {
          let usersPromise = this.buildUsersIndexPromise(http.response.result, reject);
          Promise.resolve(usersPromise).then(() => {
            resolve(http);
          })
        }

      }).catch(reject);

    });
  }

  buildUsersIndexPromise(annotations: Array<AnnotationGetDTO>, reject): Promise<void | HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>> {

    this.accountsByUri = new Map();

    let uniqueUsers = new Set<string>();
    annotations.forEach(annotation => {
      if (annotation.publisher) {
        uniqueUsers.add(annotation.publisher);
      }
    });

    return this.$securityService.getAccountsByURI(Array.from(uniqueUsers)).then(http => {
      http.response.result.forEach(accountDTO => {
        this.accountsByUri.set(accountDTO.uri, accountDTO);
      })
    }).catch(reject);
  }

  getAccountNames(accounturi: string): string {
    if (!accounturi) {
      return undefined;
    }
    const accountDTO = this.accountsByUri.get(accounturi);
    if (accountDTO) {
      return accountDTO.linked_person
          ? `${accountDTO.person_first_name} ${accountDTO.person_last_name}`
          : accountDTO.email;
    }
    return undefined;
  }

  static newFilter() {
    return {
      bodyValue: undefined,
      motivation: undefined,
      published: undefined,
      publisher: undefined
    };
  }

  editAnnotation(annotation) {
    let copy = JSON.parse(JSON.stringify(annotation));
    this.annotationModalForm.showEditForm(copy);
  }

  showDetails(annotation: any) {
    this.selectedAnnotation = {
      uri: annotation.uri,
      motivation: annotation.motivation ? { name: annotation.motivation.name } : undefined,
      published: annotation.published,
      publisher: this.getAccountNames(annotation.publisher),
      description: annotation.description
    };
    this.isModalVisible = true;
  }

  get fields() {

    let tableFields = [];

    if (this.columnsToDisplay.has("published")) {
      tableFields.push({key: "published", label: "Annotation.published", sortable: true});
    }
    if (this.columnsToDisplay.has("publisher")) {
      tableFields.push({key: "publisher", label: "Annotation.publisher", sortable: false});
    }
    if (this.columnsToDisplay.has("description")) {
      tableFields.push({key: "description", label: "Annotation.description", sortable: true});
    }
    if (this.columnsToDisplay.has("targets")) {
      tableFields.push({key: "targets", label: "Annotation.targets", sortable: true});
    }

    if (this.enableActions) {
      tableFields.push({key: "actions", label: "component.common.actions", sortable: false});
    }

    return tableFields;
  }

  deleteAnnotation(uri: string) {
    this.$service.deleteAnnotation(uri).then(() => {
      this.$nextTick(() => {
        this.refresh();
      });
      let message = this.$i18n.t("Annotation.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
      this.$opensilex.showSuccessToast(message);
      this.$emit("onDelete", uri);
    }).catch(this.$opensilex.errorHandler);
  }


}
</script>

<style scoped lang="scss">

.pageActionsBtns {
  margin-left: 10px;
  margin-bottom: 10px;
}

</style>


<i18n>
en:
  Annotation:
    name: The annotation
    add: Add annotation
    edit: Edit annotation
    delete: Delete annotation
    details: Details annotation
    motivation: Motivation
    motivation-placeholder: Select a motivation
    motivation-help: Intent or motivation for the creation of the Annotation.
    description: Description
    publisher: Publisher
    published: Date
    target: Target
    list-title: Annotations
    already-exist: the annotation already exist
    image: Image annotations
fr:
  Annotation:
    name: L'annotation
    add: Ajouter une annotation
    edit: Éditer l'annotation
    delete: Supprimer l'annotation
    details: Détailler l'annotation
    motivation: Motivation
    motivation-placeholder: Sélectionnez une motivation
    motivation-help: "Intention ou motivation guidant la création de l'annotation"
    description: Description
    published: Date
    publisher: Publieur
    target: Cible
    list-title: Annotations
    already-exist: l'annotation existe déjà
    image: Annotations d'image
</i18n>
