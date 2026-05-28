import InteroperabilityButton from '../components/common/buttons/InteroperabilityButton.vue';

export default {
  title: 'Components/InteroperabilityButton',
  component: InteroperabilityButton,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { InteroperabilityButton },
  setup() { return { args }; },
  template: '<InteroperabilityButton v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
