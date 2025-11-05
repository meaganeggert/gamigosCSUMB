module.exports = {
  env: {
    es6: true,
    node: true,
  },
  parserOptions: {
    ecmaVersion: 2018,
  },
  extends: [
    'eslint:recommended',
    'google',
  ],
  rules: {
    'require-jsdoc': 'off',
    'max-len': ['warn', { code: 100 }],
    'quote-props': ['error', 'consistent-as-needed'],
    'object-curly-spacing': ['error', 'always'],
    'indent': ['error', 2],
    'linebreak-style': 'off',
  },
  overrides: [
    {
      files: ['**/*.spec.*'],
      env: {
        mocha: true,
      },
      rules: {},
    },
  ],
  globals: {},
};
