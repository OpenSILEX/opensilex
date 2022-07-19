<template>
    <b-form>
        <!-- URI -->
        <opensilex-UriForm
            :uri.sync="form.uri"
            label="DeviceForm.uri"
            :editMode="editMode"
            :generated.sync="uriGenerated"
            helpMessage="DeviceForm.uri-help"
        ></opensilex-UriForm>

        <!-- rdfType -->
        <opensilex-TypeForm
            :type.sync="form.rdf_type"
            :baseType="this.baseType"
            helpMessage="DeviceForm.type-help"
            :required="true"
            :multiple="false"
            :disabled="editMode"
            :ignoreRoot="false"
            @select="typeSwitch($event.id,false)"
        ></opensilex-TypeForm>

        <!-- name -->
        <opensilex-InputForm
            :value.sync="form.name"
            label="component.common.name"
            type="text"
            helpMessage="DeviceForm.name-help"
            :required="true"
        ></opensilex-InputForm>

        <!-- description -->
        <opensilex-TextAreaForm
            :value.sync="form.description"
            label="DeviceForm.description"
            type="text"
            helpMessage="DeviceForm.description-help"
        ></opensilex-TextAreaForm>

        <!-- brand -->
        <opensilex-InputForm
            :value.sync="form.brand"
            label="DeviceForm.brand"
            type="text"
            helpMessage="DeviceForm.brand-help"
        ></opensilex-InputForm>

        <!-- constructor_model -->
        <opensilex-InputForm
            :value.sync="form.constructor_model"
            label="DeviceForm.constructor_model"
            type="text"
            helpMessage="DeviceForm.constructor_model-help"
        ></opensilex-InputForm>

        <!-- serial_number -->
        <opensilex-InputForm
            :value.sync="form.serial_number"
            label="DeviceForm.serial_number"
            type="text"
            helpMessage="DeviceForm.serial_number-help"
        ></opensilex-InputForm>

        <!--person_in_charge -->
        <opensilex-UserSelector
            :users.sync="form.person_in_charge"
            label="DeviceForm.person_in_charge"
            helpMessage="DeviceForm.person_in_charge-help"
        ></opensilex-UserSelector>

        <!-- Period -->
        <opensilex-DateRangePickerForm
            :start.sync="form.start_up"
            :end.sync="form.removal"
            labelStart="DeviceForm.start_up"
            labelEnd="DeviceForm.removal"
            helpMessageStart="DeviceForm.start_up-help"
            helpMessageEnd="DeviceForm.removal-help"
        ></opensilex-DateRangePickerForm>

        <opensilex-OntologyRelationsForm
            ref="ontologyRelationsForm"
            :rdfType="this.form.rdf_type"
            :relations="this.form.relations"
            :excludedProperties="this.excludedProperties"
            :baseType="this.baseType"
            :editMode="editMode"
        ></opensilex-OntologyRelationsForm>

        <!-- metadata -->
        <opensilex-AttributesTable
          ref="attributeTable"
          :attributesArray='attributesArray'
        ></opensilex-AttributesTable>

    </b-form>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {DevicesService} from "opensilex-core/index";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import OntologyRelationsForm from "../../ontology/OntologyRelationsForm.vue";
import {DeviceCreationDTO} from "opensilex-core/model/deviceCreationDTO";
import AttributesTable from "../../common/forms/AttributesTable.vue";

@Component
export default class DeviceForm extends Vue {
    $opensilex: OpenSilexVuePlugin;
    service: DevicesService;
    $store: any;
    $t: any;

    excludedProperties: Set<string>;

    uriGenerated = true;

    @Ref("attributeTable") readonly attTable!: AttributesTable;
    @Ref("ontologyRelationsForm") readonly ontologyRelationsForm!: OntologyRelationsForm;

    @Prop()
    editMode;

    baseType: string = "";

    @Prop({default: () => DeviceForm.getEmptyForm()})
    form: DeviceCreationDTO;

    created(){
        this.service = this.$opensilex.getService("opensilex.DevicesService");
        this.baseType = this.$opensilex.Oeso.DEVICE_TYPE_URI;

        this.excludedProperties = new Set<string>([
            this.$opensilex.Rdfs.LABEL,
            this.$opensilex.Rdfs.COMMENT,
            this.$opensilex.Oeso.HAS_MODEL,
            this.$opensilex.Oeso.HAS_BRAND,
            this.$opensilex.Oeso.HAS_SERIAL_NUMBER,
            this.$opensilex.Oeso.PERSON_IN_CHARGE,
            this.$opensilex.Oeso.START_UP,
            this.$opensilex.Oeso.REMOVAL,
        ]);

    }

    get user() {
        return this.$store.state.user;
    }

    getEmptyForm() {
        return DeviceForm.getEmptyForm();
    }

    static getEmptyForm() : DeviceCreationDTO {
        return {
            uri: undefined,
            name: undefined,
            rdf_type: undefined,
            brand: undefined,
            constructor_model: undefined,
            serial_number: undefined,
            person_in_charge: undefined,
            start_up: undefined,
            removal: undefined,
            description: undefined,
            metadata: undefined,
            relations: []
        };
    }

    reset() {
        this.uriGenerated = true;
    }

    create(form){
        form.metadata = this.attTable.pushAttributes();
        return this.service.createDevice(false, form).then((http) => {
            form.uri = http.response.result;
            return form;
        }).catch(this.$opensilex.errorHandler);
    }

    update(form: DeviceCreationDTO) {
        form.metadata = this.attTable.pushAttributes();
        return this.service.updateDevice(form).then(() => {
            return form;
        }).catch(this.$opensilex.errorHandler);
    }

    attributesArray = [];

    readAttributes(metadata: { [key: string]: string; }) {
        AttributesTable.readAttributes(metadata,this.attributesArray);
    }

    typeSwitch(type: string, initialLoad: boolean) {
        this.ontologyRelationsForm.typeSwitch(type,initialLoad);
    }

}

</script>

<style scoped lang="scss">
</style>

<i18n>

en:
    DeviceForm:
        uri: URI
        uri-help: Unique device identifier autogenerated
        type: Type
        type-help: Device Type
        name: The device
        name-help: A name given to the device
        brand: Brand
        brand-help: A brand of the device
        constructor_model: Constructor model
        constructor_model-help: A constructor model of the device
        serial_number: Serial number
        serial_number-help: A serial number of the device
        person_in_charge: Person in charge
        person_in_charge-help: Person in charge of the device
        start_up: Start up
        start_up-help: Date of start up
        removal: Removal
        removal-help: Date of removal
        description: Description
        description-help: Description associated of the device
        variable: Variable
        variable-help: Insert one or several URI's variables

fr:
    DeviceForm:
        uri: URI
        uri-help: Identifiant unique du dispositif généré automatiquement
        type: Type
        type-help: Type de dispositif
        name: Le dispositif
        name-help: Nom du dispositif
        brand: Marque du dispositif
        brand-help: Marque du dispositif
        constructor_model: Modèle constructeur
        constructor_model-help: Modèle constructeur du dispositif
        serial_number: Numéro de série
        serial_number-help: Numéro de série du dispositif
        person_in_charge: Personne responsable
        person_in_charge-help: Personne responsable du dispositif
        start_up: Date d'obtention
        start_up-help: Date d'obtention du dispositif
        removal: Date de mise hors service
        removal-help: Date de mise hors service du dispositif
        description: Description
        description-help: Description associée au dispositif
        variable: Variable
        variable-help: Insérer une ou plusieurs URI de variables
</i18n>
