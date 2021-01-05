<template>
    <opensilex-ModalForm
        ref="modalForm"
        modalSize="lg"
        :tutorial="false"
        component="opensilex-AnnotationForm"
        createTitle="Annotation.add"
        editTitle="Annotation.edit"
        icon="fa#vials"
        :createAction="create"
        :updateAction="update"
        :successMessage="successMessage"
    ></opensilex-ModalForm>

</template>


<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import ModalForm from "../../common/forms/ModalForm.vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {AnnotationUpdateDTO} from "opensilex-core/model/annotationUpdateDTO";
import {AnnotationCreationDTO} from "opensilex-core/model/annotationCreationDTO";

@Component
export default class AnnotationModalForm extends Vue {

    $opensilex: OpenSilexVuePlugin;
    $store: any;
    service: AnnotationsService;
    $i18n: any;

    @Prop()
    target: string;

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    @Ref("modalForm") readonly modalForm!: ModalForm;

    created() {
        this.service = this.$opensilex.getService("opensilex.AnnotationsService");
    }

    showCreateForm() {
        this.modalForm.showCreateForm();
    }

    showEditForm(form : AnnotationUpdateDTO) {
        this.modalForm.showEditForm(form);
    }

    @Prop()
    editMode;

    create(annotation : AnnotationCreationDTO) {

        if(! annotation.targets){
            annotation.targets = [];
        }
        annotation.targets.push(this.target);

        this.service.createAnnotation(annotation).then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
            this.$emit("onCreate", http.response.result.toString());
        }).catch(this.$opensilex.errorHandler);
    }

    successMessage(annotation : AnnotationCreationDTO){
        return this.$i18n.t("Annotation.name");
    }

    update(annotation: AnnotationUpdateDTO) {
        this.service.updateAnnotation(annotation).then(() => {
            this.$emit("onUpdate", annotation.uri);
        }).catch(this.$opensilex.errorHandler);
    }
}

</script>
