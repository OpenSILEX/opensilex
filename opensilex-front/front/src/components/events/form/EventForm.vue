<template>
    <ValidationObserver ref="validatorRef">

        <div class="row">
            <div class="col">
                <opensilex-UriForm
                        :uri.sync="form.uri"
                        label="component.common.uri"
                        :editMode="editMode"
                        :generated.sync="uriGenerated"
                        :required="true"
                ></opensilex-UriForm>
            </div>
        </div>

        <div class="row">
            <div class="col">
                <!-- Type -->
                <opensilex-TypeForm
                        :type.sync="form.rdf_type"
                        :baseType="baseType"
                        :required="true"
                        placeholder="Event.type-placeholder"
                        @update:type="typeSwitch"
                ></opensilex-TypeForm>
            </div>
        </div>

        <div class="row">
            <div class="col">
                <opensilex-TagInputForm
                        :value.sync="form.targets"
                        :baseType="$opensilex.Oeev.CONCERNS"
                        label="Event.targets"
                        type="text"
                        :required="true"
                        helpMessage="Event.targets-help"
                ></opensilex-TagInputForm>
            </div>
        </div>

        <div class="row">
            <div class="col" v-if="! form.is_instant">
                <opensilex-DateTimeForm
                        :value.sync="form.start"
                        label="Event.start"
                        :maxDate="form.end"
                        :required="startRequired"
                        @change="updateRequiredProps"
                        helpMessage="Event.start-help"
                ></opensilex-DateTimeForm>
            </div>

            <div class="col">
                <opensilex-DateTimeForm
                        :value.sync="form.end"
                        :minDate="form.start"
                        label="Event.end"
                        :required="endRequired"
                        @change="updateRequiredProps"
                        helpMessage="Event.end-help"
                ></opensilex-DateTimeForm>
            </div>

        </div>

        <div class="row">
            <div class="col">

                <opensilex-FormField
                        :required="true"
                        label="Event.is-instant"
                        helpMessage="Event.is-instant-help"
                >
                    <template v-slot:field="field">
                        <b-form-checkbox v-model="form.is_instant" switch>
                        </b-form-checkbox>
                    </template>

                </opensilex-FormField>

            </div>
        </div>

        <br>

        <div class="row">
            <div class="col">
                <!-- Comment -->
                <opensilex-TextAreaForm
                        :value.sync="form.description"
                        label="component.common.description"
                >
                </opensilex-TextAreaForm>
            </div>
        </div>

        <slot v-bind:form="form"></slot>
        <div v-for="(v, index) in typeProperties" v-bind:key="index">
            <component
                    :is="v.definition.input_component"
                    :property="v.definition"
                    :value.sync="v.property"
                    @update:value="updateRelation($event, v.definition.property)"
            ></component>
        </div>

        <div>
            <opensilex-MoveForm v-if="isMove()"
                    :form.sync="form"
            ></opensilex-MoveForm>
        </div>

    </ValidationObserver>
</template>

<script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {EventCreationDTO} from "opensilex-core/model/eventCreationDTO";
    import {OntologyService} from "opensilex-core/api/ontology.service";
    import MoveForm from "./MoveForm.vue";
    import {MoveCreationDTO} from "opensilex-core/model/moveCreationDTO";

    @Component
    export default class EventForm extends Vue {

        @Ref("validatorRef") readonly validatorRef!: any;

        $opensilex: any;
        ontologyService: OntologyService;

        uriGenerated = true;
        editMode = false;

        errorMsg: String = "";

        @Prop({default: () => MoveForm.getEmptyForm()})
        form: MoveCreationDTO;

        propertyComponents = [];

        baseType: string = "";
        typeModel = null;

        startRequired = false;
        endRequired = true;

        created(){
            this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
            this.baseType = this.$opensilex.Oeev.EVENT_TYPE_URI;
        }

        static getEmptyForm() : EventCreationDTO{
            return {
                uri: undefined,
                rdf_type: undefined,
                relations: [],
                start: undefined,
                end: undefined,
                targets: [],
                description: undefined,
                is_instant: true
            };
        }

        getEmptyForm() {
            return MoveForm.getEmptyForm();
        }

        updateRequiredProps(){

            if (! this.form.is_instant) {
                this.endRequired = true;
            } else {
                if(this.form.start){
                    this.endRequired = false;
                }else {
                    this.endRequired = !this.form.end;
                }
            }
        }

        reset() {
            this.uriGenerated = true;
            return this.validatorRef.reset();
        }

        validate() {
            return this.validatorRef.validate();
        }

        handleErrorMessage(errorMsg: string) {
            this.errorMsg = errorMsg;
        }

        updateRelation(value, property) {
            for (let i in this.form.relations) {
                let relation = this.form.relations[i];
                if (relation.property == property) {
                    relation.value = value;
                    return;
                }
            }

            this.form.relations.push({
                property: property,
                value: value
            });
        }

        get typeProperties() {
            let internalTypeProperties = [];
            if (this.typeModel) {
                for (let i in this.typeModel.data_properties) {
                    let dataProperty = this.typeModel.data_properties[i];
                    if (dataProperty.property != "rdfs:label") {
                        let propValue = this.valueByProperties[dataProperty.property];
                        if (dataProperty.is_list) {
                            if (!propValue) {
                                propValue = [];
                            } else if (!Array.isArray(propValue)) {
                                propValue = [propValue];
                            }
                        }
                        internalTypeProperties.push({
                            definition: dataProperty,
                            property: propValue
                        });
                    }
                }

                for (let i in this.typeModel.object_properties) {
                    let objectProperty = this.typeModel.object_properties[i];
                    let propValue = this.valueByProperties[objectProperty.property];
                    if (objectProperty.is_list) {
                        if (!propValue) {
                            propValue = [];
                        } else if (!Array.isArray(propValue)) {
                            propValue = [propValue];
                        }
                    }
                    internalTypeProperties.push({
                        definition: objectProperty,
                        property: propValue
                    });
                }
            }

            return internalTypeProperties;
        }

        get valueByProperties(): Object {
            let valueByProperties = {};

            for (let i in this.form.relations) {
                let relation = this.form.relations[i];
                if (
                    valueByProperties[relation.property] &&
                    !Array.isArray(valueByProperties[relation.property])
                ) {
                    valueByProperties[relation.property] = [
                        valueByProperties[relation.property]
                    ];
                }

                if (Array.isArray(valueByProperties[relation.property])) {
                    valueByProperties[relation.property].push(relation.value);
                } else {
                    valueByProperties[relation.property] = relation.value;
                }
            }

            return valueByProperties;
        }


        typeSwitch(type) {

            if(! type || type == this.$opensilex.Oeev.EVENT_TYPE_URI || type == this.$opensilex.Oeev.EVENT_TYPE_PREFIXED_URI){
                this.typeModel = null;
                return;
            }

            // this.setMoveCreationDto();
            return this.$opensilex
                .getService("opensilex.VueJsOntologyExtensionService")
                .getRDFTypeProperties(this.form.rdf_type, this.baseType)
                .then(http => {
                    this.typeModel = http.response.result;
                    if (!this.editMode) {
                        let relations = [];
                        for (let i in this.typeModel.data_properties) {
                            let dataProperty = this.typeModel.data_properties[i];
                            if (dataProperty.is_list) {
                                relations.push({
                                    value: [],
                                    property: dataProperty.property
                                });
                            } else {
                                relations.push({
                                    value: null,
                                    property: dataProperty.property
                                });
                            }
                        }

                        for (let i in this.typeModel.object_properties) {
                            let objectProperty = this.typeModel.object_properties[i];
                            if (objectProperty.is_list) {
                                relations.push({
                                    value: [],
                                    property: objectProperty.property
                                });
                            } else {
                                relations.push({
                                    value: null,
                                    property: objectProperty.property
                                });
                            }
                        }

                        this.form.relations = relations;
                    }
                });

        }

        isMove(): boolean {
            if ( ! this.form){
                return false;
            }
            return this.form.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_URI
                || this.form.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_PREFIXED_URI
        }

    }
</script>
