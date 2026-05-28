import ProjectList from '../components/projects/ProjectList.vue';

export default {
  title: 'Components/ProjectList',
  component: ProjectList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProjectList },
  setup() { return { args }; },
  template: '<ProjectList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
