import VariableTimeIntervalSelector from '../components/variables/form/VariableTimeIntervalSelector.vue';

export default {
  title: 'Components/VariableTimeIntervalSelector',
  component: VariableTimeIntervalSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableTimeIntervalSelector },
  setup() { return { args }; },
  template: '<VariableTimeIntervalSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
