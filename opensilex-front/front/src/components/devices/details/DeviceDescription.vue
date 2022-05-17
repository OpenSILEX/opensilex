<template>
    <div class="container-fluid">
        <opensilex-PageContent>
            <b-row>
                <b-col>

                    <opensilex-Card
                        label="DeviceDescription.description"
                        icon="ik#ik-clipboard"
                    >
                        <template v-slot:rightHeader>
                            <opensilex-EditButton
                                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_DEVICE_MODIFICATION_ID
                  )
                "
                                label="DeviceDescription.update"
                                @click="editForm"
                            ></opensilex-EditButton>
                            <opensilex-DeleteButton
                                v-if="
                  user.hasCredential(credentials.CREDENTIAL_DEVICE_DELETE_ID)
                "
                                label="DeviceDescription.delete"
                                :small="true"
                                @click="deleteDevice(device.uri)"
                            ></opensilex-DeleteButton>
                        </template>
                        <template v-slot:body>
                            <opensilex-UriView :uri="device.uri"></opensilex-UriView>
                            <opensilex-StringView
                                label="DeviceDescription.type"
                                :value="device.rdf_type_name"
                            ></opensilex-StringView>
                            <opensilex-StringView
                                label="DeviceDescription.name"
                                :value="device.name"
                            ></opensilex-StringView>
                            <opensilex-StringView
                                label="DeviceDescription.brand"
                                :value="device.brand"
                            ></opensilex-StringView>
                            <opensilex-StringView
                                label="DeviceDescription.constructorModel"
                                :value="device.constructor_model"
                            ></opensilex-StringView>
                            <opensilex-StringView
                                label="DeviceDescription.serialNumber"
                                :value="device.serial_number"
                            ></opensilex-StringView>
                            <opensilex-StringView
                                label="DeviceDescription.personInCharge"
                                :value="device.person_in_charge"
                            ></opensilex-StringView>
                            <opensilex-StringView
                                label="DeviceDescription.start_up"
                                :value="device.start_up"
                            ></opensilex-StringView>
                            <opensilex-StringView
                                label="DeviceDescription.removal"
                                :value="device.removal"
                            ></opensilex-StringView>
                            <opensilex-StringView
                                label="DeviceDescription.description"
                                :value="device.description"
                            ></opensilex-StringView>

                            <div :key="index" v-for="(relation, index) in device.relations">

                                <!-- Display custom properties (except variables, since they are displayed in another component) -->
                                <opensilex-StringView
                                    v-if="! isIsPartOfRelation(relation) && ! isVariableRelation(relation)"
                                    :label="getPropertyName(relation.property)"
                                    :value="relation.value"
                                ></opensilex-StringView>

                                <!-- is Part of -->
                                <opensilex-UriView
                                    v-else-if="isIsPartOfRelation(relation)"
                                    :title="getPropertyName(relation.property)"
                                    :uri="relation.value"
                                    :value="relation.value"
                                    :to="{ path: '/device/details/' + encodeURIComponent(relation.value) }"
                                ></opensilex-UriView>

                            </div>

                        </template>
                    </opensilex-Card>

                    <opensilex-Card
                        label="DeviceDescription.additionalInfo"
                        icon="ik#ik-clipboard"
                        v-if="addInfo.length !== 0"
                    >
                        <template v-slot:body>
                            <b-table
                                ref="tableAtt"
                                striped
                                hover
                                small
                                responsive
                                :fields="attributeFields"
                                :items="addInfo"
                            >
                                <template v-slot:head(attribute)="data">{{
                                    $t(data.label)
                                    }}
                                </template>
                                <template v-slot:head(value)="data">{{
                                    $t(data.label)
                                    }}
                                </template>
                            </b-table>
                        </template>
                    </opensilex-Card>
                </b-col>
                <b-col>
                    <b-row>
                        <!-- Variables -->
                        <opensilex-Card
                            label="DeviceDescription.variables"
                            icon="ik#ik-clipboard"
                        >
                            <template v-slot:body>
                                <opensilex-TableView
                                    v-if="device.relations.length !== 0"
                                    :items="device.relations.filter(relation => isVariableRelation(relation))"
                                    :fields="relationsFields"
                                    :globalFilterField="true"
                                >
                                    <template v-slot:cell(uri)="{ data }">
                                        <opensilex-UriLink
                                            :uri="data.item.value"
                                            :value="data.item.value"
                                            :to="{
                      path:
                        '/variable/details/' +
                        encodeURIComponent(data.item.value),
                    }"
                                        ></opensilex-UriLink>
                                    </template>
                                </opensilex-TableView>

                                <p v-else>
                                    <strong>{{ $t("DeviceDescription.no-var-provided") }}</strong>
                                </p>
                            </template>
                        </opensilex-Card>
                    </b-row>
                </b-col>
            </b-row>
        </opensilex-PageContent>

      <opensilex-DeviceModalForm
          ref="deviceForm"
          @onUpdate="refresh()"
      ></opensilex-DeviceModalForm>

    </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {DevicesService, DeviceGetDetailsDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import {VueJsOntologyExtensionService, VueRDFTypeDTO} from "../../../lib";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import ModalForm from "../../common/forms/ModalForm.vue";
import DeviceModalForm from "../form/DeviceModalForm.vue";

@Component
export default class DeviceDescription extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;
    $route: any;
    $t: any;
    $i18n: any;
    service: DevicesService;
    vueJsOntologyService: VueJsOntologyExtensionService;

    uri: string = null;
    addInfo = [];

    baseType: string;
    propertyByUri: Map<string, VueRDFTypeDTO> = new Map();

    @Ref("deviceForm") readonly deviceForm!: DeviceModalForm;
    @Ref("tableAtt") readonly tableAtt!: any;

    get user() {
        return this.$store.state.user;
    }

    refresh() {
        this.loadDevice();
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    relationsFields: any[] = [
        {
            key: "uri",
            label: "DeviceDescription.uri",
            sortable: true,
        },
    ];

    device: DeviceGetDetailsDTO = {
        uri: null,
        rdf_type: null,
        name: null,
        brand: null,
        constructor_model: null,
        serial_number: null,
        person_in_charge: null,
        start_up: null,
        removal: null,
        description: null,
        metadata: null,
        relations: [],
    };

    created() {
        this.service = this.$opensilex.getService("opensilex.DevicesService");
        this.vueJsOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");

        this.uri = decodeURIComponent(this.$route.params.uri);
        this.baseType = this.$opensilex.Oeso.DEVICE_TYPE_URI;
        // this.$opensilex.hideLoader(); // TODO: don't wait on create that the form is created
        this.loadDevice();
    }

    loadDevice() {
        this.service
            .getDevice(this.uri)
            .then((http: HttpResponse<OpenSilexResponse<DeviceGetDetailsDTO>>) => {
                this.device = http.response.result;
                this.getAddInfo();
                this.loadProperties();
            })
            .catch(this.$opensilex.errorHandler);
    }

    loadProperties() {
        this.vueJsOntologyService.getRDFTypeProperties(this.device.rdf_type, this.baseType)
            .then((http: HttpResponse<OpenSilexResponse<VueRDFTypeDTO>>) => {
                let typeModel = http.response.result;

                this.propertyByUri = new Map();
                typeModel.data_properties.forEach(property => {
                    this.propertyByUri.set(property.uri, property);
                });
                typeModel.object_properties.forEach(property => {
                    this.propertyByUri.set(property.uri, property);
                });
            }).catch(this.$opensilex.errorHandler);
    }

    getAddInfo() {
        this.addInfo = [];
        for (const property in this.device.metadata) {
            let tableData = {
                attribute: property,
                value: this.device.metadata[property],
            };
            this.addInfo.push(tableData);
        }
    }

    attributeFields = [
        {
            key: "attribute",
            label: "DeviceDescription.attribute",
        },
        {
            key: "value",
            label: "DeviceDescription.value",
        },
    ];

    deleteDevice(uri: string) {
        this.service
            .deleteDevice(uri)
            .then(() => {
                this.$router.go(-1);
                this.$emit("onDelete", uri);
            })
            .catch((error) => {
                if (error.response.result.title && error.response.result.title === "LINKED_DEVICE_ERROR") {
                    let message = this.$i18n.t("DeviceList.associated-device-error") + " " + error.response.result.message;
                    this.$opensilex.showErrorToast(message);
                } else {
                    this.$opensilex.errorHandler(error);
                }
            });
    }

    @Ref("modalForm") readonly modalForm!: ModalForm;
    renderModalForm: boolean = false;

    editForm() {
        this.deviceForm.showEditForm(this.device.uri);
    }

    isVariableRelation(relation: RDFObjectRelationDTO): boolean {
        const measures_prop = this.$opensilex.Oeso.MEASURES_PROP_URI;
        return relation.property == measures_prop || relation.property == this.$opensilex.Oeso.getShortURI(measures_prop);
    }

    isIsPartOfRelation(relation: RDFObjectRelationDTO) : boolean {
      const measures_prop = this.$opensilex.Oeso.IS_PART_OF;
      return relation.property == measures_prop || relation.property == this.$opensilex.Oeso.getShortURI(measures_prop);
    }

    getPropertyName(propertyUri: string) {

        if (!propertyUri) {
            return undefined;
        }

        let property = this.propertyByUri.get(propertyUri);
        if (property) {
            return property.name;
        }
        return propertyUri;
    }

}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    DeviceDescription:
        events: Events
        title: Device
        description: Description
        uri: URI
        name: Name
        type: Type
        start_up: Start up
        brand: Brand
        constructorModel: Constructor model
        serialNumber: Serial number
        personInCharge: Person in charge
        localisation: Localisation
        variables: Variables
        position: Position
        update: Update device
        delete: Delete device
        additionalInfo: Metadata
        removal: Removal
        attribute: Attribute
        value: Value
        no-var-provided: No variable provided

fr:
    DeviceDescription:
        events: Événements
        title: Dispositif
        description: Description
        uri: URI
        name: Nom
        type: Type
        start_up: Date de mise en service
        brand: Marque
        constructorModel: Modèle du constructeur
        serialNumber: Numéro de série
        personInCharge: Personne responsable
        localisation: Localisation
        variables: Variables
        position: Position
        update: Modifier le dispositif
        delete: Supprimer ce dispositif
        additionalInfo: Métadonnées
        removal: Date de mise hors service
        attribute: Attribut
        value: Valeur
        no-var-provided: Aucune variable associée






</i18n>
