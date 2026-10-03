package com.fcv.citas.application.service;

import com.fcv.citas.application.port.out.TransactionPort;

/** Transacción directa para pruebas unitarias de casos de uso. */
final class DirectTransactions implements TransactionPort {
    @Override public <T> T required(java.util.function.Supplier<T> work) { return work.get(); }
    @Override public void required(Runnable work) { work.run(); }
}
