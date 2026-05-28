import AgroportalMethodForm from '../components/variables/agroportal/AgroportalMethodForm.vue';

export default {
  title: 'Components/AgroportalMethodForm',
  component: AgroportalMethodForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalMethodForm },
  setup() { return { args }; },
  template: '<AgroportalMethodForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
