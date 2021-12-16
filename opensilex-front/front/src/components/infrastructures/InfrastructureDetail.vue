<template>
  <div style="display: contents;">
    <b-card v-if="selected">
      <template v-slot:header>
        <h3>
          <opensilex-Icon icon="ik#ik-clipboard" />
          {{ $t("component.common.details-label") }}
        </h3>
        <div class="card-header-right" v-if="withActions">
          <b-button-group>
            <opensilex-EditButton
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
                )
              "
              @click="editInfrastructure()"
              label="InfrastructureTree.edit"
              :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID
                )
              "
              @click="deleteInfrastructure()"
              label="InfrastructureTree.delete"
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
            path: '/infrastructure/details/' + encodeURIComponent(selected.uri),
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
          label="InfrastructureDetail.parentOrganizations"
          :inline="false"
        >
        </opensilex-UriListView>

        <!-- Groups -->
        <opensilex-UriListView
            label="InfrastructureDetail.groups.label"
            :list="groupUriList"
            :inline="false"
            v-if="hasGroups"
        >
        </opensilex-UriListView>

        <!-- Facilities -->
        <opensilex-UriListView
            label="InfrastructureDetail.facilities.label"
            :list="facilityUriList"
            :inline="false"
            v-if="hasFacilities"
          >
        </opensilex-UriListView>

        <!-- Sites -->
        <opensilex-UriListView
            label="InfrastructureDetail.sites.label"
            :list="siteUriList"
            :inline="false"
            v-if="hasSites"
          >
        </opensilex-UriListView>

        <!-- Expe -->
        <opensilex-UriListView
            label="InfrastructureDetail.experiments.label"
            :list="experimentUriList"
            :inline="false"
            v-if="hasExperiments"
          >
        </opensilex-UriListView>
      </div>
    </b-card>
    <opensilex-ModalForm
        ref="infrastructureForm"
        component="opensilex-InfrastructureForm"
        createTitle="InfrastructureTree.add"
        editTitle="InfrastructureTree.update"
        icon="ik#ik-globe"
        @onCreate="$emit('onCreate', $event)"
        @onUpdate="$emit('onUpdate', $event)"
        :initForm="setParents"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {InfrastructureGetDTO} from "opensilex-core/index";
import {OrganizationsService} from "opensilex-core/api/organizations.service";

@Component
export default class InfrastructureDetail extends Vue {
  $opensilex: any;
  organizationService: OrganizationsService;

  @Prop()
  selected: InfrastructureGetDTO;

  @Prop({
    default: false,
  })
  withActions;

  @Ref("infrastructureForm") readonly infrastructureForm!: any;

  created() {
    this.organizationService = this.$opensilex.getService("opensilex-core.OrganisationsService");
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

  get hasFacilities() {
    return this.selected.facilities.length > 0;
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
          path: "/infrastructure/details/" + encodeURIComponent(parent.uri),
        },
      }
    });
  }

  get facilityUriList() {
    return this.selected.facilities.map(facility => {
      return {
        uri: facility.uri,
        value: facility.name,
        to: {
          path: "/infrastructure/facility/details/" + encodeURIComponent(facility.uri)
        }
      };
    });
  }

  get siteUriList() {
    return this.selected.sites.map(site => {
      return {
        uri: site.uri,
        value: site.name,
        to: {
          path: "/infrastructure/site/details/" + encodeURIComponent(site.uri)
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

  editInfrastructure() {
    this.organizationService
      .getInfrastructure(this.selected.uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let getDto = http.response.result;
        let editDto = {
          ...getDto,
          uri: getDto.uri,
          groups: getDto.groups.map(group => group.uri),
          facilities: getDto.facilities.map(facility => facility.uri),
          parents: getDto.parents.map(parent => parent.uri)
        };
        this.infrastructureForm.showEditForm(editDto);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteInfrastructure() {
    this.$emit("onDelete");
  }

  setParents(form) {
    form.parents = this.selected.parents;
  }

  setInfrastructure(form) {
    form.groups = this.selected.groups.map((group) => group.uri);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureDetail:
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
  InfrastructureDetail:
    parentOrganizations: Organisations parentes
    groups:
      label: "Groupes"
    facilities:
      label: "Installations techniques"
    sites:
      label: "Sites"
    experiments:
      label: "Expérimentations"
</i18n>