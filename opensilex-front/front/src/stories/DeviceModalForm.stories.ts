import DeviceModalForm from '../components/devices/form/DeviceModalForm.vue';

export default {
  title: 'Components/DeviceModalForm',
  component: DeviceModalForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceModalForm },
  setup() { return { args }; },
  template: '<DeviceModalForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
