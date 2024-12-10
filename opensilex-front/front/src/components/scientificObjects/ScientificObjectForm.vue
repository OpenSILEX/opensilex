<template>
    <opensilex-ModalForm
            ref="modalForm"
            component="opensilex-OntologyObjectForm"
            createTitle="ExperimentScientificObjects.add"
            editTitle="ExperimentScientificObjects.update"
            modalSize="lg"
            icon="ik#ik-target"
            :createAction="callScientificObjectCreation"
            :updateAction="callScientificObjectUpdate"
            @onCreate="$emit('onCreate', $event)"
            @onUpdate="$emit('onUpdate', $event)"
    >
        <template v-slot:customFields="{ form }">
            <b-form-checkbox v-if="!editMode" v-model="hasMove" switches @change="onToggleMove">{{ $t("ScientificObjectForm.toggleLocation") }}</b-form-checkbox>
            <br>
            <div v-if="hasMove">
                <div class="row">
                    <div class="col">
                        <opensilex-DateTimeForm
                                :value.sync="formMove.start"
                                label="Event.start"
                        ></opensilex-DateTimeForm>
                    </div>

                    <div class="col">
                        <opensilex-DateTimeForm
                                :value.sync="formMove.end"
                                label="Event.end"
                                :required="!!formMove.start || !!locationFilled"
                        ></opensilex-DateTimeForm>
                    </div>

                </div>
                <opensilex-MoveForm :form.sync="formMove" :locationFilled.sync="locationFilled" ref="moveForm"></opensilex-MoveForm>
            </div>
        </template>
    </opensilex-ModalForm>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {ScientificObjectsService} from "opensilex-core/index";
import OntologyObjectForm from "../ontology/OntologyObjectForm.vue";
import ModalForm from "../common/forms/ModalForm.vue";
import {MultiValuedRDFObjectRelation} from "../ontology/models/MultiValuedRDFObjectRelation";
import Oeso from "../../ontologies/Oeso";
import Rdfs from "../../ontologies/Rdfs";
import {ScientificObjectDetailDTO} from "opensilex-core/model/scientificObjectDetailDTO";
import {ScientificObjectCreationDTO} from "opensilex-core/model/scientificObjectCreationDTO";
import {ScientificObjectUpdateDTO} from "opensilex-core/model/scientificObjectUpdateDTO";
import DTOConverter from "../../models/DTOConverter";
import {UserGetDTO} from "../../../../../opensilex-security/front/src/lib";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import MoveForm from "@/components/events/form/MoveForm.vue";
import {MoveCreationDTO} from "opensilex-core/model/moveCreationDTO";

@Component
export default class ScientificObjectForm extends Vue {
//#region Plugins and services
    private readonly $opensilex: OpenSilexVuePlugin;
    private soService: ScientificObjectsService;
    //endregion

    //#region Props
    @Prop({default: () => {}})
    private readonly context;
    //endregion

    //#region Refs
    @Ref("modalForm")
    private readonly modalForm!: ModalForm<OntologyObjectForm, ScientificObjectCreationDTO, ScientificObjectUpdateDTO>;
    @Ref("moveForm")
    private readonly moveForm!: MoveForm;
    //endregion

    //#region Data
    private hasMove: boolean = false;
    private formMove: MoveCreationDTO = MoveForm.getEmptyForm();
    private editMode:boolean = false;
    private locationFilled: boolean = false;
    //endregion

    //#region Computed
    //endregion

    //#region Events
    //endregion

    //#region Events handlers
    public onToggleMove(){
        this.formMove= MoveForm.getEmptyForm();
    }
    //endregion

    //#region Public methods
    public createScientificObject(parentURI?) {
        let form: OntologyObjectForm = this.modalForm.getFormRef();
        this.initOntologyObjectForm(form, undefined);

        // if parentURI property is set, then use this value as default isPartOf relation value
        form.setInitHandler((relation: MultiValuedRDFObjectRelation) => {
            if (parentURI) {
                if (this.$opensilex.Oeso.checkURIs(relation.property.uri, this.$opensilex.Oeso.IS_PART_OF)) {
                    Vue.set(relation, "value", parentURI);
                    form.updateRelations();
                }
            }
        });

        this.modalForm.showCreateForm();
    }

    public editScientificObject(objectURI: string) {
        this.soService
                .getScientificObjectDetail(objectURI, this.getExperimentURI())
                .then((http) => {
                    let form: OntologyObjectForm = this.modalForm.getFormRef();
                    let os: ScientificObjectDetailDTO = http.response.result;

                    this.initOntologyObjectForm(form, os.rdf_type);
                    this.excludeCurrentURIFromParentSelector(objectURI, form);
                    let publisher: UserGetDTO = os.publisher;
                    const editDto = DTOConverter.extractURIFromResourceProperties<ScientificObjectDetailDTO, ScientificObjectUpdateDTO>(os);
                    editDto.publisher = publisher;

                    this.modalForm.showEditForm(editDto);
                    this.editMode = true;
                });
    }
    //endregion

    //#region Hooks
    private created() {
        this.soService = this.$opensilex.getService("opensilex.ScientificObjectsService");
    }
    //endregion

    //#region Private methods
    /**
     * Inner function used to pass some properties to the OntologyObjectForm
     * @param form
     * @param type
     */
    private initOntologyObjectForm(form: OntologyObjectForm, type: string) {

        let excludedProperties = new Set<string>([
            Oeso.getShortURI(Oeso.HAS_GEOMETRY), // location with move
            Rdfs.getShortURI(Rdfs.LABEL), // let OntologyObjectForm handle rdfs:label by default
            Oeso.getShortURI(Oeso.IS_HOSTED) //is_hosted has to be calculated (from move)
        ]);

        let xp: string = this.getExperimentURI();
        form.setContext(xp)
        if (!type) {
            form.setBaseType(this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI, this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);
        } else {
            form.setBaseType(type, this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI)
        }
        form.setExcludedProperties(excludedProperties);

        if (!xp) {
            form.setLoadCustomProperties(false);
        }
    }

    private excludeCurrentURIFromParentSelector(objectURI: string, form: OntologyObjectForm) {
        let customComponentProps = new Map<string, Map<string, any>>();

        let isPartOf = Oeso.getShortURI(Oeso.IS_PART_OF);
        customComponentProps.set(isPartOf, new Map<string, any>());
        customComponentProps.get(isPartOf).set("excluded", new Set<string>([objectURI]));

        form.setCustomComponentProps(customComponentProps);
    }

    private getExperimentURI() {
        if (this.context && this.context.experimentURI) {
            return this.context.experimentURI;
        }
        return undefined;
    }

    private callScientificObjectCreation(form: ScientificObjectCreationDTO) {
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

        return this.soService
                .createScientificObject({
                    uri: form.uri,
                    name: form.name,
                    rdf_type: form.rdf_type,
                    move: this.formMove,
                    experiment: this.getExperimentURI(),
                    relations: definedRelations,
                })
                .catch((error) => {
                    this.$opensilex.errorHandler(error, error.response.result.message);
                    throw error;
                });
    }

    private callScientificObjectUpdate(form: ScientificObjectUpdateDTO) {
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

        return this.soService
                .updateScientificObject({
                    uri: form.uri,
                    name: form.name,
                    rdf_type: form.rdf_type,
                    move: this.formMove,
                    publisher: form.publisher,
                    publication_date: form.publication_date,
                    experiment: this.getExperimentURI(),
                    relations: definedRelations
                })
                .catch((error) => {
                    this.$opensilex.errorHandler(error, error.response.result.message);
                    throw error;
                });
    }
    //endregion
}
</script>
<style scoped lang="scss">
</style>

<i18n>
en:
    ScientificObjectForm:
        toggleLocation: Add location
fr:
    ScientificObjectForm:
        toggleLocation: Ajouter une localisation
</i18n>
