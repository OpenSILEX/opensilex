import Histogram from '../components/home/dashboard/Histogram.vue';

export default {
  title: 'Components/Histogram',
  component: Histogram,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { Histogram },
  setup() { return { args }; },
  template: '<Histogram v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
