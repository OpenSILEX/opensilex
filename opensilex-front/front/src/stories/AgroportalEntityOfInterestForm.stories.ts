import AgroportalEntityOfInterestForm from '../components/variables/agroportal/AgroportalEntityOfInterestForm.vue';

export default {
  title: 'Components/AgroportalEntityOfInterestForm',
  component: AgroportalEntityOfInterestForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalEntityOfInterestForm },
  setup() { return { args }; },
  template: '<AgroportalEntityOfInterestForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
