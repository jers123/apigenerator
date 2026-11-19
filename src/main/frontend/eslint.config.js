// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const jsdoc = require("eslint-plugin-jsdoc");

module.exports = defineConfig([
  {
    // Documentación obligatoria en el código de producción (equivale a Checkstyle en el back)
    files: ['**/*.ts'],
    ignores: ['**/*.spec.ts'],
    extends: [jsdoc.configs['flat/recommended-typescript-error']],
    languageOptions: {
      parser: tseslint.parser,
    },
    rules: {
      'jsdoc/require-jsdoc': [
        'error',
        {
          publicOnly: false,
          require: { ClassDeclaration: true, MethodDefinition: true, FunctionDeclaration: true },
          contexts: [
            'PropertyDefinition',
            'TSInterfaceDeclaration',
            'TSTypeAliasDeclaration',
            'TSPropertySignature',
          ],
        },
      ],
      'jsdoc/require-description': 'error',
      'jsdoc/require-param-description': 'error',
      'jsdoc/require-returns-description': 'error',
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {},
  },
]);
