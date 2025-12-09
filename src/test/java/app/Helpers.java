package app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;

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
}
