<template>
    <ValidationObserver ref="validatorRef">

        <div class="row">
            <div class="col">
                <opensilex-UriForm
                    :uri.sync="form.uri"
                    label="component.common.uri"
                    :editMode="editMode"
                    helpMessage="component.common.uri-help-message"
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
                    :ignoreRoot="false"
                    :required="false"
                    :disabled="editMode"
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

        <div class="row">
            <div class="col" v-if="! form.is_instant">
                <opensilex-DateTimeForm
                    :value.sync="form.start"
                    label="Event.start"
                    :maxDate="form.end"
                    :required="startRequired"
                    @update:value="updateRequiredProps"
                    helpMessage="Event.start-help"
                ></opensilex-DateTimeForm>
            </div>

            <div class="col">
                <opensilex-DateTimeForm
                    :value.sync="form.end"
                    label="Event.end"
                    :required="endRequired"
                    @update:value="updateRequiredProps"
                    helpMessage="Event.end-help"
                ></opensilex-DateTimeForm>
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

        <div v-for="(relation, index) in typeRelations" v-bind:key="index">
            <component
                :is="getInputComponent(relation.property)"
                :property="relation.property"
                :label="relation.property.name"
                :required="relation.property.is_required"
                :multiple="relation.property.is_list"
                :value.sync="relation.value"
                @update:value="updateRelation($event,relation.property)"
            ></component>
        </div>

        <div>
            <opensilex-MoveForm v-if="isMove()" :form.sync="form"></opensilex-MoveForm>
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
import {VueJsOntologyExtensionService, VueRDFTypeDTO} from "../../../lib";

@Component
export default class EventForm extends Vue {

    @Ref("validatorRef") readonly validatorRef!: any;

    $opensilex: any;
    ontologyService: OntologyService;
    vueOntologyService: VueJsOntologyExtensionService;
    uriGenerated = true;

    @Prop({default: false})
    editMode: boolean;

    errorMsg: String = "";

    @Prop({default: () => MoveForm.getEmptyForm()})
    form: MoveCreationDTO;

    baseType: string = "";
    typeModel = null;
    propertyComponents = [];

    startRequired = false;
    endRequired = true;

    propertyFilter = (property) => property;

    setTypePropertyFilterHandler(handler) {
        this.propertyFilter = handler;
    }

    created() {
        this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
        this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
        this.baseType = this.$opensilex.Oeev.EVENT_TYPE_URI;
    }

    static getEmptyForm(): EventCreationDTO {
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

    setBaseType(baseType) {
        this.baseType = baseType;
    }

    updateRequiredProps() {

        if (this.form.is_instant) {
            this.endRequired = true;
        } else {
            if (this.form.start) {
                this.startRequired = true;
                this.endRequired = false;
            } else {
                this.startRequired = true;
                this.endRequired = true;
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

    initHandler = () => {
    };

    setInitObjHandler(handler) {
        this.initHandler = handler;
    }


    getInputComponent(property) {
        if (property.input_components_by_property && property.input_components_by_property[property.property]) {
            return property.input_components_by_property[property.property];
        }
        return property.input_component;
    }

    resetTypeModel(){
        this.typeModel = undefined;
    }


    get typeRelations() {

        let internalTypeProperties = [];

        if (this.typeModel) {
            for (let i in this.typeModel.data_properties) {
                let dataProperty = this.typeModel.data_properties[i];
                if (dataProperty.property != "rdfs:label") {

                    let relation = this.form.relations.find(relation => relation.property == dataProperty.property);

                    internalTypeProperties.push({
                        property: dataProperty,
                        value: relation.value
                    });
                }
            }

            for (let i in this.typeModel.object_properties) {

                let objectProperty = this.typeModel.object_properties[i];
                let relation = this.form.relations.find(relation => relation.property == objectProperty.property);

                internalTypeProperties.push({
                    property: objectProperty,
                    value: relation.value
                });
            }
        }
        return internalTypeProperties;
    }

    typeSwitch(type) {

        if (!type) {
            return;
        }

        return this.vueOntologyService
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
                                value: undefined,
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
                                value: undefined,
                                property: objectProperty.property
                            });
                        }
                    }

                    this.form.relations = relations;
                }
            });

    }

    updateRelation(newValue,property){

        let relation = this.form.relations.find(relation =>
            relation.property == property.property
        );

        relation.value = newValue;
    }


    isMove(): boolean {
        if (!this.form) {
            return false;
        }
        return this.form.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_URI
            || this.form.rdf_type == this.$opensilex.Oeev.MOVE_TYPE_PREFIXED_URI
    }

}
</script>
