<template>
  <div ref="modalRef" @ok.prevent="validate">
    <ValidationObserver ref="validatorRef">
      <b-form>
          
        <!-- Label -->
        <b-form-group  required>
          <slot name="label">
            <opensilex-FormInputMessageHelper label=component.experiment.label helpMessage="component.experiment.label-help" >
            </opensilex-FormInputMessageHelper>
          </slot>
          <ValidationProvider :name="$t('component.experiment.label')" rules="required" v-slot="{ errors }" >
            <b-form-input id="label" v-model="form.label" type="text" required placeholder="ZA17"></b-form-input>
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>

      </b-form>
    </ValidationObserver>
  </div>
</template>