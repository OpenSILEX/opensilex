import VariableStructureDetails from '../components/variables/views/VariableStructureDetails.vue';

export default {
  title: 'Components/VariableStructureDetails',
  component: VariableStructureDetails,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { VariableStructureDetails },
  setup() { return { args }; },
  template: '<VariableStructureDetails v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
