import ProjectForm2 from '../components/projects/ProjectForm2.vue';

export default {
  title: 'Components/ProjectForm2',
  component: ProjectForm2,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProjectForm2 },
  setup() { return { args }; },
  template: '<ProjectForm2 v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
