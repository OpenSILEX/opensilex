import PageActions from '../components/layout/PageActions.vue';

export default {
  title: 'Components/PageActions',
  component: PageActions,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { PageActions },
  setup() { return { args }; },
  template: '<PageActions v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
