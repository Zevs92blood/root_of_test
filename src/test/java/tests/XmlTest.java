package tests;


import app.Helpers;
import org.testng.annotations.Test;

import java.util.List;

public class XmlTest {

    // Внимание: замените эти пути на реальные пути к вашим файлам в ресурсах
    private static final String CONTROL_FILE_PATH = "control.xml";
    private static final String TEST_FILE_PATH = "test.xml";

    @Test
    public void compareAndFailOnDifference() throws Exception {

        // 1. Получаем список всех найденных отличий
        List<String> differences = Helpers.compareUnorderedXml(
                CONTROL_FILE_PATH,
                TEST_FILE_PATH
        );

        // 2. Проверяем, есть ли отличия
        if (!differences.isEmpty()) {

            // 3. Вывод всех отличий перед падением
            System.err.println("\n=======================================================");
            System.err.println("!!! СБОЙ ТЕСТА: Обнаружены отличия в XML-файлах (Всего: " + differences.size() + ") !!!");
            System.err.println("=======================================================");

            for (String diff : differences) {
                // Вывод каждого отличия для наглядности
                System.err.println("- " + diff);
            }

            System.err.println("=======================================================\n");

            // 4. Тест падает с осмысленным сообщением
            throw new AssertionError("Обнаружены отличия в XML: " + differences.size() + ". См. вывод выше.");
        } else {
            System.out.println("✅ XML-файлы идентичны (игнорируя порядок). Тест пройден.");
        }
    }


}