import UriListView from '../components/common/views/UriListView.vue';

export default {
  title: 'Components/UriListView',
  component: UriListView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { UriListView },
  setup() { return { args }; },
  template: '<UriListView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
