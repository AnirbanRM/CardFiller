package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("authenticator.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("CardFiller");
        primaryStage.setScene(new Scene(root, 693, 332));
        primaryStage.setResizable(false);
        primaryStage.show();
        ((Authenticator)loader.getController()).curr_stg = primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
