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
            <opensilex-GeometryForm
                :value.sync="form.geometry"
                label="component.common.geometry"
                helpMessage="component.common.geometry-help"
            ></opensilex-GeometryForm>
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

@Component
export default class ScientificObjectForm extends Vue {
    $opensilex: any;
    $store: any;

    soService: ScientificObjectsService;

    @Prop({
        default: () => {},
    })
    context;

    @Ref("modalForm") readonly modalForm!: ModalForm;

    created() {
        this.soService = this.$opensilex.getService(
            "opensilex.ScientificObjectsService"
        );
    }

    /**
     * Inner function used to pass some properties to the OntologyObjectForm
     * @param form
     * @param type
     */
    initOntologyObjectForm(form: OntologyObjectForm, type: string){

        let excludedProperties = new Set<string>([
            Oeso.getShortURI(Oeso.HAS_GEOMETRY), // handle geometry manually with opensilex-GeometryForm
            Rdfs.getShortURI(Rdfs.LABEL) // let OntologyObjectForm handle rdfs:label by default
        ]);

        let xp: string = this.getExperimentURI();
        form.setContext(xp)
        if (!type) {
            form.setBaseType(this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI, this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);
        } else {
            form.setBaseType(type, this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI)
        }
        form.setExcludedProperties(excludedProperties);

        if(! xp){
          form.setLoadCustomProperties(false);
        }
    }

    excludeCurrentURIFromParentSelector(objectURI: string, form: OntologyObjectForm){
        let customComponentProps = new Map<string, Map<string, any>>();

        let isPartOf = Oeso.getShortURI(Oeso.IS_PART_OF);
        customComponentProps.set(isPartOf, new Map<string, any>());
        customComponentProps.get(isPartOf).set("excluded", new Set<string>([objectURI]));

        form.setCustomComponentProps(customComponentProps);
    }

    createScientificObject(parentURI?) {
        let form: OntologyObjectForm = this.modalForm.getFormRef();
        this.initOntologyObjectForm(form, undefined);

        // if parentURI property is set, then use this value as default isPartOf relation value
        form.setInitHandler((relation: MultiValuedRDFObjectRelation) => {
            if (parentURI) {
                if (this.$opensilex.Oeso.checkURIs(relation.property.uri, this.$opensilex.Oeso.IS_PART_OF)) {
                    Vue.set(relation, "value", parentURI);
                }
            }
        });

        this.modalForm.showCreateForm();
    }

    editScientificObject(objectURI: string) {
        this.soService
            .getScientificObjectDetail(objectURI, this.getExperimentURI())
            .then((http) => {
                let form: OntologyObjectForm = this.modalForm.getFormRef();
                let os: ScientificObjectDetailDTO = http.response.result;

                this.initOntologyObjectForm(form, os.rdf_type);
                this.excludeCurrentURIFromParentSelector(objectURI, form);
                this.modalForm.showEditForm(os);
            });
    }

    getExperimentURI() {
        if (this.context && this.context.experimentURI) {
            return this.context.experimentURI;
        }
        return undefined;
    }

    callScientificObjectCreation(form) {
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
                geometry: form.geometry,
                experiment: this.getExperimentURI(),
                relations: definedRelations,
            })
            .catch((error) => {
                this.$opensilex.errorHandler(error, error.response.result.message);
                throw error;
            });
    }

    callScientificObjectUpdate(form) {
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
                geometry: form.geometry,
                experiment: this.getExperimentURI(),
                relations: definedRelations,
            })
            .catch((error) => {
                this.$opensilex.errorHandler(error, error.response.result.message);
                throw error;
            });
    }
}
</script>
<style scoped lang="scss">
</style>
