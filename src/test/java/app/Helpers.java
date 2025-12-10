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
                //.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAttributes("name")))
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
            } else if (type == ComparisonType.ATTR_VALUE) {

                // --- Сборка полного элемента для Control (Ожидалось) ---
                Node controlNode = comparison.getControlDetails().getTarget();
                Node controlElement = controlNode != null ? controlNode.getParentNode() : null;
                String controlElementFull = formatElementAsString(controlElement);

                // --- Сборка полного элемента для Test (Найдено) ---
                Node testNode = comparison.getTestDetails().getTarget();
                Node testElement = testNode != null ? testNode.getParentNode() : null;
                String testElementFull = formatElementAsString(testElement);

                String attrName = comparison.getControlDetails().getTarget().getNodeName();

                differenceDescription = String.format(
                        "Отличие ЗНАЧЕНИЯ АТРИБУТА: Путь: %s\n" +
                                "Атрибут: '%s', Ожидалось: '%s', Найдено: '%s'\n" +
                                "Ожидаемый элемент: <%s>\n" +
                                "Найденный элемент: <%s>",
                        testPath,
                        attrName,
                        comparison.getControlDetails().getValue(),
                        comparison.getTestDetails().getValue(),
                        controlElementFull,
                        testElementFull
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

                String pathControl = comparison.getControlDetails().getXPath();
                String pathTest = comparison.getTestDetails().getXPath();

                // --- 🛑 ИСПРАВЛЕНИЕ: Пытаемся найти наиболее похожий узел для конкретики ---

                Node controlElement = comparison.getControlDetails().getTarget();
                Node testElement = comparison.getTestDetails().getTarget();

                // Если мы видим, что пути похожи, но сопоставление сломалось,
                // это намекает на отличие содержимого/атрибутов, а не полное отсутствие.

                if (controlElement != null && testElement != null) {
                    // Используем DOM, чтобы быстро сравнить атрибуты и понять, в чем разница.
                    // Если их имена тегов совпадают, но один атрибут отличается,
                    // мы можем вывести конкретное сообщение.

                    // ВНИМАНИЕ: Здесь нужно применить сложную логику, которая требует
                    // доступа к DOM-структуре и ручного сравнения атрибутов.

                    // Однако, самый простой и надежный способ — это использовать
                    // более мягкий NodeMatcher, чтобы дать XMLUnit возможность сделать это за нас.

                    // Поскольку мы не можем легко повторить логику XMLUnit по поиску
                    // похожего узла, мы должны использовать тот факт, что в вашем случае
                    // это всегда отличие атрибута!


                    // Если узел сломался, но он существует в обоих путях (что странно для CHILD_LOOKUP),
                    // то мы предполагаем, что отличие в атрибуте или значении.
                    differenceDescription = String.format(
                            "Отличие ЗНАЧЕНИЯ/АТРИБУТА (Сломано сопоставление): Путь: %s\n" +
                                    "Проверьте, не отличаются ли атрибуты, которые ломают сопоставитель (value)",
                            pathControl != null ? pathControl : pathTest
                    );

                } else if (pathControl != null && pathTest == null) {
                    // Узел отсутствует в Test
                    differenceDescription = String.format(
                            "Отличие СТРУКТУРЫ (УЗЕЛ ОТСУТСТВУЕТ в Test): Путь Control: %s",
                            pathControl
                    );
                } else if (pathControl == null && pathTest != null) {
                    // Узел лишний в Test
                    differenceDescription = String.format(
                            "Отличие СТРУКТУРЫ (ЛИШНИЙ УЗЕЛ в Test): Путь Test: %s",
                            pathTest
                    );
                } else {
                    differenceDescription = String.format(
                            "Отличие СТРУКТУРЫ (Не удалось сопоставить): Control: %s, Test: %s",
                            pathControl,
                            pathTest
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

        // Новый вспомогательный метод (добавить в класс Helpers или CustomDifferenceCollector)
        private static String formatElementAsString(Node elementNode) {
            if (elementNode == null || elementNode.getNodeType() != Node.ELEMENT_NODE) {
                return "N/A";
            }

            StringBuilder sb = new StringBuilder(elementNode.getNodeName());

            // Получаем список атрибутов
            org.w3c.dom.NamedNodeMap attributes = elementNode.getAttributes();
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    sb.append(" ").append(attr.getNodeName())
                            .append("=\"").append(attr.getNodeValue()).append("\"");
                }
            }
            return sb.toString();
        }
    }
}


