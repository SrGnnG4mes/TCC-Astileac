import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

/**
 * Nao ha configuracao de CSRF nem de credenciais aqui, e isso e
 * deliberado.
 *
 * O proxy do servidor de desenvolvimento (proxy.conf.json) faz o
 * navegador ver a API e o front-end na MESMA origem. Duas consequencias:
 *
 *   1. O cookie de sessao viaja sozinho, sem withCredentials -- que so
 *      e necessario em requisicao de origem cruzada.
 *   2. O interceptor de CSRF que o provideHttpClient ja instala passa a
 *      agir. Ele le o cookie XSRF-TOKEN e envia o valor no cabecalho
 *      X-XSRF-TOKEN, exatamente os nomes que o SecurityConfig espera --
 *      mas SOMENTE em URL relativa. Em origem cruzada ele nao anexa
 *      nada, e o resultado seria 403 sem explicacao aparente.
 *
 * Por isso todas as chamadas usam caminho relativo ("/autenticacao/...")
 * e nunca "http://localhost:8080/...".
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withFetch()),
    provideRouter(routes)
  ]
};
