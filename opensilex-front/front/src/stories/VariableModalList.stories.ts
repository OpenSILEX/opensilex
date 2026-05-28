import VariableModalList from '../components/variables/VariableModalList.vue';

export default {
  title: 'Components/VariableModalList',
  component: VariableModalList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableModalList },
  setup() { return { args }; },
  template: '<VariableModalList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
