import VariableHelp from '../components/variables/views/VariableHelp.vue';

export default {
  title: 'Components/VariableHelp',
  component: VariableHelp,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableHelp },
  setup() { return { args }; },
  template: '<VariableHelp v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
