/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spondbob.bluetooth;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class BluetoothActivity extends Activity {
	private String serverUrl = "http://10.151.36.38/bluetoothAttendance/client/";
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_CLASS = 1;
    private ListView newDevicesListView;

    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private List<String> devicesName = new ArrayList<String>();
    private List<String> devicesAddr = new ArrayList<String>();
    private List<String> devicesSignal = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);
        
        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for newly discovered devices
        newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode){
    		case 0:
    			if (resultCode == RESULT_OK) {
    	        	Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
    	        	doDiscovery();
    	        }else{
    	        	Toast.makeText(this, "Bluetooth still disabled", Toast.LENGTH_SHORT).show();
    	        }
    			break;
    		case 1:
    			if (resultCode == RESULT_OK) {
    				if (data.hasExtra("classId")) {
    	                doSendData(data.getExtras().getString("classId"));
    	            }else{
    	            	Toast.makeText(this, "no selected class", Toast.LENGTH_SHORT).show();
    	            }
    			}
    			break;
    	}
    }
    
    /* create menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch (item.getItemId()) {    	
			case R.id.menu_send:
				menuSend();
				return true;
			case R.id.menu_discover:
				menuDiscover();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    private void menuDiscover(){
    	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	if (mBluetoothAdapter == null) {
    		Toast.makeText(BluetoothActivity.this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
    	}else{
    		if (!mBluetoothAdapter.isEnabled()) {
    			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    		}else{
    			doDiscovery();
    		}
    	}
    }
    
    private void menuSend(){
    	if(mNewDevicesArrayAdapter.getCount() == 0){
    		Toast.makeText(BluetoothActivity.this, "No devices can be sent", Toast.LENGTH_SHORT).show();
    	}else{
    		Intent intent = new Intent(BluetoothActivity.this, ClassSelector.class);
	        startActivityForResult(intent, REQUEST_CLASS);
    	}
    }
    
    private void doDiscovery() {
    	// Indicate scanning in the title
    	setProgressBarIndeterminateVisibility(true);
    	setTitle(R.string.scanning);
    	
    	findViewById(R.id.intro).setVisibility(View.GONE);
    	findViewById(R.id.new_devices).setVisibility(View.VISIBLE);

    	// If we're already discovering, stop it
    	if (mBtAdapter.isDiscovering()) {
    		mBtAdapter.cancelDiscovery();
    	}else {
    		Toast.makeText(BluetoothActivity.this, "scanning ...", Toast.LENGTH_SHORT).show();
    		mNewDevicesArrayAdapter.clear();
        	devicesName.clear();
        	devicesAddr.clear();
        	devicesSignal.clear();
        	
        	// Request discover from BluetoothAdapter
        	mBtAdapter.startDiscovery();
    	}
    }
 
    private void doSendData(String classId){
    	//HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
    	try{
    		HttpClient client = new DefaultHttpClient();
    		JSONObject jsonDatas = new JSONObject();
    		HttpPost post = new HttpPost(serverUrl);
    		for(int i = 0; i < mNewDevicesArrayAdapter.getCount(); i++){
    			JSONObject jsonData = new JSONObject();
    			jsonData.put("name", devicesName.get(i).toString());
    			jsonData.put("addr", devicesAddr.get(i).toString());
    			jsonData.put("classId", classId.toString());
    			jsonDatas.put(Integer.toString(i), jsonData.toString());
    		}
    		Toast.makeText(BluetoothActivity.this, jsonDatas.length(), Toast.LENGTH_SHORT).show();
    		ArrayList<NameValuePair> pair = new ArrayList<NameValuePair>(1);
    		pair.add(new BasicNameValuePair("bluetoothData", jsonDatas.toString()));
    		post.setEntity(new UrlEncodedFormEntity(pair));
    		HttpResponse response = client.execute(post);
    		client.execute(post);
    		
    		if(response == null){
    			Toast.makeText(BluetoothActivity.this, "send fail", Toast.LENGTH_SHORT).show();
    		}else{
	    		String s;
	        	int n = jsonDatas.length();
	        	if(n > 1)
	        		s = Integer.toString(n) + " devices sent";
	        	else
	        		s = Integer.toString(n) + " device sent";
	        	Toast.makeText(BluetoothActivity.this, s, Toast.LENGTH_SHORT).show();
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    		Log.e("log_tag","error: "+e.toString());
    	}
    }
    
    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                devicesName.add(device.getName());
                devicesAddr.add(device.getAddress());
                devicesSignal.add(Integer.toString(rssi));
                mNewDevicesArrayAdapter.add(device.getName() + " (" + rssi + ")" + "\n" + device.getAddress());
                
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                Toast.makeText(BluetoothActivity.this, "found " + mNewDevicesArrayAdapter.getCount() + " device(s)", Toast.LENGTH_SHORT).show();
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                	findViewById(R.id.intro).setVisibility(View.VISIBLE);
                	findViewById(R.id.new_devices).setVisibility(View.GONE);
                }
            }
        }
    };

}