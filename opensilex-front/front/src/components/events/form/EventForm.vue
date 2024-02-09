<template>
    <ValidationObserver ref="validatorRef">

        <div class="row">
            <div class="col" v-if="!linkedToAreaForm">
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
                    ref="typeForm"
                    :type.sync="form.rdf_type"
                    :baseType="baseType"
                    :ignoreRoot="false"
                    :required="false"
                    :disabled="editMode"
                    placeholder="Event.type-placeholder"
                    @select="typeSwitch($event.id,false)"
                    @open="customOptionsTypes"
                ></opensilex-TypeForm>
            </div>
        </div>

        <div class="row">
            <div class="col" v-if="!linkedToAreaForm">
                <!-- Target -->
                <opensilex-TagInputForm
                    :value.sync="form.targets"
                    :baseType="this.$opensilex.Oeev.CONCERNS"
                    label="Event.targets"
                    type="text"
                    :required="true"
                    helpMessage="Event.targets-help"
                ></opensilex-TagInputForm>
            </div>
        </div>

      <div class="row">
        <div class="col" v-if="!linkedToAreaForm">
          <!-- Description -->
          <opensilex-TextAreaForm
              :value.sync="form.description"
              label="component.common.description"
              helpMessage="Event.description"
              placeholder="Event.description"
              @keydown.native.enter.stop
          >
          </opensilex-TextAreaForm>
        </div>
      </div>

        <div class="row">
            <div class="col">
                <!--Is instant-->
                <opensilex-FormField
                    :required="true"
                    label="Event.is-instant"
                    helpMessage="Event.is-instant-help"
                >
                    <template v-slot:field="field">
                        <b-form-checkbox v-model="form.is_instant" switch @change="updateIsInstantFilter">
                        </b-form-checkbox>
                    </template>
                </opensilex-FormField>
            </div>
        </div>

        <div class="row">
            <div class="col" v-if="!form.is_instant">
                <opensilex-DateTimeForm
                    ref="startDateSelector"
                    :value.sync="form.start"
                    label="Event.start"
                    :maxDate="form.end"
                    :required="startRequired"        
                    helpMessage="Event.start-help"
                    @input="updateRequiredProps('startDateSelector')"
                    @clear="updateRequiredProps('startDateSelector')"
                ></opensilex-DateTimeForm>
            </div>

            <div class="col">
                <opensilex-DateTimeForm
                    ref="endDateSelector"
                    :value.sync="form.end"
                    label="Event.end"
                    :required="endRequired"               
                    helpMessage="Event.end-help"
                    @input="updateRequiredProps('endDateSelector')"
                    @clear="updateRequiredProps('endDateSelector')"
                ></opensilex-DateTimeForm>
            </div>

        </div>

        <br>
        <slot v-bind:form="form"></slot>

        <opensilex-OntologyRelationsForm
            ref="ontologyRelationsForm"
            :rdfType="this.form.rdf_type"
            :relations="this.form.relations"
            :excludedProperties="this.excludedProperties"
            :baseType="this.baseType"
            :editMode="editMode"
            :context="context ? { experimentURI: context} : undefined"
        ></opensilex-OntologyRelationsForm>

        <div>
            <opensilex-MoveForm v-if="isMove()" :form.sync="form" ref="moveForm"></opensilex-MoveForm>
        </div>

    </ValidationObserver>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {OntologyService} from "opensilex-core/api/ontology.service";
import MoveForm from "./MoveForm.vue";
import {VueJsOntologyExtensionService} from "../../../lib";
import OpenSilexVuePlugin from 'src/models/OpenSilexVuePlugin';
import OntologyRelationsForm from "../../ontology/OntologyRelationsForm.vue";
import {EventCreationDTO, MoveCreationDTO } from 'opensilex-core/index';
import TypeForm from "../../common/forms/TypeForm.vue";

@Component
export default class EventForm extends Vue {

    @Ref("validatorRef") readonly validatorRef!: any;
    @Ref("moveForm") readonly moveForm!: MoveForm;

    $opensilex: OpenSilexVuePlugin;
    ontologyService: OntologyService;
    vueOntologyService: VueJsOntologyExtensionService;
    uriGenerated = true;

    @Prop({default: false})
    editMode: boolean;

    errorMsg: String = "";

    @Prop({default: () => MoveForm.getEmptyForm()})
    form: MoveCreationDTO;

    @Prop({default: false})
    linkedToAreaForm: boolean;

    @Ref("ontologyRelationsForm") readonly ontologyRelationsForm!: OntologyRelationsForm;
    @Ref("typeForm") readonly typeForm!: TypeForm;

    excludedProperties: Set<string>;

    context: string = "";

    baseType: string = "";
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

        this.excludedProperties = new Set<string>([
            this.$opensilex.Oeev.CONCERNS,
            this.$opensilex.Oeev.IS_INSTANT,
            this.$opensilex.Time.HAS_BEGINNING,
            this.$opensilex.Time.HAS_END,
            this.$opensilex.Rdfs.COMMENT,

            // from/to properties to handle into MoveForm
            this.$opensilex.Oeev.FROM,
            this.$opensilex.Oeev.TO
        ]);
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

    setContext(context) {
        this.context = context;
    }
 
    updateIsInstantFilter(ref){
        this.$emit('change');
        this.updateRequiredProps(ref)
    } 

    updateRequiredProps(ref){
        if (this.form.end === "") {
            this.form.end = undefined
        }
        if (this.form.start === ""){
            this.form.start = undefined
        }

        if (this.form.is_instant) {
            this.endRequired = true;
        } else {           
            if(this.form.start == undefined && this.form.end == undefined) {
                this.startRequired = true;
                this.endRequired = true; 
            } else {
                this.startRequired = !!this.form.start;
                this.endRequired = !!this.form.end;
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

    typeSwitch(type: string, initialLoad: boolean) {
        this.ontologyRelationsForm.typeSwitch(type, initialLoad);
    }

    isMove(): boolean {
        if (!this.form || ! this.form.rdf_type) {
            return false;
        }

        return this.$opensilex.Oeev.checkURIs(this.form.rdf_type, this.$opensilex.Oeev.MOVE_TYPE_URI);
    }
    //TODO : when the eventForm is used in the area context, the rdftype "move" is disabled because don't make sense : an area can't move (temporary until creation of specific service)
    customOptionsTypes(){
      if(this.linkedToAreaForm && this.typeForm.typesOptions){
        //Get rdfType options of events loaded from the field typeForm
        let listOptions = this.typeForm.typesOptions;
        // find the rdfType "move" in event options
        let move = listOptions[0].children.find(option => option.label === "Move");
        // add the propriety disabled to "move"
        move.isDisabled = true;
      }
    }

    handleSubmitError(){
        this.moveForm.handleSubmitError()
    }
}
</script>
