import app.LoginLogout;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static com.codeborne.selenide.Selenide.open;

public class BaseTest {


    private static final String CONFIG_FILE_PATH = "src/test/resources/config.properties";
    private static final String BASE_URL_KEY = "baseUrl";
    private Properties config;

    /**
     * Загружает конфигурацию из config.properties перед запуском всех тестов.
     */
    @BeforeSuite
    public void setupConfig() {
        config = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE_PATH)) {
            config.load(fis);
            System.out.println("Конфигурация успешно загружена.");
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке файла конфигурации: " + CONFIG_FILE_PATH);
            e.printStackTrace();
            // Можно добавить тут проброс исключения, чтобы тесты не запускались
        }

        // Настройка Selenide
        // Selenide автоматически управляет WebDriver, но можно указать некоторые настройки
        // Устанавливаем браузер Chrome
        Configuration.browser = "chrome";

        // Опционально: можно настроить таймауты, размер окна и т.д.
        // Configuration.timeout = 10000; // Таймаут 10 секунд
        // Configuration.browserSize = "1920x1080";
    }

    /**
     * Открывает базовый URL перед выполнением каждого тестового метода.
     */
    @BeforeMethod
    public void openBaseUrl() {
        String baseUrl = config.getProperty(BASE_URL_KEY);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            open(baseUrl);
            System.out.println("Переход по URL: " + baseUrl);
        } else {
            System.err.println("Базовый URL не найден в файле конфигурации!");
        }
    }



    /**
     * Выполняет логин, запускает переданные действия и выполняет разлогин.
     * * @param login Логин пользователя
     *
     * @param password Пароль пользователя
     * @param actions  Блок кода (лямбда), содержащий действия пользователя
     */
    protected void doAsUser(String login, String password, LoginLogout actions) {
        System.out.println("--- Запуск блока 'doAsUser' ---");
        try {
            // 1. **ЛОГИН**
            System.out.println("Попытка логина под пользователем: " + login);
            performLogin(login, password);

            // 2. **ДЕЙСТВИЯ ПОЛЬЗОВАТЕЛЯ**
            System.out.println("Выполнение пользовательских действий...");
            actions.execute(); // Выполняется лямбда-выражение из теста

        } catch (Exception e) {
            System.err.println("Ошибка во время выполнения действий пользователя: " + e.getMessage());
            // Пробрасываем исключение, чтобы TestNG пометил тест как упавший
            throw new RuntimeException("Тест упал во время выполнения блока действий.", e);
        } finally {
            // 3. **РАЗЛОГИН** (Выполняется в любом случае, даже если действия упали)
            System.out.println("Выполнение разлогина.");
            performLogout();
        }
        System.out.println("--- Блок 'doAsUser' завершен ---");
    }

    // --- ЗАГЛУШКИ ДЛЯ ЛОГИНА И РАЗЛОГИНА ---

    /**
     * TODO: Заменить на реальную логику ввода данных и нажатия кнопки входа.
     */
    private void performLogin(String login, String password) {
        // Пример (используйте реальные локаторы вашего приложения):
        // $("#login-field").setValue(login);
        // $("#password-field").setValue(password);
        // $("#login-button").click();

        // Для примера: имитация логина
        System.out.println("Логин успешно выполнен (заглушка).");
        // Предполагаем, что после логина мы на главной странице
        Selenide.open("/");
    }

    /**
     * TODO: Заменить на реальную логику клика по кнопке выхода.
     */
    private void performLogout() {
        // Пример (используйте реальные локаторы вашего приложения):
        // $("#user-menu").click();
        // $("#logout-link").click();

        // Для примера: имитация разлогина
        System.out.println("Разлогин успешно выполнен (заглушка).");
    }
}

