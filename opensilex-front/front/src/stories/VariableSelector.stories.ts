import VariableSelector from '../components/variables/views/VariableSelector.vue';

export default {
  title: 'Components/VariableSelector',
  component: VariableSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableSelector },
  setup() { return { args }; },
  template: '<VariableSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
