import ScientificObjectTypeSelector from '../components/scientificObjects/ScientificObjectTypeSelector.vue';

export default {
  title: 'Components/ScientificObjectTypeSelector',
  component: ScientificObjectTypeSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ScientificObjectTypeSelector },
  setup() { return { args }; },
  template: '<ScientificObjectTypeSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
