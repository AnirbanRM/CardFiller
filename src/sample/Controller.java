package sample;


//Storage -> Images/JSON organization/templateN/front.png back.png template.png
// upload users -> User and templateN

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;


import javax.imageio.ImageIO;
import java.io.*;
import java.net.URL;
import java.util.*;


public class Controller implements Initializable {

    public Stage curr_stg;
    File csv_file_path;
    HashMap<String,Integer> placeHolders;
    Face front,back, activeFace;

    String currentDragging;

    @FXML
    TextField img_path_box_f,img_path_box_b,csv_file_box,output_dir_box;
    @FXML
    Canvas canvas;
    @FXML
    ScrollPane scrollpane;
    @FXML
    Button export_button,img_path_browse_f,img_path_browse_b,csv_file_browse,output_browse,upload_button;
    @FXML
    ListView placeholder_list;
    @FXML
    AnchorPane mainpane;
    @FXML
    ComboBox<String> font_box;
    @FXML
    CheckBox bold,italics,underline;
    @FXML
    TextField font_size_box;
    @FXML
    ColorPicker font_colour_box;
    @FXML
    RadioButton front_radio,back_radio;

    private void populateFontBox(){
        fontEnable(false);
        font_box.setCellFactory(c -> {
            ListCell<String> cell = new ListCell<String>(){
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setText(item);
                        setFont(new Font(item, 13));
                    }
                    else {
                        setText(null);
                    }
                }
            };
            return cell;
        });


        font_box.getItems().addAll(Font.getFontNames());
        font_box.getSelectionModel().select((new Font(13)).getName());

        CheckBox[] cbxs = {bold,italics,underline};
        for(CheckBox c : cbxs)
            c.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
                    activeFace.activeElement.isBold=bold.isSelected();
                    activeFace.activeElement.isItalic=italics.isSelected();
                    activeFace.activeElement.isUnderline=underline.isSelected();
                    try{redraw();}catch (Exception e){}
                }
            });

        font_box.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                activeFace.activeElement.font = new Font(font_box.getSelectionModel().getSelectedItem(),15);
                try{redraw();}catch (Exception e){}
            }
        });
    }

    private double distance(double X1, double Y1, double X2, double Y2){
        return Math.sqrt(Math.pow((X1-X2),2) + Math.pow((Y1-Y2),2));
    }

    void fontEnable(boolean bool){
        if(activeFace.activeElement==null)
            bool = false;

        font_box.setDisable(!bool);
        bold.setDisable(!bool);
        italics.setDisable(!bool);
        underline.setDisable(!bool);
        font_size_box.setDisable(!bool);
        font_colour_box.setDisable(!bool);

        if(activeFace.activeElement!=null){
            font_size_box.setText(String.valueOf(activeFace.activeElement.size));
            font_box.getSelectionModel().select(activeFace.activeElement.font.getName());
            bold.setSelected(activeFace.activeElement.isBold);
            italics.setSelected(activeFace.activeElement.isItalic);
            underline.setSelected(activeFace.activeElement.isUnderline);
            font_colour_box.setValue(activeFace.activeElement.color);
        }
    }

    public void makeDraggable(){

        canvas.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                for(Element e : activeFace.droppedEntities){
                    if(distance(e.X,e.Y,mouseEvent.getX(),mouseEvent.getY())<=5) {
                        activeFace.activeElement = e;
                        fontEnable(true);
                        try{redraw();}catch (Exception i){}
                        return;
                    }
                }
                activeFace.activeElement = null;
                fontEnable(false);
                try{redraw();}catch (Exception i){}
            }
        });

        canvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if(activeFace.activeElement!=null){
                    activeFace.activeElement.X = mouseEvent.getX();
                    activeFace.activeElement.Y = mouseEvent.getY();
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

        canvas.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                String t = dragEvent.getDragboard().getString();
                if(t==null)
                    return;

                activeFace.droppedEntities.add(new Element(t,dragEvent.getX(),dragEvent.getY()));

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

    private void load_image(File f,Face face){
        if(f==null)
            return;
        ((face==front)?img_path_box_f:img_path_box_b).setText(f.getPath());
        face.card_img_prepared = null;

        try {
            face.card_img = ImageIO.read(f);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }

        canvas.setWidth(face.card_img.getWidth());
        canvas.setHeight(face.card_img.getHeight());
        try {
            redraw();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void redraw() throws Exception{
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());

        if(activeFace.card_img==null)
            return;

        canvas.setWidth(activeFace.card_img.getWidth());
        canvas.setHeight(activeFace.card_img.getHeight());

        if(activeFace.card_img_prepared==null){
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(activeFace.card_img, "png", bos);
            activeFace.card_img_prepared = SwingFXUtils.toFXImage(ImageIO.read(new ByteArrayInputStream(bos.toByteArray())),null);
        }

        g.drawImage(activeFace.card_img_prepared,0,0);

        for(Element e : activeFace.droppedEntities){
            if(e.equals(activeFace.activeElement)){
                g.setFill(Color.valueOf("#FF0000"));
                g.setStroke(Color.valueOf("#FF0000"));
                g.strokeRect(e.X-10,e.Y-10,20,20);
            }

            g.setFill(e.color);
            g.setStroke(e.color);

            g.fillOval(e.X-5,e.Y-5,10,10);
            g.strokeOval(e.X-8,e.Y-8,16,16);

            Font f = Font.font(e.font.getName(), e.isBold?FontWeight.BOLD:FontWeight.NORMAL, e.isItalic? FontPosture.ITALIC:FontPosture.REGULAR, e.size);
            g.setFont(f);
            g.fillText(e.entity,e.X+20,e.Y+20);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        front = new Face();
        back = new Face();
        activeFace = front;
        populateFontBox();

        front_radio.setOnMouseClicked(mouseEvent -> {
            activeFace = front;
            fontEnable(true);
            try{
                redraw();
            }catch (Exception e){
                e.printStackTrace();
            }
        });

        back_radio.setOnMouseClicked(mouseEvent -> {
            activeFace = back;
            fontEnable(true);
            try{
                redraw();
            }catch (Exception e){
                e.printStackTrace();
            }
        });

        img_path_browse_f.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter efil = new FileChooser.ExtensionFilter("Images","*.jpg","*.jpeg","*.png");
                fileChooser.getExtensionFilters().add(efil);
                load_image(fileChooser.showOpenDialog(curr_stg),front);
            }
        });

        img_path_browse_b.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter efil = new FileChooser.ExtensionFilter("Images","*.jpg","*.jpeg","*.png");
                fileChooser.getExtensionFilters().add(efil);
                load_image(fileChooser.showOpenDialog(curr_stg),back);
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

        font_size_box.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                try{
                    Integer.parseInt(font_size_box.getText());
                }catch (Exception e){
                    font_size_box.setText(String.valueOf(Element.DEFAULT_FONT_SIZE));
                }
                activeFace.activeElement.size = Integer.parseInt(font_size_box.getText());
                try {
                    redraw();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        font_colour_box.valueProperty().addListener(new ChangeListener<Color>() {
            @Override
            public void changed(ObservableValue<? extends Color> observableValue, Color color, Color t1) {
                activeFace.activeElement.color = font_colour_box.getValue();
                try{
                    redraw();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        export_button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                export();
            }
        });
        front.droppedEntities = new ArrayList<Element>();
        back.droppedEntities = new ArrayList<Element>();

        makeDraggable();

        upload_button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                try {
                    if(resourceBundle.containsKey("NAME"))
                        upload(resourceBundle.getString("ORG"), resourceBundle.getString("NAME"));
                    else
                        upload(resourceBundle.getString("ORG"),null);
                }catch (Exception e){}
            }
        });


        if(resourceBundle.containsKey("FRONT")){
            img_path_box_f.setText(resourceBundle.getString("FRONT"));
            load_image(new File(resourceBundle.getString("FRONT")),front);
        }

        if(resourceBundle.containsKey("BACK")){
            img_path_box_b.setText(resourceBundle.getString("BACK"));
            load_image(new File(resourceBundle.getString("BACK")),back);
        }

        if(resourceBundle.containsKey("TEMPLATE")){
            createfields(new File(resourceBundle.getString("TEMPLATE")));
        }

    }

    private void createfields(File template) {
        StringBuilder stringBuilder = new StringBuilder();

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(template)));
            String line = "";
            while ((line = reader.readLine()) !=null)
                stringBuilder.append(line);
        }catch (Exception e){
            e.printStackTrace();
        }

        HashSet<String> set = new HashSet<String>();
        JSONObject jsonObject = new JSONObject(stringBuilder.toString());

        JSONObject front = jsonObject.getJSONObject("front");
        set.addAll(front.keySet());

        JSONObject back = jsonObject.getJSONObject("back");
        set.addAll(back.keySet());

        int i = 0;
        placeHolders = new HashMap<String, Integer>();
        for(String s : set)
            placeHolders.put(s.strip(),i++);
        placeholder_list.getItems().addAll(placeHolders.keySet());

        for(String k : front.keySet()){
            JSONObject obj = front.getJSONObject(k);
            JSONArray posXY = obj.getJSONArray("position");
            Element e = new Element(k,posXY.getDouble(0),posXY.getDouble(1));
            e.size = obj.getJSONObject("fontStyle").getInt("size");
            JSONArray colourRGBA = obj.getJSONObject("fontStyle").getJSONArray("color");
            e.color = new Color(colourRGBA.getDouble(0),colourRGBA.getDouble(1),colourRGBA.getDouble(2),colourRGBA.getDouble(3));
            e.font = Font.font(obj.getJSONObject("fontStyle").getString("fontFamily"));
            this.front.droppedEntities.add(e);
        }

        for(String k : back.keySet()){
            JSONObject obj = back.getJSONObject(k);
            JSONArray posXY = obj.getJSONArray("position");
            Element e = new Element(k,posXY.getDouble(0),posXY.getDouble(1));
            e.size = obj.getJSONObject("fontStyle").getInt("size");
            JSONArray colourRGBA = obj.getJSONObject("fontStyle").getJSONArray("color");
            e.color = new Color(colourRGBA.getDouble(0),colourRGBA.getDouble(1),colourRGBA.getDouble(2),colourRGBA.getDouble(3));
            e.font = Font.font(obj.getJSONObject("fontStyle").getString("fontFamily"));
            this.back.droppedEntities.add(e);
        }

        try{
            redraw();
        }catch (Exception e){
            e.printStackTrace();
        }
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

            fis.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void export_a_record(String[] a,int num){
        for(Face fc : new Face[]{front,back}) {
            Canvas c = new Canvas(fc.card_img.getWidth(), fc.card_img.getHeight());
            GraphicsContext g = c.getGraphicsContext2D();

            g.drawImage(fc.card_img_prepared, 0, 0);

            for (Element i : fc.droppedEntities) {
                try {
                    Font f = Font.font(i.font.getName(), i.isBold ? FontWeight.BOLD : FontWeight.NORMAL, i.isItalic ? FontPosture.ITALIC : FontPosture.REGULAR, i.size);
                    g.setFont(f);
                    g.setFill(i.color);
                    g.fillText(a[placeHolders.get(i.entity)], i.X + 20, i.Y + 20);
                } catch (Exception e) {
                }
            }

            WritableImage wi = new WritableImage((int) c.getWidth(), (int) c.getHeight());
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);

            File img_file = new File(output_dir_box.getText() + "\\" + num + ((fc == front)?"_front":"_back") + ".png");
            try {
                img_file.createNewFile();
                ImageIO.write(SwingFXUtils.fromFXImage(c.snapshot(sp, wi), null), "png", img_file);
            } catch (Exception e) {
            }
        }
    }

    void upload(String org,String templatename) throws IOException{
        Stage primaryStage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("uploader.fxml"));

        HashMap<String,Object> map = new HashMap<>();
        if(templatename!=null)
            map.put("NAME",templatename);
        map.put("ORG",org);
        map.put("FRONT",img_path_box_f.getText());
        map.put("BACK",img_path_box_b.getText());
        JSONGenerator jsonGenerator = new JSONGenerator(front,back);
        jsonGenerator.saveJSON();
        map.put("TEMPLATE",jsonGenerator.getPath());

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
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.initOwner(curr_stg);
        primaryStage.setTitle("CardFiller");
        primaryStage.setScene(new Scene(root, 674, 527));
        primaryStage.setResizable(false);
        primaryStage.show();
        ((Uploader)loader.getController()).curr_stg = primaryStage;

    }

}
