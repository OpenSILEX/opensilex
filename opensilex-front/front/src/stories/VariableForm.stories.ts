import VariableForm from '../components/variables/form/VariableForm.vue';

export default {
  title: 'Components/VariableForm',
  component: VariableForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableForm },
  setup() { return { args }; },
  template: '<VariableForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
