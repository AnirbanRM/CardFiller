package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Authenticator implements Initializable {

    Stage curr_stg;
    @FXML
    TextField org,email,pwd;
    @FXML
    Button signin;
    @FXML
    Label message;

    abstract class AuthenticationResult{
        abstract void onSuccess();
        abstract void onFail();
        abstract void onError(Exception e);
    }

    void authenticate(AuthenticationResult result){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    boolean re = FirebaseHandler.getInstance().userExists(org.getText(),email.getText());
                    if(re)
                        try {
                            re = FirebaseHandler.authenticate(email.getText(), pwd.getText());
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    final boolean resu = re;

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if(resu)
                                result.onSuccess();
                            else
                                result.onFail();
                        }
                    });
                }catch (Exception e){
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            result.onError(e);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        signin.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                message.textFillProperty().setValue(Color.BLACK);
                message.setText("Authenticating... Please Wait!");
                authenticate(new AuthenticationResult() {
                    @Override
                    void onSuccess() {
                        message.textFillProperty().setValue(Color.DARKGREEN);
                        message.setText("Credentials matched");
                        try {
                            launch();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    void onFail() {
                        message.textFillProperty().setValue(Color.RED);
                        message.setText("Invalid Credential");
                    }

                    @Override
                    void onError(Exception e) {
                        message.textFillProperty().setValue(Color.RED);
                        message.setText("Unknown error occured : "+e.toString());
                    }
                });
            }
        });
    }

    private void launch() throws IOException {
        Stage primaryStage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("templatechooser.fxml"));

        HashMap<String,String> resources = new HashMap<String, String>();
        resources.put("ORG",org.getText());
        resources.put("USER",email.getText());
        loader.setResources(new ResourceBundle() {

            @Override
            protected Object handleGetObject(String key) {
                return resources.get(key);
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(resources.keySet());
            }
        });

        Parent root = loader.load();

        primaryStage.setTitle("CardFiller");
        primaryStage.setScene(new Scene(root, 704, 469));
        primaryStage.setResizable(false);
        curr_stg.close();
        primaryStage.show();
        ((TemplateChooser)loader.getController()).curr_stg = primaryStage;
    }
}
