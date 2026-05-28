import EventTypes from '../components/ontology/typesView/EventTypes.vue';

export default {
  title: 'Components/EventTypes',
  component: EventTypes,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EventTypes },
  setup() { return { args }; },
  template: '<EventTypes v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
