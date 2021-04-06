package sample;

import com.google.api.core.ApiFuture;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import javafx.application.Platform;
import org.json.JSONObject;

import javax.swing.plaf.ViewportUI;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class FirebaseHandler {

    private static FirebaseHandler fbase;
    private Firestore fs;

    private FirebaseHandler() throws IOException {
        InputStream serviceAccount = new FileInputStream("src\\sample\\service_account.json");
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(credentials)
                .setStorageBucket("alacard-283318.appspot.com")
                .build();
        FirebaseApp.initializeApp(options);

        fs = FirestoreClient.getFirestore();
    }

    static FirebaseHandler getInstance() throws IOException{
        if(fbase==null)
            fbase = new FirebaseHandler();
        return fbase;
    }

    boolean userExists(String organization,String user){
        if(organization.strip().equals("")||user.strip().equals(""))
            return false;
        ApiFuture<DocumentSnapshot> s = fs.collection("enterprise").document(organization).collection("admin").document(user).get();
        try {
            return s.get().exists();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean authenticate(String user, String pwd) throws FirebaseAuthException, IOException {
        File f = new File("src/sample/service_account.json");
        BufferedReader r = new BufferedReader(new FileReader(f));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) !=null)
            sb.append(line);
        JSONObject o = new JSONObject(sb.toString());

        URL url = new URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="+o.get("api_key"));
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setDoInput(true);
        http.setConnectTimeout(3000);

        http.setRequestProperty("Accept", "application/json");
        http.setRequestProperty("Content-Type", "application/json");

        String data = "{\"email\": \""+user+"\",\"password\": \""+pwd+"\",\"returnSecureToken\": \"false\" }";

        byte[] out = data.getBytes(StandardCharsets.UTF_8);

        OutputStream stream = http.getOutputStream();
        stream.write(out);

        BufferedReader reader;
        try {
             reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
        }catch (Exception e){
            return false;
        }

        sb = new StringBuilder();
        line = "";
        boolean userexits = false;
        while ((line = reader.readLine()) !=null)
            sb.append(line);
        http.disconnect();
        JSONObject object = new JSONObject(sb.toString());
        if(object.get("localId").toString().length()>0)
            userexits = true;

        return userexits;
    }


    ArrayList<String> getTemplates(String org){
        Bucket bucket = StorageClient.getInstance().bucket();

        String prefix = "EnterpriseTemplate/"+org+"/";
        ArrayList<String> el = new ArrayList<String>();

        Storage s = bucket.getStorage();
        Page<Blob> blobs =
                bucket.getStorage().list(
                        bucket.getName(),
                        Storage.BlobListOption.prefix(prefix),
                        Storage.BlobListOption.currentDirectory());

        for (Blob blob : blobs.iterateAll()) {
            String template = blob.getName().substring(prefix.length()).strip();
            if(template.equals(""))
                continue;
            el.add(template.substring(0,template.length()-1));
        }

        return el;
    }

    void getFile(String fileKey,String extension,DownloadCompleteListener listener){

        new Thread(() -> {
            try {
                Storage storage = StorageClient.getInstance().bucket().getStorage();

                Blob blob = storage.get(BlobId.of(StorageClient.getInstance().bucket().getName(), "EnterpriseTemplate/"+fileKey));
                File f = File.createTempFile("file", extension);
                blob.downloadTo(f.toPath());
                listener.onDownloadCompleted(f);
            }catch (Exception e){
                listener.onFailedListener(e);
            }
        }).start();
    }

    static abstract class DownloadCompleteListener{
        abstract void onDownloadCompleted(File f);
        abstract void onFailedListener(Exception e);
    }

    static abstract class UploadCompleteListener{
        abstract void onUploadCompleted();
        abstract void onUploadFailed(Exception e);
    }

    void uploadTemplate(String org, String templatename, String front, String back, String template, UploadCompleteListener listener){
        new Thread(() -> {

            Storage storage = StorageClient.getInstance().bucket().getStorage();
            String key = "EnterpriseTemplate/"+org+"/"+templatename+"/";

            try {
                BlobId blobId = BlobId.of(StorageClient.getInstance().bucket().getName(), key+"templateFront.png");
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
                storage.create(blobInfo, Files.readAllBytes(Paths.get(front)));
            }catch (Exception e){
                listener.onUploadFailed(e);
            }

            try {
                BlobId blobId = BlobId.of(StorageClient.getInstance().bucket().getName(), key+"templateBack.png");
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
                storage.create(blobInfo, Files.readAllBytes(Paths.get(back)));
            }catch (Exception e){
                listener.onUploadFailed(e);
            }

            try {
                BlobId blobId = BlobId.of(StorageClient.getInstance().bucket().getName(), key+"template.json");
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
                storage.create(blobInfo, Files.readAllBytes(Paths.get(template)));
            }catch (Exception e){
                listener.onUploadFailed(e);
            }

            Platform.runLater(()-> listener.onUploadCompleted());

        }).start();
    }
}
