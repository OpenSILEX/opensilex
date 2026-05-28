import ContactsList from '../components/persons/ContactsList.vue';

export default {
  title: 'Components/ContactsList',
  component: ContactsList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ContactsList },
  setup() { return { args }; },
  template: '<ContactsList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
