package com.fcv.citas.adapter.out.persistence;

import com.fcv.citas.application.port.out.TransactionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class SpringTransactionAdapter implements TransactionPort {
    private final TransactionTemplate transactionTemplate;

    public SpringTransactionAdapter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T required(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    @Override
    public void required(Runnable work) {
        transactionTemplate.executeWithoutResult(status -> work.run());
    }
}
