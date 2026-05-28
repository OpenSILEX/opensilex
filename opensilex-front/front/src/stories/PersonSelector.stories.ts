import PersonSelector from '../components/persons/PersonSelector.vue';

export default {
  title: 'Components/PersonSelector',
  component: PersonSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { PersonSelector },
  setup() { return { args }; },
  template: '<PersonSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
