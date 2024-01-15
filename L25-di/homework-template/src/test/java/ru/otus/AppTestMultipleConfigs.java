package ru.otus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.otus.appcontainer.AppComponentsContainerImpl;
import ru.otus.config.AppConfigFirst;
import ru.otus.config.AppConfigSecond;

import static ru.otus.AppTestUtils.verifyAppComponents;

public class AppTestMultipleConfigs {

    @DisplayName("Из контекста тремя способами должен корректно доставаться компонент с проставленными полями")
    @ParameterizedTest(name = "Достаем по: {0}")
    @CsvSource(value = {"GameProcessor, ru.otus.services.GameProcessor",
            "GameProcessorImpl, ru.otus.services.GameProcessor",
            "gameProcessor, ru.otus.services.GameProcessor",

            "IOService, ru.otus.services.IOService",
            "IOServiceStreams, ru.otus.services.IOService",
            "ioService, ru.otus.services.IOService",

            "PlayerService, ru.otus.services.PlayerService",
            "PlayerServiceImpl, ru.otus.services.PlayerService",
            "playerService, ru.otus.services.PlayerService",

            "EquationPreparer, ru.otus.services.EquationPreparer",
            "EquationPreparerImpl, ru.otus.services.EquationPreparer",
            "equationPreparer, ru.otus.services.EquationPreparer"
    })
    public void shouldExtractFromContextCorrectComponentWithNotNullFields(String classNameOrBeanId, Class<?> rootClass) throws Exception {
        var ctx = new AppComponentsContainerImpl(AppConfigFirst.class, AppConfigSecond.class);
        verifyAppComponents(classNameOrBeanId, rootClass, ctx);
    }

}
