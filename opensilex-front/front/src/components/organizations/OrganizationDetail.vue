<template>
  <div>
    <b-card v-if="selected">
      <template v-slot:header>
        <h3>
          <opensilex-Icon icon="ik#ik-clipboard" />
          {{ $t("component.common.informations") }}
        </h3>
        <div class="card-header-right" v-if="withActions">
          <b-button-group>
            <opensilex-EditButton
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID
                )
              "
              @click="editOrganization()"
              label="OrganizationTree.edit"
              :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_ORGANIZATION_DELETE_ID
                )
              "
              @click="deleteOrganization()"
              label="OrganizationTree.delete"
              :small="true"
            ></opensilex-DeleteButton>
          </b-button-group>
        </div>
      </template>
      <div>
        <!-- URI -->
        <opensilex-UriView
          :uri="selected.uri"
          :value="selected.uri"
          :to="{
            path: '/organization/details/' + encodeURIComponent(selected.uri),
          }"
        ></opensilex-UriView>
        <!-- Name -->
        <opensilex-StringView
          label="component.common.name"
          :value="selected.name"
        ></opensilex-StringView>
        <!-- Type -->
        <opensilex-TypeView
          :type="selected.rdf_type"
          :typeLabel="selected.rdf_type_name"
        ></opensilex-TypeView>

        <!-- Parents -->
        <opensilex-UriListView
          v-if="hasParents"
          :list="parentUriList"
          label="OrganizationDetail.parentOrganizations"
          :inline="false"
        >
        </opensilex-UriListView>

        <!-- Groups -->
        <opensilex-UriListView
            label="OrganizationDetail.groups.label"
            :list="groupUriList"
            :inline="false"
            v-if="hasGroups"
        >
        </opensilex-UriListView>

        <!-- Sites -->
        <opensilex-UriListView
            label="OrganizationDetail.sites.label"
            :list="siteUriList"
            :inline="false"
            v-if="hasSites"
          >
        </opensilex-UriListView>

        <!-- Expe -->
        <opensilex-UriListView
            label="OrganizationDetail.experiments.label"
            :list="experimentUriList"
            :inline="false"
            v-if="hasExperiments"
          >
        </opensilex-UriListView>

        <!-- Metadata -->
        <opensilex-MetadataView
          v-if="selected.publisher && selected.publisher.uri"
          :publisher="selected.publisher"
          :publicationDate="selected.publication_date"
          :lastUpdatedDate="selected.last_updated_date" 
        ></opensilex-MetadataView>
      </div>
    </b-card>
    <opensilex-ModalForm
        ref="organizationForm"
        component="opensilex-OrganizationForm"
        createTitle="OrganizationTree.add"
        editTitle="OrganizationTree.update"
        icon="ik#ik-globe"
        @onCreate="$emit('onCreate', $event)"
        @onUpdate="$emit('onUpdate', $event)"
        :initForm="setParents"
        :lazy="true"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {OrganizationGetDTO} from "opensilex-core/index";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import DTOConverter from "../../models/DTOConverter";

@Component
export default class OrganizationDetail extends Vue {
  $opensilex: any;
  organizationService: OrganizationsService;

  @Prop()
  selected: OrganizationGetDTO;

  @Prop({
    default: false,
  })
  withActions;

  @Ref("organizationForm") readonly organizationForm!: any;

  created() {
    this.organizationService = this.$opensilex.getService("opensilex-core.OrganizationsService");
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get hasParents() {
    return this.selected.parents.length > 0;
  }

  get hasGroups() {
    return this.selected.groups.length > 0;
  }

  get hasSites() {
    return this.selected.sites.length > 0;
  }

  get hasExperiments() {
    return this.selected.experiments.length > 0;
  }

  get groupUriList() {
    return this.selected.groups.map(group => {
      return {
        uri: group.uri,
        value: group.name,
        to: {
          path: "/groups#" + encodeURIComponent(group.uri),
        },
      }
    });
  }

  get parentUriList() {
    return this.selected.parents.map(parent => {
      return {
        uri: parent.uri,
        value: parent.name,
        to: {
          path: "/organization/details/" + encodeURIComponent(parent.uri),
        },
      }
    });
  }

  get siteUriList() {
    return this.selected.sites.map(site => {
      return {
        uri: site.uri,
        value: site.name,
        to: {
          path: "/organization/site/details/" + encodeURIComponent(site.uri)
        }
      };
    });
  }

  get experimentUriList() {
    return this.selected.experiments.map(experiment => {
      return {
        uri: experiment.uri,
        value: experiment.name,
        to: {
          path: "/experiment/details/" + encodeURIComponent(experiment.uri)
        }
      };
    });
  }

  editOrganization() {
    this.organizationService
      .getOrganization(this.selected.uri)
      .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
        let editDto = DTOConverter.extractURIFromResourceProperties(http.response.result);
        this.organizationForm.showEditForm(editDto);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteOrganization() {
    this.$emit("onDelete");
  }

  setParents(form) {
    form.parents = this.selected.parents;
  }

  setOrganization(form) {
    form.groups = this.selected.groups.map((group) => group.uri);
  }
}
</script>

<style scoped lang="scss">

</style>

<i18n>
en:
  OrganizationDetail:
    parentOrganizations: Parent organizations
    groups:
      label: "Groups"
    facilities:
      label: "Facilities"
    sites:
      label: "Sites"
    experiments:
      label: "Experiments"
fr:
  OrganizationDetail:
    parentOrganizations: Organisations parentes
    groups:
      label: "Groupes"
    facilities:
      label: "Installations environnementales"
    sites:
      label: "Sites"
    experiments:
      label: "Expérimentations"
</i18n>