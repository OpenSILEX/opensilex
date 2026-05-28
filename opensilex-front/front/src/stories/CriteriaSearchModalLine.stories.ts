import CriteriaSearchModalLine from '../components/scientificObjects/CriteriaSearchModalLine.vue';

export default {
  title: 'Components/CriteriaSearchModalLine',
  component: CriteriaSearchModalLine,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { CriteriaSearchModalLine },
  setup() { return { args }; },
  template: '<CriteriaSearchModalLine v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
