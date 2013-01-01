package com.spondbob.bluetooth;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ClassSelector extends ListActivity {

	private String classId = null;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.titleClassSelector);
        
        List<String> list = new ArrayList<String>();
        ArrayAdapter<String> classAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        setListAdapter(classAdapter);
        
        list.add("Jaringan Nirkabel dan Komputasi Bergerak (A)");
        list.add("Topik Khusus Komputasi Berbasis Jaringan (A)");
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		classId = Integer.toString(position + 1);
		finish();
	}
	
	@Override
	public void finish() {
		Intent data = new Intent();
		if(classId != null)
			data.putExtra("classId", classId);
		setResult(RESULT_OK, data);
		super.finish();
	}
}
