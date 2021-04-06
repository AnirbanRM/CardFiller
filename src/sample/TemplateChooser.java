package sample;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class TemplateChooser implements Initializable {
    public Stage curr_stg;

    @FXML
    ListView<String> templ_listview;
    @FXML
    Label org_label,status;
    @FXML
    Button mod_templ,new_templ;

    String template="";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        FirebaseHandler fh;
        org_label.setText(resourceBundle.getString("ORG"));
        status.setText("Fetching enterprise templates... Please Wait!");
        try {
             fh = FirebaseHandler.getInstance();
             new Thread(() -> {
                 ArrayList<String> al = fh.getTemplates(resourceBundle.getString("ORG"));
                 Platform.runLater(()->{
                     templ_listview.getItems().addAll(al);
                     status.setText(al.size()+" template(s) found");
                 });
             }).start();
        }catch (Exception e){}

        templ_listview.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> mod_templ.setDisable(false));

        mod_templ.setOnMouseClicked(mouseEvent -> {
                FirebaseHandler firebaseHandler=null;
                template = templ_listview.getSelectionModel().getSelectedItem();
            try {
                firebaseHandler = FirebaseHandler.getInstance();
            }catch (Exception e){
                e.printStackTrace();
                return;
            }

            ArrayList<DFile> filesToDownload = new ArrayList<DFile>();
            filesToDownload.add(new DFile(resourceBundle.getString("ORG") + "/" + template + "/" + "templateFront.png",".png"));
            filesToDownload.add(new DFile(resourceBundle.getString("ORG") + "/" + template + "/" + "templateBack.png",".png"));
            filesToDownload.add(new DFile(resourceBundle.getString("ORG") + "/" + template + "/" + "template.json",".json"));
            downloadfile(firebaseHandler,filesToDownload,0);
        });

        new_templ.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                try {
                    launchApp(null, null);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    void downloadfile(FirebaseHandler firebaseHandler, ArrayList<DFile> files, int i){
        Platform.runLater(()->status.setText("Downloading File "+String.valueOf(i+1)+" of 3..."));
        if(i==files.size()-1){
            firebaseHandler.getFile(files.get(i).path, files.get(i).ext, new FirebaseHandler.DownloadCompleteListener() {

                @Override
                void onDownloadCompleted(File f) {
                    files.get(i).savedLoc = f;
                    Platform.runLater(()->{
                        try {
                            launchApp(files, template);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                void onFailedListener(Exception e) {
                    e.printStackTrace();
                }
            });

        }else {
            firebaseHandler.getFile(files.get(i).path, files.get(i).ext, new FirebaseHandler.DownloadCompleteListener() {

                @Override
                void onDownloadCompleted(File f) {
                    files.get(i).savedLoc = f;
                    downloadfile(firebaseHandler, files, i + 1);
                }

                @Override
                void onFailedListener(Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    void launchApp(ArrayList<DFile> files,String template) throws IOException {
        Stage primaryStage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("home.fxml"));

        HashMap<String,String> map = new HashMap<>();
        if(template!=null)
            map.put("NAME",template);
        map.put("ORG",org_label.getText());
        if(files!=null) {
            map.put("FRONT", files.get(0).savedLoc.getAbsolutePath());
            map.put("BACK", files.get(1).savedLoc.getAbsolutePath());
            map.put("TEMPLATE", files.get(2).savedLoc.getAbsolutePath());
        }
        loader.setResources(new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return map.get(key);
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(map.keySet());
            }
        });

        Parent root = loader.load();
        primaryStage.setTitle("CardFiller");
        primaryStage.setScene(new Scene(root, 1200, 700));
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);
        curr_stg.close();
        primaryStage.show();
        ((Controller)loader.getController()).curr_stg = primaryStage;
    }
}

class DFile{

    public String path;
    public String ext;
    public File savedLoc;

    DFile(String path, String ext){
        this.path = path;
        this.ext = ext;
    }
}