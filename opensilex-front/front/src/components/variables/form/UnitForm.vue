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
                    helpMessage="UnitForm.uri-help"
                ></opensilex-UriForm>
            </div>
        </div>

        <div class="row">

            <div class="col-lg-5">

                <!-- Name -->
                <opensilex-InputForm
                        :value.sync="form.name"
                        label="component.common.name"
                        type="text"
                        :required="true"
                        placeholder="UnitForm.name-placeholder"
                ></opensilex-InputForm>

                <!-- symbol -->
                <opensilex-InputForm
                        :value.sync="form.symbol"
                        label="UnitForm.symbol"
                        type="text"
                        placeholder="UnitForm.symbol-placeholder"
                ></opensilex-InputForm>

                <!-- alternative symbol -->
                <opensilex-InputForm
                        :value.sync="form.alternative_symbol"
                        label="UnitForm.alternative-symbol"
                        placeholder="UnitForm.alternative-symbol-placeholder"
                        type="text"
                        :required="false"
                ></opensilex-InputForm>

                <!-- Comment -->
                <opensilex-TextAreaForm
                        :value.sync="form.description"
                        label="component.common.description">
                </opensilex-TextAreaForm>
            </div>

            <div class="col">
                <b-form-group
                        label="component.skos.ontologies-references-label"
                        label-size="lg"
                        label-class="font-weight-bold pt-0"
                        class="mb-0"
                >
                    <template v-slot:label>{{ $t('component.skos.ontologies-references-label') }}</template>
                </b-form-group>

                <div class="row">
                    <div class="col">
                        <b-card-text>
                            <ul>
                                <li
                                        v-for="externalOntologyRef in externalOntologiesRefs"
                                        :key="externalOntologyRef.name"
                                >
                                    <a
                                            target="_blank"
                                            v-bind:title="externalOntologyRef.name"
                                            v-bind:href="externalOntologyRef.link"
                                            v-b-tooltip.v-info.hover.left="externalOntologyRef.description"
                                    >{{ externalOntologyRef.name }}</a>
                                </li>
                            </ul>
                        </b-card-text>
                    </div>

                    <div class="col-lg-7">
                        <p> {{$t("UnitForm.ontologies-help")}}</p>
                    </div>
                </div>
            </div>

        </div>


    </ValidationObserver>
</template>


<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import UnitCreate from "./UnitCreate.vue";
import {ExternalOntologies} from "../../../models/ExternalOntologies";
// @ts-ignore
import { UnitCreationDTO } from "opensilex-core/index";

@Component
export default class UnitForm extends Vue {
    $opensilex: any;

    title = "";
    uriGenerated = true;

    @Prop()
    editMode;

    errorMsg: String = "";

    @PropSync("form")
    unitDto: UnitCreationDTO;

    externalOntologiesRefs: any[] = ExternalOntologies.getExternalOntologiesReferences(UnitCreate.selectedOntologies);

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
    a {color: #007bff;}
</style>