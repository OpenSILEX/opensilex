import VariableCreate from '../components/variables/form/VariableCreate.vue';

export default {
  title: 'Components/VariableCreate',
  component: VariableCreate,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableCreate },
  setup() { return { args }; },
  template: '<VariableCreate v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
