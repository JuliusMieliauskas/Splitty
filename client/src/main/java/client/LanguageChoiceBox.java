package client;

import client.scenes.MainCtrl;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class LanguageChoiceBox extends ComboBox<String> {

    public LanguageChoiceBox(MainCtrl mainCtrl) {
        mainCtrl.registerLanguageChoiceBox(this);
        initLanguageChoiceBox(mainCtrl);
    }

    /**
     * initializes language choice box
     */
    public void initLanguageChoiceBox(MainCtrl mainCtrl) {
        getItems().clear();
        getItems().addAll(LanguageController.getAvailableLanguages());
        setValue(LanguageController.capitalizedLocale(
                LanguageController.getInstance().getCurrentLocale().getLanguage()
        ));
        setButtonCell(createListCell());
        setCellFactory(listView -> createListCell());

        valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String locale = newVal.toLowerCase();
                LanguageController.getInstance().setLocale(new Locale(locale));
                mainCtrl.updateLanguage(LanguageController.getInstance().getCurrentLocale());
            }
            }
        );
    }

    private ListCell<String> createListCell() {
        return new ListCell<String>() {
            private ImageView imageView = new ImageView();

            @Override
            protected void updateItem(String locale, boolean empty) {
                super.updateItem(locale, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Path path = Path.of("src/main/resources/client/flags/" + locale + ".png");
                    path = ((new File(String.valueOf(path))).exists() ? path : Path.of("client/" + path));
                    imageView.setImage(null);
                    if (Files.exists(path)) {
                        imageView.setImage(new Image(path.toUri().toString()));
                        imageView.setFitWidth(30);
                        imageView.setFitHeight(20);
                        setGraphic(imageView);
                    } else {
                        System.out.println("Image not found: " + path.toAbsolutePath());
                    }
                    setText(locale);
                }
            }
        };
    }

    /**
     * sets the new language of this component
     */
    public void updateLanguage() {
        System.out.println("Setting language choice box");
        setValue(LanguageController.capitalizedLocale(
                LanguageController.getInstance().getCurrentLocale().toString()
        ));
    }

    /**
     * updates itslef with latest info from LanguageController
     */
    public void update() {
        getItems().clear();
        getItems().addAll(LanguageController.getAvailableLanguages());
        setValue(LanguageController.capitalizedLocale(
                LanguageController.getInstance().getCurrentLocale().getLanguage()
        ));
    }

}
