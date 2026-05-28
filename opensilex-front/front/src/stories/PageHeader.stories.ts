import PageHeader from '../components/layout/PageHeader.vue';

export default {
  title: 'Components/PageHeader',
  component: PageHeader,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { PageHeader },
  setup() { return { args }; },
  template: '<PageHeader v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
