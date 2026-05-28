import CharacteristicSelector from '../components/variables/form/CharacteristicSelector.vue';

export default {
  title: 'Components/CharacteristicSelector',
  component: CharacteristicSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { CharacteristicSelector },
  setup() { return { args }; },
  template: '<CharacteristicSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
