/**
 * Camada de apresentacao (REST). Expoe os endpoints da API e traduz
 * requisicoes/respostas HTTP em chamadas para a camada de service.
 *
 * Regra do projeto: controllers recebem e devolvem apenas DTOs
 * (ver pacote {@code dto}), nunca entidades JPA.
 */
package br.edu.ifac.astileac.backend.controller;
