<template>
    <opensilex-WizardForm
            ref="facilityForm"
            :steps="steps"
            createTitle="FacilitiesView.add"
            editTitle="FacilitiesView.update"
            icon="ik#ik-map"
            :createAction="callOrganizationFacilityCreation"
            :updateAction="callOrganizationFacilityUpdate"
            @onCreate="$emit('onCreate', $event)"
            @onUpdate="$emit('onUpdate', $event)"
            :initForm="getEmptyForm"
            modalSize="lg"
    ></opensilex-WizardForm>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import DTOConverter from "../../models/DTOConverter";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {LocationsService} from "opensilex-core/api/locations.service";
import {
    LocationObservationDTO,
    FacilityCreationDTO,
    FacilityGetDTO,
    FacilityUpdateDTO,
    UserGetDTO
} from "opensilex-core/index";
import WizardForm from "../../components/common/forms/WizardForm.vue";

@Component
export default class FacilityModalForm extends Vue {
    //#region Plugins and services
    private readonly $opensilex: OpenSilexVuePlugin;
    private locationsService: LocationsService;
    //endregion

    //#region Props
    @Prop({default: () => {}})
    private initForm: (dto: FacilityCreationDTO) => void;
    //endregion

    //#region Refs
    @Ref("facilityForm")
    private readonly facilityForm!: WizardForm;
    //endregion

    //#region Data
    private steps = [
        {
            component: "opensilex-FacilityForm",
        },
        {
            component: "opensilex-LocationsForm",
        }
    ];
    //endregion

    //#region Computed
    //endregion

    //#region Events
    //endregion

    //#region Events handlers
    //endregion

    //#region Public methods
    public showEditForm(uri) {
        let editDto;
        this.$opensilex
                .getService<OrganizationsService>("opensilex.OrganizationsService")
                .getFacility(uri)
                .then((http) => {
                    let dto: FacilityGetDTO = http.response.result;
                    let publisher: UserGetDTO = dto.publisher;
                    editDto = DTOConverter.extractURIFromResourceProperties<FacilityGetDTO, FacilityUpdateDTO>(dto);
                    editDto.publisher = publisher;
                }).catch(this.$opensilex.errorHandler)
                .finally(() => {
                  if (editDto) {
                        this.locationsService.searchLocationHistory(
                                uri,
                                undefined,
                                undefined,
                                [],
                                0,
                                0
                        ).then((http: HttpResponse<OpenSilexResponse<Array<LocationObservationDTO>>>) => {
                            editDto.locations = http.response.result
                            this.facilityForm.showEditForm(editDto);
                        });
                    } else {
                        editDto.locations = [];
                        this.facilityForm.showEditForm(editDto);
                    }
                });
    }

    public showCreateForm() {
        this.facilityForm.showCreateForm();
    }

    //endregion

    //#region Hooks
    private created() {
        this.locationsService = this.$opensilex.getService<LocationsService>("opensilex.LocationsService")
    }

    //endregion

    //#region Private methods
    /* return empty form by default for the facility creation from facilities page
    and a pre-filled form for creation from an organization or a site */
    private getEmptyForm() {
        let emptyForm = {
            uri: undefined,
            rdf_type: undefined,
            name: undefined,
            description: undefined,
            address: undefined,
            organizations: [],
            sites: [],
            variableGroups: [],
            relations: [],
            locations: []
        };
        this.initForm(emptyForm);
        return emptyForm;
    }

    private callOrganizationFacilityCreation(form: FacilityCreationDTO) {
        let definedRelations = [];
        for (let i in form.relations) {
            let relation = form.relations[i];
            if (relation.value != null) {
                if (Array.isArray(relation.value)) {
                    for (let j in relation.value) {
                        definedRelations.push({
                            property: relation.property,
                            value: relation.value[j],
                        });
                    }
                } else {
                    definedRelations.push(relation);
                }
            }
        }

        form.relations = definedRelations;

        return this.$opensilex
                .getService<OrganizationsService>("opensilex.OrganizationsService")
                .createFacility(form)
                .then((http: HttpResponse<OpenSilexResponse<string>>) => {
                    let message = this.$i18n.t("OrganizationFacilityForm.name") + " " + form.name + " " + this.$i18n.t("component.common.success.creation-success-message");
                    this.$opensilex.showSuccessToast(message);
                })
                .catch((error) => {
                    if (error.status === 409) {
                        console.error("Organization facility already exists", error);
                        this.$opensilex.errorHandler(
                                error,
                                this.$t(
                                        "OrganizationFacilityForm.organization-facility-already-exists"
                                )
                        );
                    } else {
                        this.$opensilex.errorHandler(error);
                    }
                });
    }

    private callOrganizationFacilityUpdate(form: FacilityUpdateDTO) {
        let definedRelations = [];
        for (let i in form.relations) {
            let relation = form.relations[i];
            if (relation.value != null) {
                if (Array.isArray(relation.value)) {
                    for (let j in relation.value) {
                        definedRelations.push({
                            property: relation.property,
                            value: relation.value[j],
                        });
                    }
                } else {
                    definedRelations.push(relation);
                }
            }
        }

        form.relations = definedRelations;
        return this.$opensilex
                .getService<OrganizationsService>("opensilex.OrganizationsService")
                .updateFacility(form)
                .then((http: HttpResponse<OpenSilexResponse<string>>) => {
                    let message = this.$i18n.t("OrganizationFacilityForm.name") + " " + form.name + " " + this.$i18n.t("component.common.success.update-success-message");
                    this.$opensilex.showSuccessToast(message);
                })
                .catch(this.$opensilex.errorHandler);
    }

    //endregion
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    OrganizationFacilityForm:
        name: The facility
        facility-uri: Organization facility URI
        form-name-placeholder: Enter organization facility name
        form-type-placeholder: Select organization facility type
        organization-facility-already-exists: Organization facility already exists with this URI
        geometryIsNotSaved: Geospatial information has been entered but has not been confirmed
fr:
    OrganizationFacilityForm:
        name: L'installation environnementale
        facility-uri: URI de l'installation environnementale
        form-name-placeholder: Saisir le nom de l'installation environnementale
        form-type-placeholder: Sélectionner le type de l'installation environnementale
        organization-facility-already-exists: Une installation environnementale existe déjà avec cette URI
        geometryIsNotSaved: Des informations géospatiales ont été saisies mais n'ont pas été confirmées
</i18n>