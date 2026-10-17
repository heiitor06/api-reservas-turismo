package dev.carlosheitor.agencia.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

public final class Concorrencia {

	private Concorrencia() {
	}

	/**
	 * Dispara as tarefas ao mesmo tempo: cada uma ganha a sua thread, todas esperam a largada e saem juntas.
	 * Sem a largada, as primeiras terminariam antes das últimas começarem e não haveria disputa nenhuma.
	 * Volta só quando todas terminaram; erro inesperado dentro de uma tarefa é relançado aqui.
	 */
	public static void executarJuntos(int quantidade, IntFunction<Callable<Void>> tarefa) throws Exception {
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<Void>> resultados = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(quantidade)) {
			for (int i = 0; i < quantidade; i++) {
				Callable<Void> trabalho = tarefa.apply(i);
				resultados.add(executor.submit(() -> {
					largada.await();
					return trabalho.call();
				}));
			}
			largada.countDown();
		}
		for (Future<Void> resultado : resultados) {
			resultado.get();
		}
	}

}
