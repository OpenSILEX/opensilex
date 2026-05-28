import FacilitiesTypes from '../components/ontology/typesView/FacilitiesTypes.vue';

export default {
  title: 'Components/FacilitiesTypes',
  component: FacilitiesTypes,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilitiesTypes },
  setup() { return { args }; },
  template: '<FacilitiesTypes v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
