const { defineConfig } = require('@playwright/test');
module.exports = defineConfig({
 testDir:'./src/test/e2e', fullyParallel:false, workers:1,
 timeout:45000, expect:{timeout:10000}, retries:0,
 reporter: process.env.CI ? [['list'],['html',{open:'never'}]] : 'list',
 use:{baseURL:process.env.E2E_BASE_URL || 'http://127.0.0.1:8081', screenshot:'only-on-failure',trace:'retain-on-failure',locale:'pt-BR',timezoneId:'America/Sao_Paulo'},
 projects:[{name:'320',use:{viewport:{width:320,height:740}}},{name:'1280',use:{viewport:{width:1280,height:900}}}]
});
