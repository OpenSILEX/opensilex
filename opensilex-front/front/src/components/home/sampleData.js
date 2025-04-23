// NaiveUI Demo Datas 

const names = ['Lijuan', 'Renaud', 'Dhruthi', 'Slim', 'Chahinez', 'Valentin', 'Lydia', 'Arnaud', 'Seb', 'Max'];
const roles = ['Développeur', 'Designer', 'Chef de projet', 'Analyste', 'Consultant', 'Manager', 'Scrum Master', 'Stagiaire', 'Délégué au café'];

export const sampleData = Array.from({ length: 50 }, (_, i) => ({
  id: i + 1,
  name: names[Math.floor(Math.random() * names.length)],
  age: Math.floor(Math.random() * 40) + 20, // Âge entre 20 et 60 snif
  role: roles[Math.floor(Math.random() * roles.length)]
}));
