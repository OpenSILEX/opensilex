import PersonContact from '../components/persons/PersonContact.vue';

export default {
  title: 'Components/PersonContact',
  component: PersonContact,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { PersonContact },
  setup() { return { args }; },
  template: '<PersonContact v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
