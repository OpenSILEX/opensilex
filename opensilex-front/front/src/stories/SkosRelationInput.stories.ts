import SkosRelationInput from '../components/common/external-references/skos/SkosRelationInput.vue';

export default {
  title: 'Components/SkosRelationInput',
  component: SkosRelationInput,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SkosRelationInput },
  setup() { return { args }; },
  template: '<SkosRelationInput v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
