import app.LoginLogout;
import com.codeborne.selenide.Configuration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import pages.LoginPage;
import pages.MainPage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static app.UI._$;
import static com.codeborne.selenide.Selenide.open;

public class BaseTest {


    private static final String CONFIG_FILE_PATH = "src/test/resources/config.properties";
    private static final String FILE_STORAGE = "src/test/resources";
    private static final String BASE_URL_KEY = "baseUrl";
    private static final String TIMEOUT_KEY = "timeout";
    private static final String BROWSER_KEY = "browser";
    private static final String BROWSER_SIZE_KEY = "browserSize";
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

        // Настройка Selenide из проперти
        Configuration.browser = config.getProperty(BROWSER_KEY);
        Configuration.timeout = Long.parseLong(config.getProperty(TIMEOUT_KEY));
        Configuration.browserSize = config.getProperty(BROWSER_SIZE_KEY);
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
            _$(LoginPage.class).performLogin(login, password);

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
            _$(MainPage.class).performLogout();
        }
        System.out.println("--- Блок 'doAsUser' завершен ---");
    }
}

