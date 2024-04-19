package client;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ResourceBundle;

import com.google.inject.Injector;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Builder;
import javafx.util.BuilderFactory;
import javafx.util.Callback;
import javafx.util.Pair;

public class MyFXML {

    private Injector injector;


    public MyFXML(Injector injector) {
        this.injector = injector;
    }


    /**
     * Load an FXML file from path specified in parts.
     * Return a Pair with the controller of type <T> and the root Parent node of the loaded FXML file.
     *
     * @param c The Class object corresponding to the controller type T.
     *          This is used for type safety and is not directly used in the method.
     * @param parts The parts of the path to the FXML file, will be added together in the string
     * @return A Pair containing the controller and the root Parent node of the loaded FXML file.
     * @param <T> The type of the controller.
     * @throws RuntimeException if there is an IOException during the FXML loading process.
     */
    public <T> Pair<T, Parent> load(Class<T> c, String... parts) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("client.UIResources.language", new Locale("english"));
            var loader = new FXMLLoader(getLocation(parts), bundle, null, new MyFactory(), StandardCharsets.UTF_8);
            Parent parent = loader.load();
            T ctrl = loader.getController();
            URL cssLocation = getLocation("client", "styles", "app.css");
            System.out.println(cssLocation);
            parent.getStylesheets().add(cssLocation.toExternalForm());
            return new Pair<>(ctrl, parent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URL getLocation(String... parts) {
        var path = Path.of("", parts).toString();
        return MyFXML.class.getClassLoader().getResource(path);
    }

    final private class MyFactory implements BuilderFactory, Callback<Class<?>, Object> {

        @Override
        @SuppressWarnings("rawtypes")
        public Builder<?> getBuilder(Class<?> type) {
            return new Builder() {
                @Override
                public Object build() {
                    return injector.getInstance(type);
                }
            };
        }

        @Override
        public Object call(Class<?> type) {
            return injector.getInstance(type);
        }
    }
}