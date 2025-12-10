package pages;

import com.codeborne.selenide.Condition;

import static app.UI._$;
import static com.codeborne.selenide.Selenide.$x;

public class MainPage {
    private final String menuL = "";
    private final String logoutL = "";
    private final String clientBtnL = "";

    public void performLogout() {
        $x(menuL).shouldBe(Condition.exist).click();
        $x(logoutL).shouldBe(Condition.exist).click();
        $x(menuL).shouldNotBe(Condition.exist);
    }

    public ClientPage goToClientPage(){
        $x(clientBtnL).shouldBe(Condition.exist).click();
        return _$(ClientPage.class);
    }

}
