import LocationsForm from '../components/location/form/LocationsForm.vue';

export default {
  title: 'Components/LocationsForm',
  component: LocationsForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { LocationsForm },
  setup() { return { args }; },
  template: '<LocationsForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
