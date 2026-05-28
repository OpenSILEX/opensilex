import VariableDataTypeSelector from '../components/variables/form/VariableDataTypeSelector.vue';

export default {
  title: 'Components/VariableDataTypeSelector',
  component: VariableDataTypeSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableDataTypeSelector },
  setup() { return { args }; },
  template: '<VariableDataTypeSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
