package app;

import com.codeborne.selenide.Selenide;

public class UI {
    public static <T> T _$(Class<T> page){
        return Selenide.page(page);
    }
}
