package kr.or.womanup.nambu.hjy.userapitest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostActivity extends AppCompatActivity {
    EditText edtId, edtPass, edtAdrr;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        edtId = findViewById(R.id.edt_id);
        edtPass = findViewById(R.id.edt_pass);
        edtAdrr = findViewById(R.id.edt_addr);
        imageView = findViewById(R.id.imageView);

        Button btnPost = findViewById(R.id.btn_post1);
        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserPostThread thread = new UserPostThread();
                thread.start();

            }
        });

        Button btnPhoto= findViewById(R.id.btn_image);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent,1001);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1001){
            if(resultCode==RESULT_OK){
                try {
                    InputStream stream = getContentResolver().openInputStream(data.getData());
                    Bitmap image = BitmapFactory.decodeStream(stream);
                    imageView.setImageBitmap(image);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class UserPostThread extends Thread{
        @Override
        public void run() {
            super.run();
            String user_id = edtId.getText().toString();
            String pass = edtPass.getText().toString();
            String addr = edtAdrr.getText().toString();

            UUID uuid = UUID.randomUUID();
            String filename = uuid.toString()+".jpg";
            upload(filename);

            OkHttpClient client = new OkHttpClient();

            RequestBody body = new FormBody.Builder()
                    .add("uname",user_id)
                    .add("passwd",pass)
                    .add("addr",addr)
                    .add("filename",filename)
                    .build();

            String url = "http://10.0.2.2:8000/users/";

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            GetCallBack callBack = new GetCallBack();
            Call call = client.newCall(request);
            call.enqueue(callBack); //실행(enqueue)하는데 응답은 callback에 해달라는 의미.

        }

        class GetCallBack implements Callback {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d("Rest", e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String success = jsonObject.getString("result");
                    Intent intent = new Intent();
                    intent.putExtra("success", success);
                    setResult(RESULT_OK, intent);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finish();//현재 액티비티 닫기
                        }
                    });

                } catch (JSONException e) {
                    Log.d("Rest", e.getMessage());
                }
            }
        }
        
        //Azure Blob 스토리지에 이미지 올리기
        public void upload(String filename){
            String connectionString = getString(R.string.connectionString);
            try {
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
                CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
                String containerName = getString(R.string.container_name);
                CloudBlobContainer container = blobClient.getContainerReference(containerName);

                Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                int imageLength = inputStream.available();

                CloudBlockBlob imageBlob = container.getBlockBlobReference(filename);
                imageBlob.upload(inputStream,imageLength);

            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}