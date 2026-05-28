import LocationForm from '../components/location/form/LocationForm.vue';

export default {
  title: 'Components/LocationForm',
  component: LocationForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { LocationForm },
  setup() { return { args }; },
  template: '<LocationForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
