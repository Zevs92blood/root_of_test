package app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Helpers {

    /**
     * Читает XML-файл из тестовых ресурсов, преобразует его в JSON и возвращает результат в виде строки.
     *
     * @param resourcePath Путь к файлу в ресурсах (например, "xml/data.xml").
     * @return JSON-строка, форматированная для удобного чтения.
     * @throws IOException Если файл не найден или произошла ошибка при чтении/преобразовании.
     */
    public static String convertXmlFileToJson(String resourcePath) throws IOException {

        // 1. Находим файл в ресурсах с помощью ClassLoader
        // Это надежный способ, который работает независимо от того, как запущен тест (IDE, Gradle и т.д.)
        ClassLoader classLoader = Helpers.class.getClassLoader();
        URL resourceUrl = classLoader.getResource(resourcePath);

        if (resourceUrl == null) {
            throw new IOException("Файл не найден в ресурсах: " + resourcePath);
        }

        File xmlFile = new File(resourceUrl.getFile());

        // 2. Создаем маппер для XML
        XmlMapper xmlMapper = new XmlMapper();

        // Читаем XML-файл и преобразуем его в универсальный узел JSON (JsonNode)
        JsonNode jsonNode = xmlMapper.readTree(xmlFile);

        // 3. Создаем стандартный маппер для JSON
        ObjectMapper jsonMapper = new ObjectMapper();

        // Преобразуем JsonNode обратно в форматированную JSON-строку
        // writerWithDefaultPrettyPrinter() делает JSON красиво отформатированным
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    }

    public static List<String> compareUnorderedXml(String controlXmlPath, String testXmlPath) throws IOException, URISyntaxException {

        // 1. Читаем оба файла полностью в строки
        String controlXmlContent = getResourceContentAsString(controlXmlPath);
        String testXmlContent = getResourceContentAsString(testXmlPath);

        CustomDifferenceCollector collector = new CustomDifferenceCollector();

        // 2. Настройка DiffBuilder для сравнения строк:
        Diff diff = DiffBuilder
                .compare(controlXmlContent) // <-- Передаем строку
                .withTest(testXmlContent)   // <-- Передаем строку
                // Игнорируем порядок элементов, сопоставляя их по имени и атрибутам
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                // Применяем цепочку DifferenceEvaluator'ов
                .withDifferenceEvaluator(
                        collector // Используем только ваш CustomDifferenceCollector
                )
                .ignoreWhitespace() // Игнорируем пробелы
                .build();

        // 3. Выполняем сравнение для активации коллектора
        diff.getDifferences().forEach(d -> {});

        return collector.getDifferences();
    }

    // Вспомогательный метод для получения xml из ресурсов
        private static String getResourceContentAsString(String resourcePath) throws IOException, URISyntaxException {
        ClassLoader classLoader = Helpers.class.getClassLoader();
        java.net.URL resourceUrl = classLoader.getResource(resourcePath);

        if (resourceUrl == null) {
            throw new IOException("Файл не найден в ресурсах: " + resourcePath);
        }

        // 🛑 ИСПОЛЬЗУЕМ java.nio.file для надежного чтения файла
        // Примечание: Это требует преобразования URL в Path, что иногда может
        // давать сбои в специфических средах (например, внутри JAR-файлов),
        // но должно надежно работать в IDE.
        java.nio.file.Path path = java.nio.file.Paths.get(resourceUrl.toURI());

        // Читаем все байты и преобразуем их в строку с кодировкой UTF-8
        byte[] fileBytes = java.nio.file.Files.readAllBytes(path);
        return new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    // ----------------------------------------------------------------------------------
    // Внутренний класс для детального сбора и форматирования различий
    // ----------------------------------------------------------------------------------

    private static class CustomDifferenceCollector implements DifferenceEvaluator {
        private final List<String> differences = new ArrayList<>();

        public List<String> getDifferences() {
            return differences;
        }

        @Override
        public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {

            if (outcome == ComparisonResult.EQUAL || outcome == ComparisonResult.SIMILAR) {
                return outcome;
            }

            ComparisonType type = comparison.getType();

            // Игнорируем технический "шум" (длина списка узлов, порядок)
            if (type == ComparisonType.NODE_TYPE ||
                    type.name().contains("SCHEMA") ||
                    type == ComparisonType.CHILD_NODELIST_LENGTH ||
                    type == ComparisonType.CHILD_NODELIST_SEQUENCE
            )
            {
                return outcome;
            }

            String controlPath = comparison.getControlDetails().getXPath();
            String testPath = comparison.getTestDetails().getXPath();
            String differenceDescription = "";

            // --- ИСПРАВЛЕННАЯ ЛОГИКА ПРОВЕРКИ АТРИБУТОВ ---
            // Проверяем, относится ли сравнение к атрибуту (по типу узла, который отличается)
            Node controlTarget = comparison.getControlDetails().getTarget();

            if (controlTarget != null && controlTarget.getNodeType() == Node.ATTRIBUTE_NODE) {
                // Отличие атрибута
                String attrName = controlTarget.getNodeName();
                differenceDescription = String.format(
                        "Отличие АТРИБУТА: Путь: %s, Атрибут: '%s', Ожидалось: '%s', Найдено: '%s'",
                        testPath,
                        attrName,
                        comparison.getControlDetails().getValue(),
                        comparison.getTestDetails().getValue()
                );
            } else if (type == ComparisonType.TEXT_VALUE) {
                // Отличие текстового значения элемента
                differenceDescription = String.format(
                        "Отличие ЗНАЧЕНИЯ: Путь: %s, Ожидалось: '%s', Найдено: '%s'",
                        testPath,
                        comparison.getControlDetails().getValue(),
                        comparison.getTestDetails().getValue()
                );
            } else if (type == ComparisonType.CHILD_LOOKUP) {

                // --- ИСПРАВЛЕННАЯ ЛОГИКА ДЛЯ CHILD_LOOKUP ---

                // Проверяем, в какой стороне не удалось найти сопоставление, чтобы получить
                // наилучший доступный путь.
                String availableXPath;

                if (comparison.getControlDetails().getXPath() != null) {
                    // Узел был в Control, но не найден в Test (Узел отсутствует)
                    availableXPath = comparison.getControlDetails().getXPath();
                    differenceDescription = String.format(
                            "Отличие СТРУКТУРЫ (Узел отсутствует): Путь: %s",
                            availableXPath
                    );
                } else if (comparison.getTestDetails().getXPath() != null) {
                    // Узел найден в Test, но не ожидался в Control (Узел лишний)
                    availableXPath = comparison.getTestDetails().getXPath();
                    differenceDescription = String.format(
                            "Отличие СТРУКТУРЫ (Лишний узел): Путь: %s",
                            availableXPath
                    );
                } else {
                    // Крайний случай, когда XPath недоступен
                    differenceDescription = String.format(
                            "Отличие СТРУКТУРЫ (Не удалось найти сопоставление - %s): Control: %s, Test: %s",
                            type.getDescription(),
                            controlPath, // Исходные, менее надежные пути
                            testPath
                    );
                }
            } else {
                // Этот else ловит все остальные отличия (например, CHILD_NODELIST_LENGTH)
                differenceDescription = String.format(
                        "Отличие СТРУКТУРЫ (%s): Путь Control: %s, Путь Test: %s",
                        type.getDescription(),
                        controlPath,
                        testPath
                );
            }

            differences.add(differenceDescription);
            return outcome;
        }
    }
}


