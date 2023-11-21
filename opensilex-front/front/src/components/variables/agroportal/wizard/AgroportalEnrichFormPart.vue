<template>
    <ValidationObserver ref="validatorRef">

        <div class="row">

            <div class="col-lg-6">

                <!-- URI -->
                <opensilex-UriForm
                    :uri.sync="form.uri"
                    label="component.common.uri"
                    :generated.sync="uriGenerated"
                    :required="true"
                    helpMessage="EntityEnrichForm.uri-help"
                    :editMode="editMode"
                ></opensilex-UriForm>

                <!-- Name -->
                <opensilex-InputForm
                    :value.sync="form.name"
                    label="component.common.name"
                    type="text"
                    :required="true"
                    :placeholder="props.namePlaceholder"
                ></opensilex-InputForm>

                <!-- Comment -->
                <opensilex-TextAreaForm
                        :value.sync="form.description"
                        label="component.common.description">
                </opensilex-TextAreaForm>
            </div>
        </div>
    </ValidationObserver>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class AgroportalEnrichFormPart extends Vue {
    $opensilex: any;

    uriGenerated = true;

    @Prop()
    editMode;

    errorMsg: String = "";

    @PropSync("form")
    formDto: any;

    @Prop()
    props;

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

<i18n>
en:
  EntityEnrichForm:
    uri-help: "Uncheck this checkbox if you want to insert a concept from an existing ontology or if want to set a particular URI. Let it checked if you want to create a new entity with an auto-generated URI"
fr:
  EntityEnrichForm:
    uri-help: "Décocher si vous souhaitez ajouter une entité à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une entité avec une URI auto-générée"
</i18n>