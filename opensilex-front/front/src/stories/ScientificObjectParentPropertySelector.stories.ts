import ScientificObjectParentPropertySelector from '../components/scientificObjects/ScientificObjectParentPropertySelector.vue';

export default {
  title: 'Components/ScientificObjectParentPropertySelector',
  component: ScientificObjectParentPropertySelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ScientificObjectParentPropertySelector },
  setup() { return { args }; },
  template: '<ScientificObjectParentPropertySelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
