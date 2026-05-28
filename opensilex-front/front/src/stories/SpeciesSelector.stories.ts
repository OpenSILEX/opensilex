import SpeciesSelector from '../components/species/SpeciesSelector.vue';

export default {
  title: 'Components/SpeciesSelector',
  component: SpeciesSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SpeciesSelector },
  setup() { return { args }; },
  template: '<SpeciesSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
