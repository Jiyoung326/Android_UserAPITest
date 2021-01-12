package kr.or.womanup.nambu.hjy.userapitest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    UserAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView=findViewById(R.id.recycler);

        adapter = new UserAdapter(this,R.layout.layout_user);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(manager);

        DividerItemDecoration decoration =
                new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(decoration);

        Button btnGet = findViewById(R.id.btn_get);
        btnGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserGetThread thread = new UserGetThread();
                thread.start();
            }
        });

        Button btnPost = findViewById(R.id.btn_post);
        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,PostActivity.class);
                startActivityForResult(intent,101);
            }
        });

    }

    class UserGetThread extends Thread{
        @Override
        public void run() {
            super.run();
            //HttpURLConnection: 전에 쓰던 방법이다 실제로 잘 안 씀. 이번부턴 OKHttp 라이브러리를 사용함.
            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder();
            //127.0.0.1이면 로컬이어서 에뮬레이터 자신이다.
            //10.0.2.2를 쓰면 에뮬레이터 돌아가고 있는 컴퓨터 서버로 연결됨.
            String url = "http://10.0.2.2:8000/users/";
            builder = builder.url(url);
            Request request = builder.build(); //클라이언트로 보낼 request 만들기 끝

            GetCallBack callBack = new GetCallBack();
            Call call = client.newCall(request);
            call.enqueue(callBack); //요청에 대한 응답이 오면 callback이 실행됨.
        }

        class GetCallBack implements Callback{
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d("Rest", e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                adapter.clear();

                try {
                    JSONObject root = new JSONObject(result);
                    int count = root.getInt("count");
                    JSONArray users = root.getJSONArray("users");
                    for(int i=0;i<users.length();i++){
                        JSONObject item = users.getJSONObject(i);
                        String uid = item.getString("uname");
                        String pass = item.getString("passwd");
                        String addr = item.getString("addr");
                        String filename = item.getString("filename");
                        User user = new User(uid,pass,addr,filename);
                        adapter.addItem(user);
                    }
                    recyclerView.post(new Runnable() {
                        @Override //ui바꾸는 건 메인 쓰레드로 보내기
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String result = data.getStringExtra("success");
        if(result.equals("success")){
            UserGetThread thread = new UserGetThread();
            thread.start();
        }
    }
}