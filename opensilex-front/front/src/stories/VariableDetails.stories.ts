import VariableDetails from '../components/variables/VariableDetails.vue';

export default {
  title: 'Components/VariableDetails',
  component: VariableDetails,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableDetails },
  setup() { return { args }; },
  template: '<VariableDetails v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
