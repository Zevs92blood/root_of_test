package pages;

import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Selenide.$x;
import static app.UI._$;

public class LoginPage {

    private final String loginL = "";
    private final String passwordL = "";
    private final String loginBtnL = "";


    /**
     *
     */
    public MainPage performLogin(String login, String password) {
        $x(loginL).shouldBe(Condition.exist).setValue(login);
        $x(passwordL).shouldBe(Condition.exist).setValue(password);
        $x(loginBtnL).shouldBe(Condition.exist).click();
        return _$(MainPage.class);
    }


}
