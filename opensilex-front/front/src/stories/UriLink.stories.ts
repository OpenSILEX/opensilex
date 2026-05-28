import UriLink from '../components/common/views/UriLink.vue';

export default {
  title: 'Components/UriLink',
  component: UriLink,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { UriLink },
  setup() { return { args }; },
  template: '<UriLink v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
