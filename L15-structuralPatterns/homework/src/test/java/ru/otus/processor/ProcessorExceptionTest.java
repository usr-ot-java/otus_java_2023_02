package ru.otus.processor;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.otus.model.Message;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProcessorExceptionTest {

    private static final Message TEST_MESSAGE = new Message.Builder(1L).build();

   @Test
   public void throwsExceptionWhenEvenSecond() {
       DateTimeProvider dateTimeProvider = Mockito.mock(DateTimeProvider.class);
       Mockito.when(dateTimeProvider.getDate()).thenReturn(LocalDateTime.of(2022, 12, 31, 0, 0, 0));
       ProcessorException processorException = new ProcessorException(dateTimeProvider);
       assertThrows(RuntimeException.class, () -> processorException.process(TEST_MESSAGE));
   }

    @Test
    public void processWhenOddSecond() {
        DateTimeProvider dateTimeProvider = Mockito.mock(DateTimeProvider.class);
        Mockito.when(dateTimeProvider.getDate()).thenReturn(LocalDateTime.of(2022, 12, 31, 0, 0, 1));
        ProcessorException processorException = new ProcessorException(dateTimeProvider);
        processorException.process(TEST_MESSAGE);
    }

}
