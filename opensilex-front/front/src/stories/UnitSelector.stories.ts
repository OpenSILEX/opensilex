import UnitSelector from '../components/variables/form/UnitSelector.vue';

export default {
  title: 'Components/UnitSelector',
  component: UnitSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { UnitSelector },
  setup() { return { args }; },
  template: '<UnitSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
