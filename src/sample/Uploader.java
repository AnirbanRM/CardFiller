package sample;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Uploader implements Initializable {
    private enum MODE{NEW,OLD};
    public Stage curr_stg;

    @FXML
    ListView<String> lv;
    @FXML
    RadioButton ot,nt;
    @FXML
    TextField t_name;
    @FXML
    Button upload;
    @FXML
    Label tnlabel;

    MODE mode;

    private void show(MODE mode){
        this.mode = mode;
        if(mode==MODE.NEW){
            if(!nt.isSelected())
                nt.setSelected(true);
            lv.setDisable(true);
            t_name.setDisable(false);
            tnlabel.setDisable(false);
        }else{
            if(!ot.isSelected())
                ot.setSelected(true);
            lv.setDisable(false);
            t_name.setDisable(true);
            tnlabel.setDisable(true);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if(resourceBundle.containsKey("NAME"))
            show(MODE.OLD);
        else
            show(MODE.NEW);

        try {
            FirebaseHandler fh = FirebaseHandler.getInstance();
            new Thread(() -> {
                ArrayList<String> al = fh.getTemplates(resourceBundle.getString("ORG"));
                Platform.runLater(()->{
                    lv.getItems().addAll(al);
                });
            }).start();
        }catch (Exception e){}

        ot.setOnMouseClicked(mouseEvent -> show(MODE.OLD));
        nt.setOnMouseClicked(mouseEvent -> show(MODE.NEW));

        upload.setOnMouseClicked(mouseEvent -> {
            try {
                upload.setText("Uploading...");
                uploadD(resourceBundle.getString("ORG"), resourceBundle.getString("FRONT"), resourceBundle.getString("BACK"), resourceBundle.getString("TEMPLATE"));
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    private void uploadD(String org,String front, String back, String jsonf) throws IOException {
        FirebaseHandler fh = FirebaseHandler.getInstance();
        String template="";
        if(mode==MODE.NEW) {
            template = t_name.getText().strip();
            if (template.length() == 0)
                return;
        }else if(mode==MODE.OLD){
            if(lv.getSelectionModel().getSelectedItem()==null)
                return;
            else
                template = lv.getSelectionModel().getSelectedItem();
        }
        fh.uploadTemplate(org, template, front, back, jsonf, new FirebaseHandler.UploadCompleteListener() {
            @Override
            void onUploadCompleted() {
                curr_stg.close();
            }

            @Override
            void onUploadFailed(Exception e) {
                e.printStackTrace();
            }
        });
    }

}
