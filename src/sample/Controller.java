package sample;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

class Element{

    String entity;
    double X,Y;

    Element(String entity, double X, double Y){
        this.entity = entity;
        this.X = X;
        this.Y = Y;
    }
}

public class Controller implements Initializable {

    public Stage curr_stg;
    BufferedImage card_img; Image card_img_prepared;
    File csv_file_path;
    HashMap<String,Integer> placeHolders;
    ArrayList<Element> droppedEntities;

    String currentDragging;

    @FXML
    TextField img_path_box,csv_file_box,output_dir_box;
    @FXML
    Canvas canvas;
    @FXML
    ScrollPane scrollpane;
    @FXML
    Button export_button,img_path_browse,csv_file_browse,output_browse;
    @FXML
    ListView placeholder_list;
    @FXML
    AnchorPane mainpane;

    public void  makeDraggable(){

        canvas.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                if(dragEvent.getDragboard().hasString()){
                    dragEvent.acceptTransferModes(TransferMode.COPY);
                }
            }
        });

        canvas.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                String t = dragEvent.getDragboard().getString();
                if(t==null)
                    return;

                droppedEntities.add(new Element(t,dragEvent.getX(),dragEvent.getY()));

                try {
                    redraw();
                }catch (Exception e){}
            }
        });

        placeholder_list.setCellFactory(listView -> {
            ListCell<String> cell = new ListCell<String>(){
                @Override
                public void updateItem(String item , boolean empty) {
                    super.updateItem(item, empty);
                    setText(item);
                }
            };

            cell.setOnDragDetected(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    currentDragging = cell.getItem();
                    Dragboard db = cell.startDragAndDrop(TransferMode.COPY);

                    ClipboardContent cb = new ClipboardContent();
                    cb.putString(currentDragging);
                    db.setContent(cb);
                    mouseEvent.consume();
                }
            });

            return cell;
        });
    }


    private void populatePlaceholderList(String list){
        String[] s = list.split(",");
        placeHolders = new HashMap<String, Integer>();
        for(int i = 0; i< s.length; i++) {
            s[i] = s[i].strip();
            placeHolders.put(s[i], i);
        }

        placeholder_list.getItems().addAll(placeHolders.keySet());
    }

    private void load_csv(File f){
        if(f==null)
            return;

        csv_file_path = f;
        csv_file_box.setText(f.getPath());

        try {
            FileInputStream fis = new FileInputStream(f);
            BufferedReader bfr = new BufferedReader(new InputStreamReader(fis));
            String t = "";
            while (t.equals("")){
                t = bfr.readLine();
            }
            fis.close();
            populatePlaceholderList(t);
        }catch (Exception e){}
    }

    private void load_image(File f){
        if(f==null)
            return;
        img_path_box.setText(f.getPath());
        card_img_prepared = null;

        try {
            card_img = ImageIO.read(f);
        }catch (Exception e){
            return;
        }

        canvas.setWidth(card_img.getWidth());
        canvas.setHeight(card_img.getHeight());
        try {
            redraw();
        }catch (Exception e){}
    }

    private void redraw() throws Exception{
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());
        canvas.setWidth(card_img.getWidth());
        canvas.setHeight(card_img.getHeight());

        if(card_img_prepared==null){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(card_img, "png", bos);
            card_img_prepared = SwingFXUtils.toFXImage(ImageIO.read(new ByteArrayInputStream(bos.toByteArray())),null);
        }

        g.drawImage(card_img_prepared,0,0);

        for(Element e : droppedEntities){
            g.fillOval(e.X-5,e.Y-5,10,10);
            g.strokeOval(e.X-8,e.Y-8,16,16);
            g.setFont(new Font("Calibri",15));
            g.fillText(e.entity,e.X+15,e.Y+15);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        img_path_browse.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter efil = new FileChooser.ExtensionFilter("Images","*.jpg","*.jpeg","*.png");
                fileChooser.getExtensionFilters().add(efil);
                load_image(fileChooser.showOpenDialog(curr_stg));
            }
        });

        csv_file_browse.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter efil = new FileChooser.ExtensionFilter("CSV","*.csv");
                fileChooser.getExtensionFilters().add(efil);
                load_csv(fileChooser.showOpenDialog(curr_stg));
            }
        });
        droppedEntities = new ArrayList<Element>();

        makeDraggable();
    }
}
