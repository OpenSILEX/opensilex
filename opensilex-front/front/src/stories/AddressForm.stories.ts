import AddressForm from '../components/common/forms/AddressForm.vue';

export default {
  title: 'Components/AddressForm',
  component: AddressForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AddressForm },
  setup() { return { args }; },
  template: '<AddressForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
