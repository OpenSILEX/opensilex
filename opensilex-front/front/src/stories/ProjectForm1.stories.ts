import ProjectForm1 from '../components/projects/ProjectForm1.vue';

export default {
  title: 'Components/ProjectForm1',
  component: ProjectForm1,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProjectForm1 },
  setup() { return { args }; },
  template: '<ProjectForm1 v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
