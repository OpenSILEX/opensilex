import AgroportalCharacteristicForm from '../components/variables/agroportal/AgroportalCharacteristicForm.vue';

export default {
  title: 'Components/AgroportalCharacteristicForm',
  component: AgroportalCharacteristicForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalCharacteristicForm },
  setup() { return { args }; },
  template: '<AgroportalCharacteristicForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
