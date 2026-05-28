import ScientificObjectTypes from '../components/ontology/typesView/ScientificObjectTypes.vue';

export default {
  title: 'Components/ScientificObjectTypes',
  component: ScientificObjectTypes,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ScientificObjectTypes },
  setup() { return { args }; },
  template: '<ScientificObjectTypes v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
