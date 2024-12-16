<template>
  <b-modal
    v-if="display"
    ref="modalRef"
    :class="(modalSize === 'full' ? 'full-screen-modal-form' : '')"
    @ok.prevent="validate"
    @hide="$emit('hide')"
    @shown="disableValidation=false"
    @hidden="disableValidation=true"
    :size="modalSize"
    :static="static"
    no-close-on-backdrop
    no-close-on-esc
    @keydown.native.enter="validate"
  >
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template class="mt-1" v-slot:modal-header>
      <b-row class="mt-1" style="width:100%">
        <b-col cols="10" >
          <i>
            <h4> 
              <slot name="icon">
                <opensilex-Icon :icon="icon" class="icon-title" />
              </slot>
              <span v-if="editMode">{{ $t(editTitle) }}</span>
              <span v-else>{{ $t(createTitle) }}</span>         
            </h4>
          </i>
        </b-col>

          <opensilex-HelpButton
              v-if="tutorial && !editMode"
              label="component.tutorial.name"  
              @click="getFormRef().tutorial()"
              class="helpButton"
          ></opensilex-HelpButton> 

        <!-- Emulate built in modal header close button action -->
        <button type="button" class="close col-1" @click="modalRef.hide()" data-dismiss="modal" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
      </b-row> 
    </template>

    <template v-slot:modal-footer="footer">
      <b-button
          variant="secondary"
          @click="footer.cancel()"
      >
        {{$t('component.common.cancel')}}
      </b-button>
      <b-button
          class="greenThemeColor"
          @click="footer.ok()"
      >
        {{$t('component.common.ok')}}
      </b-button>
    </template>

    <ValidationObserver ref="validatorRef">
        <component
            ref="componentRef"
            v-bind:is="component"
            :editMode="editMode"
            :form.sync="form"
            :data="data"
            :disableValidation="disableValidation"
            @shownSelector="disableValidation=true"
            @hideSelector="disableValidation=false"
        >
            <slot name="customFields" v-bind:form="form" v-bind:editMode="editMode"></slot>
        </component>
    </ValidationObserver>
  </b-modal>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

export type ModalInnerForm<CreationDTOType, UpdateDTOType> = Vue & {
  getEmptyForm: () => CreationDTOType;
  create?: (dto: CreationDTOType) => any;
  update?: (dto: UpdateDTOType) => any;
  reset?: () => void;
  tutorial?: () => void;
  setSelectorsToFirstTimeOpenAndSetLabels?: (objectsWithLabels : Array<any>) => void;
  onShowEditForm?: () => void;
  handleSubmitError?: (error) => void;
}

/**
 * Represents a modal form used to create or update a resource. The prop `component` represents the inner form and can
 * be any type of Vue component as long as it implements a `getEmptyForm` method (see {@link ModalInnerForm}). If neither
 * of the props `createAction` or `updateAction` are given, then the `component` must implement a `create` or `update`
 * method.
 */
@Component
export default class ModalForm<InnerFormType extends ModalInnerForm<CreationDTOType, UpdateDTOType>, CreationDTOType, UpdateDTOType> extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $i18n;

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("validatorRef") readonly validatorRef!: any;
  @Ref("componentRef") readonly componentRef!: any;

  editMode = false;

  form: CreationDTOType | UpdateDTOType | {} = {};

  @Prop({default: true})
  static: boolean

  @Prop({ default: false })
  tutorial :boolean;

  @Prop()
  component: string;

  @Prop()
  editTitle;

  @Prop()
  createTitle;

  @Prop()
  icon;

  @Prop({ default: "md" })
  modalSize;

  @Prop({default: false})
  doNotHideOnError: boolean;

  @Prop()
  /**
   * Arbitrary data to be passed to the inner form component
   */
  data: any;

    /**
     * Only renders the component when either {@link showEditForm} of {@link showCreateForm} is called.
     */
  @Prop({default: false})
  lazy: boolean;

    /**
     * Has the modal been opened at least once ?
     */
  opened = false;

  @Prop({
    type: Function,
    default: () => {}
  })
  initForm: (form: CreationDTOType) => CreationDTOType;

  @Prop()
  createAction: (dto: CreationDTOType) => void;

  @Prop()
  updateAction: (dto: UpdateDTOType) => void;

  @Prop({
    type: [String, Function],
    default: "component.common.element"
  })
  successMessage: string | Function;

  @Prop({
      default: false
  })
  overrideSuccessMessage: boolean;

  disableValidation: boolean = true;

  get display(): boolean {
      return !this.lazy || this.opened;
  }

  validate() {
    if(!this.disableValidation){
      this.validatorRef.validate().then(isValid => {
        if (isValid) {
          let submitMethod: any = this.getFormRef().create;
          if (this.createAction) {
            submitMethod = this.createAction;
          }
          let successEvent = "onCreate";
          if (this.editMode) {
            if (this.updateAction) {
              submitMethod = this.updateAction;
            } else {
              submitMethod = this.getFormRef().update;
            }

            successEvent = "onUpdate";
          }

          let submitResult: any = submitMethod(this.form);
          if (!(submitResult instanceof Promise)) {
            submitResult = Promise.resolve(submitResult);
          }
          submitResult
              .then(result => {
                if (result !== false && result !== undefined) {
                  this.creationOrUpdateMessage();
                }
                this.$nextTick(() => {
                  if (result !== false) {
                    this.$emit(successEvent, result);
                  }
                  if (result !== false || !this.doNotHideOnError) {
                    this.hide();
                  }
                });
              })
              .catch((error) => {
                this.getFormRef().handleSubmitError?.(error)
              });
        }
      });
    }
  }

  creationOrUpdateMessage() {
    let successMessage;
    if (typeof this.successMessage == "function") {
      successMessage = this.successMessage(this.form);
    } else {
      successMessage = this.$i18n.t(this.successMessage);
    }

    if (!this.overrideSuccessMessage){
        if (this.editMode) {
            successMessage =
                successMessage +
                this.$i18n.t("component.common.success.update-success-message");
        } else {
            successMessage =
                successMessage +
                this.$i18n.t("component.common.success.creation-success-message");
        }
    }
    this.$opensilex.showSuccessToast(successMessage);
  }
  getFormRef(): InnerFormType {
    return this.$refs.componentRef as InnerFormType;
  }

  /**
   *
   * @param passedForm , optional passing of some predefined fields for when the user arrives on the create form
   */
  showCreateForm(passedForm?: UpdateDTOType | CreationDTOType) {
    this.opened = true;
    
    if(!this.static) {
      this.modalRef.show();
    } 
    this.editMode = false;
    this.$nextTick(() => {
      //Set passed form or create an empty one
      if(passedForm){
        this.form = passedForm;
      }else{
        this.form = this.getFormRef().getEmptyForm();
      }

      let form = this.initForm(this.form as CreationDTOType);
      if (form) {
        this.form = form;
      }
      this.modalRef.show();
      this.validatorRef.reset();
      if (this.getFormRef().reset) {
        this.getFormRef().reset();
      }
    });
  }

    /**
     *
     * @param initiallySelectedWithLabels any object type that contains the information required to set the inner-form's initially selected items (for update modals)
     *
     * @requires  setSelectorsToFirstTimeOpenAndSetLabels function to be defined in the form ref
     */
  setSelectorsToFirstTimeOpenAndSetLabels(initiallySelectedWithLabels){
    this.getFormRef().setSelectorsToFirstTimeOpenAndSetLabels(initiallySelectedWithLabels);
  }

  showEditForm(form: UpdateDTOType) {
    this.opened = true;

    this.editMode = true;

    this.$nextTick(() => {
      this.form = form;
      this.modalRef.show();
      this.validatorRef.reset();
      if (this.getFormRef().reset) {
        this.getFormRef().reset();
      }
      if(this.getFormRef().onShowEditForm){
        this.getFormRef().onShowEditForm();
      }
    });
  }

  hide() {
    this.modalRef.hide();
  }
}
</script>

<style scoped lang="scss">
::v-deep .full-screen-modal-form > .modal-dialog {
  max-width: 95%;
}

.close {
  margin-top: -12px;
  transition: 0.5s;
  right: 5px;
}
.close:hover {
  color: red;
  transition: 0.5s
}

.helpButton {
  margin-left: 25px;
  color: #00A28C;
  font-size: 1.2em;
  border: none
}
  
.helpButton:hover {
  background-color: #00A28C;
  color: #f1f1f1
}

@media (min-width: 200px) and (max-width: 993px) {
  .helpButton {
    margin-left: 0px;
    margin-right: 15px;
    }
}

</style>;

<i18n>
en:
    Move:
        fieldRequired: Location or position field required
fr:
    Move:
        fieldRequired: Veuillez saisir les informations de localisation ou de position
</i18n>