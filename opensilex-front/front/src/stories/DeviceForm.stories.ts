import DeviceForm from '../components/devices/form/DeviceForm.vue';

export default {
  title: 'Components/DeviceForm',
  component: DeviceForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceForm },
  setup() { return { args }; },
  template: '<DeviceForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
