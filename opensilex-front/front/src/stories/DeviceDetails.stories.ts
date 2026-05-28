import DeviceDetails from '../components/devices/DeviceDetails.vue';

export default {
  title: 'Components/DeviceDetails',
  component: DeviceDetails,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceDetails },
  setup() { return { args }; },
  template: '<DeviceDetails v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
