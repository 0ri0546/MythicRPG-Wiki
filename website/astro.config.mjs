import { defineConfig } from 'astro/config';

const repository = process.env.GITHUB_REPOSITORY?.split('/')[1] ?? '';
const isProjectPages = Boolean(repository && !repository.endsWith('.github.io'));
const base = process.env.GITHUB_ACTIONS === 'true' && isProjectPages ? `/${repository}` : '/';
const owner = process.env.GITHUB_REPOSITORY_OWNER ?? 'localhost';
const site = process.env.GITHUB_ACTIONS === 'true'
  ? `https://${owner}.github.io`
  : 'http://localhost:4321';

export default defineConfig({
  output: 'static',
  site,
  base,
  trailingSlash: 'always',
  build: {
    format: 'directory'
  }
});
