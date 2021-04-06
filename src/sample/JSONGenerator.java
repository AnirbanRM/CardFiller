package sample;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Random;

public class JSONGenerator {
    Face front,back;
    File f;
    JSONGenerator(Face front,Face back){
        this.front = front;
        this.back = back;
        f = null;
    }

    void saveJSON(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\"front\":{");

        for(int i = 0; i< front.droppedEntities.size(); i++) {
            Element e = front.droppedEntities.get(i);
            stringBuilder.append("\"");
            stringBuilder.append(e.entity);
            stringBuilder.append("\": { \"fontStyle\": { \"size\":");
            stringBuilder.append(e.size);
            stringBuilder.append(", \"fontFamily\": \"");
            stringBuilder.append(e.font.getFamily());
            stringBuilder.append("\", \"color\": [");
            stringBuilder.append(e.color.getRed());
            stringBuilder.append(",");
            stringBuilder.append(e.color.getGreen());
            stringBuilder.append(",");
            stringBuilder.append(e.color.getBlue());
            stringBuilder.append(",");
            stringBuilder.append(e.color.getOpacity());
            stringBuilder.append("]},\"position\": [");
            stringBuilder.append(e.X);
            stringBuilder.append(",");
            stringBuilder.append(e.Y);
            stringBuilder.append("]}");

            if (i < front.droppedEntities.size() - 1)
                stringBuilder.append(",");
        }

        stringBuilder.append("},");

        stringBuilder.append("\"back\": {");

        for(int i = 0; i< back.droppedEntities.size(); i++) {
            Element e = back.droppedEntities.get(i);
            stringBuilder.append("\"");
            stringBuilder.append(e.entity);
            stringBuilder.append("\": { \"fontStyle\": { \"size\":");
            stringBuilder.append(e.size);
            stringBuilder.append(", \"fontFamily\": \"");
            stringBuilder.append(e.font.getFamily());
            stringBuilder.append("\", \"color\": [");
            stringBuilder.append(e.color.getRed());
            stringBuilder.append(",");
            stringBuilder.append(e.color.getGreen());
            stringBuilder.append(",");
            stringBuilder.append(e.color.getBlue());
            stringBuilder.append(",");
            stringBuilder.append(e.color.getOpacity());
            stringBuilder.append("]},\"position\": [");
            stringBuilder.append(e.X);
            stringBuilder.append(",");
            stringBuilder.append(e.Y);
            stringBuilder.append("]}");

            if (i < back.droppedEntities.size() - 1)
                stringBuilder.append(",");
        }
        stringBuilder.append("}}");

        try{
            f = File.createTempFile("JSON",".json");
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            FileWriter fileWriter = new FileWriter(f);
            fileWriter.write(stringBuilder.toString());
            fileWriter.flush();
            fileWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    String getPath(){
        return f.getPath();
    }
}
