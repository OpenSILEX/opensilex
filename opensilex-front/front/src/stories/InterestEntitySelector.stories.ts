import InterestEntitySelector from '../components/variables/form/InterestEntitySelector.vue';

export default {
  title: 'Components/InterestEntitySelector',
  component: InterestEntitySelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { InterestEntitySelector },
  setup() { return { args }; },
  template: '<InterestEntitySelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
