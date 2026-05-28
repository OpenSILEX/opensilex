import FactorCategoryTypes from '../components/ontology/typesView/FactorCategoryTypes.vue';

export default {
  title: 'Components/FactorCategoryTypes',
  component: FactorCategoryTypes,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FactorCategoryTypes },
  setup() { return { args }; },
  template: '<FactorCategoryTypes v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
