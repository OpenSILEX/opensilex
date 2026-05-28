import CriteriaSearchModalCreator from '../components/scientificObjects/CriteriaSearchModalCreator.vue';

export default {
  title: 'Components/CriteriaSearchModalCreator',
  component: CriteriaSearchModalCreator,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { CriteriaSearchModalCreator },
  setup() { return { args }; },
  template: '<CriteriaSearchModalCreator v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
