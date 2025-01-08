<template>
  <b-modal ref="modalRef" :size="modalSize" :static="static" no-close-on-backdrop no-close-on-esc >
    <template v-slot:modal-header="modal">
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
        <b-col cols="2" class="modal-buttons">
          <opensilex-HelpButton
              v-if="currentStepHasTutorial && !editMode"
              label="component.tutorial.name"
              @click="startTutorial"
              class="helpButton"
          ></opensilex-HelpButton>

          <!-- Emulate built in modal header close button action -->
          <button type="button" class="close" @click="modal.close()" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </b-col>
      </b-row>
    </template>
    <b-form ref="formRef" v-if="form">
      <form-wizard
        title
        subtitle
        ref="wizardRef"
        shape="square"
        :class="{'single-wizard' : steps.length == 1}"
        color="#00a38d"
        @on-change="onChange"
      >
        <tab-content v-for="(step, index) in steps" :key="index" v-bind:title="$t(step.title)">
          <component
            :ref="'step' + index"
            v-bind:is="step.component"
            :props="step.props"
            :editMode="editMode"
            :form.sync="form"
            @fill="fillForm"
            @clear="clearForm"
            @agroportalTermSelected="agroportalTermSelected"
            @agroportalTermUnselected="agroportalTermUnselected"
          >
            <template v-for="slot of step.slots" v-slot:[slot]="scope">
              <slot :name="slot" v-bind="scope"></slot>
            </template>
          </component>
        </tab-content>

        <template slot="footer" slot-scope="props">
          <footer class="modal-footer modal-footer-replacement">
            <div class="wizard-footer-left">
              <b-button
                variant="secondary"
                @click="hide"
              >{{$t('component.common.form-wizard.cancel')}}</b-button>
            </div>

            <div class="wizard-footer-right" id="v-step-wizard-buttons">
              <b-button
                  id="btn-finish"
                  class="greenThemeColor"
                  variant="success"
                  v-if="!isBlockingStep && steps[props.activeTabIndex].finish"
                  @click="validate(props)"
              >{{getStepBtnFinishTitle(props)}}</b-button>

              <b-button-group>
                <b-button
                  variant="success"
                  v-if="props.activeTabIndex > 0"
                  @click="props.prevTab()"
                >{{getStepBtnPreviousTitle(props)}}</b-button>
                <b-button
                  v-if="!props.isLastStep"
                  class="greenThemeColor"
                  @click="nextStepHandler(props)"
                >{{getStepBtnNextTitle(props)}}</b-button>
                <b-button
                  v-if="props.isLastStep"
                  @click="validate(props)"
                  class="greenThemeColor"
                >{{getStepBtnDoneTitle(props)}}</b-button>
              </b-button-group>
            </div>
          </footer>
        </template>
      </form-wizard>
    </b-form>
  </b-modal>
</template>

<script lang="ts">
import {Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

export interface WizardFormStep {
  component: string,
  title: string,
  finish?: string,
  next?: string,
  done?: string,
  previous?: string,
  props?: {[key: string]: any},
  slots?: Array<string>
}

@Component
export default class WizardForm extends Vue {
  $opensilex: any;

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("wizardRef") readonly wizardRef!: any;

  form = null;

  editMode = false;

  @Prop({ default: true })
  static;

  @Prop({ default: true })
  isBlockingStep;

  @Prop()
  steps: Array<WizardFormStep>;

  @Prop()
  editTitle;

  @Prop()
  createTitle;

  @Prop()
  icon;

  @Prop({ default: "md" })
  modalSize;

  private currentStepIndex?: number = -1;

  @Prop()
  initForm: Function;

  @Prop()
  createAction: Function;

  @Prop()
  updateAction: Function;

  @Prop()
  convertAction: Function;

  @Prop()
  nextStepAction: Function;


  agroportalTermSelected(){
    this.$emit("agroportalTermSelected")
  }

  agroportalTermUnselected(){
    this.$emit("agroportalTermUnselected")
  }

  /**
   * Add a custom validation function to the form. This function will be called during the final validation, before
   * `update or `create` are called.
   */
  @Prop()
  validateAction: (form: unknown) => boolean;

  getStepBtnFinishTitle(props) {
    return (this.steps[props.activeTabIndex].finish)
        ? this.$t(this.steps[props.activeTabIndex].finish)
        : this.$t('component.common.form-wizard.finish');
  }

  getStepBtnPreviousTitle(props) {
    return (this.steps[props.activeTabIndex].previous)
        ? this.$t(this.steps[props.activeTabIndex].previous)
        : this.$t('component.common.form-wizard.previous');
  }

  getStepBtnNextTitle(props) {
    return (this.steps[props.activeTabIndex].next)
        ? this.$t(this.steps[props.activeTabIndex].next)
        : this.$t('component.common.form-wizard.next');
  }

  getStepBtnDoneTitle(props) {
    return (this.steps[props.activeTabIndex].done)
        ? this.$t(this.steps[props.activeTabIndex].done)
        : this.$t('component.common.form-wizard.done');
  }

  showCreateForm() {
    this.form = this.initForm();
    this.editMode = false;
    this.modalRef.show();
    for (let i = 0; i < this.steps.length; i++) {
      if (this.$refs["step" + i]) {
        let step: any = this.$refs["step" + i][0];
        if (step.reset) {
          step.reset();
        }
      }
    }
    this.wizardRef?.reset();
    this.$nextTick(() => {
      this.currentStepIndex = 0;
    });
  }

  showEditForm(form) {
    this.form = form;
    this.modalRef.show();
    this.editMode = true;
    for (let i = 0; i < this.steps.length; i++) {
      if (this.$refs["step" + i]) {
        let step: any = this.$refs["step" + i][0];
        if(step != undefined){
          if (step.reset) {
            step.reset();
          }
        }
      }
    }
    this.wizardRef?.tabs.forEach(tab => {
      tab.checked = true;
    });
    this.wizardRef?.navigateToTab(0);
    this.$nextTick(() => {
      this.currentStepIndex = 0;
    });
  }

  hide() {
    this.modalRef.hide();
  }

  validateStep(props) {
    let step: any = this.$refs["step" + props.activeTabIndex][0];
    if (step.validate) {
      return step.validate().then(isValid => {
        return isValid;
      });
    } else {
      return Promise.resolve(true);
    }
  }

  nextStepHandler(props) {
    return this.validateStep(props)
      .then(isValid => {
        if (isValid && this.nextStepAction) {
          let currentStepComponent: any = this.$refs[
            "step" + props.activeTabIndex
          ][0];
          let nextStepComponent: any = this.$refs[
            "step" + (props.activeTabIndex + 1)
          ][0];
          let nextStepResult = this.nextStepAction(
            props.activeTabIndex,
            this.form,
            nextStepComponent,
            currentStepComponent
          );
          if (!(nextStepResult instanceof Promise)) {
            nextStepResult = Promise.resolve(nextStepResult);
          }

          return nextStepResult
            .then(shouldContinue => {
              if (isValid && shouldContinue !== false) {
                props.nextTab();
              }

              return isValid;
            })
            .catch(error => {
              return false;
            });
        } else {
          if (isValid) {
            props.nextTab();
          }
          return isValid;
        }
      })
      .catch(error => {
        console.error(error);
        return false;
      });
  }

  validate(props) {
    this.validateStep(props).then(isValid => {
      if (isValid) {

        if (this.validateAction) {
          if (!this.validateAction(this.form)) {
            return false;
          }
        }

        let submitMethod: any = null;
        if (this.createAction) {
          submitMethod = this.createAction;
        }

        let successEvent = "onCreate";
        if (this.editMode) {
          if (this.updateAction) {
            submitMethod = this.updateAction;
          } else {
            submitMethod = null;
          }

          successEvent = "onUpdate";
        }

        let submitResult: any = null;
        if (submitMethod != null) {
          submitResult = submitMethod(this.form);
        }
        if (!(submitResult instanceof Promise)) {
          submitResult = Promise.resolve(submitResult);
        }
        submitResult
          .then(result => {
            if (result !== false) {
              this.$nextTick(() => {
                this.$emit(successEvent, submitResult);
                this.hide();
              });
            }
          })
          .catch(console.error);
      }
    });
  }

  fillForm(dto) {
    if (this.convertAction) {
      this.form = this.convertAction(this.form, dto);
    }
  }

  clearForm() {
      this.form = this.initForm();
  }

  skipStep() {
    this.wizardRef.nextTab();
  }

  get currentStepHasTutorial(): boolean {
    return !!(this.$refs["step" + this.currentStepIndex]
        ?.[0]
        ?.startTutorial);
  }

  startTutorial() {
    this.$refs["step" + this.currentStepIndex][0].startTutorial();
  }

  onChange(previousStep: number, nextStepIndex: number) {
    this.currentStepIndex = nextStepIndex;
  }
}
</script>

<style scoped lang="scss">
::v-deep .modal-body {
  padding-top: 0;
  padding-bottom: 0;
}

::v-deep .modal-footer {
  display: none;
}

::v-deep .wizard-header {
  display: none;
}

::v-deep .wizard-tab-content {
  padding-top: 5px;
}

.modal-footer-replacement {
  position: absolute;
  display: block;
  width: 100%;
  left: 0;
  bottom: 0;
}

::v-deep .vue-form-wizard {
  padding-bottom: 50px;
}

.wizard-tab-container {
  padding-top: 20px;
  padding-bottom: 20px;
}

.icon-title {
  margin-right: 5px;
}

#btn-finish {
  margin-right: 10px;
}

::v-deep .single-wizard .wizard-progress-with-circle,
::v-deep .single-wizard .wizard-nav {
  display: none;
}

.modal-buttons {
  display: flex;
  justify-content: right;
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

.close {
  margin-left: 1rem;
}
</style>;
