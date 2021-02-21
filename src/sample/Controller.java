package sample;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
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

    Element activeElement;

    private double distance(double X1, double Y1, double X2, double Y2){
        return Math.sqrt(Math.pow((X1-X2),2) + Math.pow((Y1-Y2),2));
    }

    public void  makeDraggable(){

        canvas.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                for(Element e : droppedEntities){
                    if(distance(e.X,e.Y,mouseEvent.getX(),mouseEvent.getY())<=5) {
                        activeElement = e;
                        break;
                    }
                }
            }
        });

        canvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if(activeElement!=null){
                    activeElement.X = mouseEvent.getX();
                    activeElement.Y = mouseEvent.getY();
                    try {
                        redraw();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });

        canvas.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                if(dragEvent.getDragboard().hasString()){
                    dragEvent.acceptTransferModes(TransferMode.COPY);
                }
            }
        });

        canvas.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                activeElement = null;
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
                }catch (Exception e){e.printStackTrace();}
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
        }catch (Exception e){e.printStackTrace();}
    }

    private void load_image(File f){
        if(f==null)
            return;
        img_path_box.setText(f.getPath());
        card_img_prepared = null;

        try {
            card_img = ImageIO.read(f);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }

        canvas.setWidth(card_img.getWidth());
        canvas.setHeight(card_img.getHeight());
        try {
            redraw();
        }catch (Exception e){
            e.printStackTrace();
        }
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

        output_browse.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                DirectoryChooser dirChooser = new DirectoryChooser();
                File f = dirChooser.showDialog(curr_stg);

                if(f==null)
                    return;

                output_dir_box.setText(f.getPath());
            }
        });

        export_button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                export();
            }
        });
        droppedEntities = new ArrayList<Element>();

        makeDraggable();
    }

    private void export(){
        try {
            FileInputStream fis = new FileInputStream(csv_file_path);
            BufferedReader bfr = new BufferedReader(new InputStreamReader(fis));

            boolean head = true;

            int i = 1;

            while(true){
                String s = bfr.readLine();

                if(s==null)
                    break;

                else if(s.length()!=0 && head)
                    head = false;

                else{
                    String[] d = s.split(",");
                    export_a_record(d,i++);
                }
            }

            //fis.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void export_a_record(String[] a,int num){

        Canvas c = new Canvas(card_img.getWidth(),card_img.getHeight());
        GraphicsContext g = c.getGraphicsContext2D();

        g.drawImage(card_img_prepared,0,0);

        for(Element i : droppedEntities){
            try {
                g.fillText(a[placeHolders.get(i.entity)], i.X, i.Y);
            }catch (Exception e){}
        }

        WritableImage wi = new WritableImage((int)c.getWidth(),(int)c.getHeight());
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);

        File img_file = new File(output_dir_box.getText()+"\\"+num+".png");
        try {
            img_file.createNewFile();
            ImageIO.write(SwingFXUtils.fromFXImage(c.snapshot(sp,wi), null), "png", img_file);
        }catch (Exception e){}
    }

}
