package com.fcv.citas.application.port.out;

import java.util.function.Supplier;

public interface TransactionPort {
    <T> T required(Supplier<T> work);
    void required(Runnable work);
}
