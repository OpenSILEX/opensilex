import VariableStructureList from '../components/variables/views/VariableStructureList.vue';

export default {
  title: 'Components/VariableStructureList',
  component: VariableStructureList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableStructureList },
  setup() { return { args }; },
  template: '<VariableStructureList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
