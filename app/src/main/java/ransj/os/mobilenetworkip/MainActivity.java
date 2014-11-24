package ransj.os.mobilenetworkip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ArrayList<IpAddress> mIps = new ArrayList<IpAddress>();
    private ArrayAdapter<IpAddress> mAdapter;
    private ListView m_lvIps;
    private BroadcastReceiver mReceiver;
    private boolean mIsTaskRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_lvIps = (ListView) findViewById(R.id.main_list);
        mAdapter = new ArrayAdapter<IpAddress>(this, android.R.layout.simple_list_item_1, mIps);
        m_lvIps.setAdapter(mAdapter);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkConnection();
            }
        };
        registerReceiver(mReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        checkConnection();
        View open = findViewById(R.id.main_open_data);
        View close = findViewById(R.id.main_close_data);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.main_close_data:
                        setMobileData(MainActivity.this, false);
                        break;
                    case R.id.main_open_data:
                        setMobileData(MainActivity.this, true);
                        break;
                }
            }
        };
        open.setOnClickListener(listener);
        close.setOnClickListener(listener);
        checkConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void checkConnection(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();
        Log.d(TAG, "current network : " + network);
        if (network != null && network.isConnected() && network.getType() == ConnectivityManager.TYPE_MOBILE && !mIsTaskRunning) {
            mIsTaskRunning = true;
            new IpTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        boolean data = getMobileDataState(this, null);
        findViewById(R.id.main_close_data).setEnabled(data);
        findViewById(R.id.main_open_data).setEnabled(!data);
    }

    /**
     * 设置手机的移动数据
     */
    public void setMobileData(Context pContext, boolean pBoolean) {
        if(getMobileDataState(this, null) == pBoolean){
            return;
        }
        Log.d(TAG, "setMobileData : "+pBoolean);
        try {

            ConnectivityManager mConnectivityManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            Class ownerClass = mConnectivityManager.getClass();

            Class[] argsClass = new Class[1];
            argsClass[0] = boolean.class;

            Method method = ownerClass.getMethod("setMobileDataEnabled", argsClass);

            method.invoke(mConnectivityManager, pBoolean);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("移动数据设置错误: " + e.toString());
        }
    }

    /**
     * 返回手机移动数据的状态
     *
     * @param pContext
     * @param arg
     *            默认填null
     * @return true 连接 false 未连接
     */
    public boolean getMobileDataState(Context pContext, Object[] arg) {

        try {

            ConnectivityManager mConnectivityManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            Class ownerClass = mConnectivityManager.getClass();

            Class[] argsClass = null;
            if (arg != null) {
                argsClass = new Class[1];
                argsClass[0] = arg.getClass();
            }

            Method method = ownerClass.getMethod("getMobileDataEnabled", argsClass);

            Boolean isOpen = (Boolean) method.invoke(mConnectivityManager, arg);

            return isOpen;

        } catch (Exception e) {
            // TODO: handle exception

            System.out.println("得到移动数据状态出错");
            return false;
        }

    }

    private class IpTask extends AsyncTask<Void, Void, String>{
        String HEADER = "您的当前IP地址：";
        @Override
        protected String doInBackground(Void... params) {
            String ip = null;
            HttpGet request = new HttpGet("http://myip.kkcha.com/s/5/index.php");
            HttpClient client = new DefaultHttpClient();
            try {
                HttpResponse response = client.execute(request);
                int code = response.getStatusLine().getStatusCode();
                String result = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
//                Log.d(TAG, "CODE : "+code);
//                Log.d(TAG, "Result : "+result);
                if(code == HttpStatus.SC_OK) {
                    //您的当前IP地址：180.166.52.154
                    int start = result.indexOf(HEADER) + HEADER.length();
                    int end = result.indexOf('\t', start);
                    ip = result.substring(start, end);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ip;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            mIsTaskRunning = false;
            Log.d(TAG, "Ip : "+result);
            if(TextUtils.isEmpty(result)){
                Toast.makeText(MainActivity.this, "failed to get ip address", Toast.LENGTH_SHORT).show();
            } else {
                IpAddress ip = new IpAddress(result);
                if(mIps.contains(ip)){
                    int index = mIps.indexOf(ip);
                    ip = mIps.get(index);
                    ip.increase();
                    mIps.remove(index);
                }
                mIps.add(0, ip);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    private class IpAddress{
        String mIp;
        int mNums;

        public IpAddress(String ip){
            mIp = ip;
        }

        public void increase(){
            mNums++;
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof IpAddress){
                return TextUtils.equals(((IpAddress) o).mIp, mIp);
            }
            return false;
        }

        @Override
        public String toString() {
            if (mNums > 0) {
                return mIp + "                    " + mNums;
            }
            return mIp;
        }
    }
}
