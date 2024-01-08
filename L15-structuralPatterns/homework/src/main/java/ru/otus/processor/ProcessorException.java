package ru.otus.processor;

import ru.otus.model.Message;

/**
 * Процессор, который выбрасывает исключение в четную секунду
 */
public class ProcessorException implements Processor {

    private final DateTimeProvider dateTimeProvider;

    public ProcessorException(DateTimeProvider dateTimeProvider) {
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public Message process(Message message) {
        var dateTime = dateTimeProvider.getDate();
        if (dateTime.getSecond() % 2 == 0) {
            throw new RuntimeException("The second given in dateTimeProvider is even");
        }
        return message;
    }
}
