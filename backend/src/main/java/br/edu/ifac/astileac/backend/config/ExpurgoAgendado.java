package br.edu.ifac.astileac.backend.config;

import br.edu.ifac.astileac.backend.service.DadosPessoaisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Apaga em definitivo os cadastros cuja janela de arrependimento
 * venceu (RF10).
 *
 * Roda uma vez por dia, de madrugada. Nao precisa ser no minuto exato
 * em que o prazo vence: 30 dias e uma garantia de MINIMO de tempo para
 * a pessoa mudar de ideia, entao apagar algumas horas depois cumpre o
 * combinado. O que nao poderia acontecer e apagar antes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpurgoAgendado {

	private final DadosPessoaisService dadosPessoaisService;

	@Scheduled(cron = "0 0 3 * * *")
	public void expurgar() {
		int apagados = dadosPessoaisService.expurgarPrazoVencido();
		if (apagados > 0) {
			log.info("Expurgo diario: {} cadastro(s) apagado(s) apos a janela de arrependimento", apagados);
		}
	}

}
