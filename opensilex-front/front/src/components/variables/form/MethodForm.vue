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
                <b-form-group
                    label="component.skos.ontologies-references-label"
                    label-size="lg"
                    label-class="font-weight-bold pt-0"
                    class="mb-0"
                >
                    <template v-slot:label>{{ $t('component.skos.ontologies-references-label') }}</template>
                </b-form-group>
                <b-card-text>
                    <ul>
                        <li
                            v-for="externalOntologyRef in externalOntologiesRefs"
                            :key="externalOntologyRef.label"
                        >
                            <a
                                target="_blank"
                                v-bind:title="externalOntologyRef.label"
                                v-bind:href="externalOntologyRef.link"
                                v-b-tooltip.v-info.hover.left="externalOntologyRef.description"
                            >{{ externalOntologyRef.label }}</a>
                        </li>
                    </ul>
                </b-card-text>
            </div>

            <div class="col">

                <!-- Name -->
                <opensilex-InputForm
                    :value.sync="form.name"
                    label="component.common.name"
                    type="text"
                    :required="true"
                    placeholder="MethodForm.name-placeholder"
                ></opensilex-InputForm>

                <!-- Comment -->
                <opensilex-TextAreaForm
                    :value.sync="form.comment"
                    label="component.common.description">
                </opensilex-TextAreaForm>
            </div>
        </div>
    </ValidationObserver>
</template>


<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {MethodCreationDTO} from "opensilex-core/model/methodCreationDTO";
    import {ExternalOntologies} from "../../../models/ExternalOntologies";
    import MethodCreate from "./MethodCreate.vue";

    @Component
    export default class MethodForm extends Vue {
        $opensilex: any;

        title = "";
        uriGenerated = true;
        editMode = false;

        errorMsg: String = "";

        @PropSync("form")
        methodDto: MethodCreationDTO;

        externalOntologiesRefs: any[] = ExternalOntologies.getExternalOntologiesReferences(MethodCreate.selectedOntologies);

        handleErrorMessage(errorMsg: string) {
            this.errorMsg = errorMsg;
        }

        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        reset() {
            this.uriGenerated = true;
            return this.validatorRef.reset();
        }

        validate() {
            return this.validatorRef.validate();
        }
    }
</script>

<style scoped lang="scss">
</style>