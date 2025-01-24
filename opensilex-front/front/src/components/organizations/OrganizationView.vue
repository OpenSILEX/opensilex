<!--
  - ******************************************************************************
  -                         OrganizationView.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 29/08/2024 09:10
  - Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
  - ******************************************************************************
  -->
<template>
    <div class="container-fluid">
        <opensilex-CreateButton
                id="createOrgaButton"
                @click="onCreateClick()"
                label="OrganizationView.create"
                class="createButton">
        </opensilex-CreateButton>

        <opensilex-PageContent>
            <template v-slot>
                <opensilex-OrganizationList
                        ref="organizationList"
                        @onEdit="OnOrganizationListEdit($event)"
                ></opensilex-OrganizationList>
            </template>
        </opensilex-PageContent>

        <opensilex-ModalForm
                v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
                ref="organizationForm"
                lazy="true"
                component="opensilex-OrganizationForm"
                createTitle="OrganizationView.create"
                editTitle="OrganizationView.update"
                icon="ik#ik-map-pin"
                @onCreate="organisationList.refresh()"
                @onUpdate="organisationList.refresh()"
        ></opensilex-ModalForm>
    </div>
</template>

<script lang="ts">
import Vue from 'vue';
import {OpenSilexStore} from "../../models/Store";
import {Component, Ref} from "vue-property-decorator";
import OrganizationList from "@/components/organizations/OrganizationList.vue";

@Component
export default class OrganizationView extends Vue {
    //#region Plugins and services
    public $store: OpenSilexStore;
    //#endregion

    //#region Refs
    @Ref("organizationForm") private readonly organizationForm
    @Ref("organizationList") private readonly organisationList!: OrganizationList;
    //#endregion

    //#region Computed
    private get user() {
        return this.$store.state.user;
    }

    private get credentials() {
        return this.$store.state.credentials;
    }
    //#endregion

    //#region Events handlers
    private OnOrganizationListEdit(dto) {
        this.organizationForm.showEditForm(dto);
    }

    private onCreateClick() {
        this.organizationForm.showCreateForm();
    }
    //#endregion


}
</script>

<style scoped lang="scss">
#createOrgaButton {
  margin-bottom: 10px;
  margin-top: -15px;
}

</style>

<i18n>
en:
    OrganizationView:
        title: "Organizations"
        description: "Manage and configure organizations"
        create: "Add organization"
        update: "Update organization"
fr:
    OrganizationView:
        title: "Organisations"
        description: "Gérer et configurer les organisations"
        create: "Ajouter une organisation"
        update: "Modifier une organisation"
</i18n>