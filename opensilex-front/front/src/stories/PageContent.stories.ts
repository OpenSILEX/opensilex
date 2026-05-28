import PageContent from '../components/layout/PageContent.vue';

export default {
  title: 'Components/PageContent',
  component: PageContent,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { PageContent },
  setup() { return { args }; },
  template: '<PageContent v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
