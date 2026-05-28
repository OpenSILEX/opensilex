import AssociatedExperimentsList from '../components/experiments/AssociatedExperimentsList.vue';

export default {
  title: 'Components/AssociatedExperimentsList',
  component: AssociatedExperimentsList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AssociatedExperimentsList },
  setup() { return { args }; },
  template: '<AssociatedExperimentsList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
