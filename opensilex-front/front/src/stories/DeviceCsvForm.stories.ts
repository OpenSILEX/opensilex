import DeviceCsvForm from '../components/devices/csv/DeviceCsvForm.vue';

export default {
  title: 'Components/DeviceCsvForm',
  component: DeviceCsvForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceCsvForm },
  setup() { return { args }; },
  template: '<DeviceCsvForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
