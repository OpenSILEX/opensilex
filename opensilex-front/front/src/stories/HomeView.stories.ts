import HomeView from '../components/home/HomeView.vue';

export default {
  title: 'Components/HomeView',
  component: HomeView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { HomeView },
  setup() { return { args }; },
  template: '<HomeView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
