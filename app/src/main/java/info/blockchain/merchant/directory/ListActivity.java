package info.blockchain.merchant.directory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

import info.blockchain.wallet.util.AppUtil;
import piuk.blockchain.android.R;

public class ListActivity extends ActionBarActivity {

	private static final int HEADING_CAFE = 1;
	private static final int HEADING_BAR = 2;
	private static final int HEADING_RESTAURANT = 3;
	private static final int HEADING_SPEND = 4;
	private static final int HEADING_ATM = 5;

	private ArrayList<BTCBusiness> businesses = null;
    private int curSelection = -1;
    private BTCBusinessAdapter adapter = null;
    
    private Handler mHandler = new Handler();
    
    private String strULat = null;
    private String strULon = null;

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.directory);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
		toolbar.setTitle(R.string.merchant_list);
		setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	strULat = extras.getString("ULAT");
        	strULon = extras.getString("ULON");
        }

        businesses = new ArrayList<BTCBusiness>();
        
        ListView listView = (ListView)findViewById(R.id.listview);
        adapter = new BTCBusinessAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
            	
            	final BTCBusiness b = businesses.get(position);

     			AlertDialog.Builder alert = new AlertDialog.Builder(ListActivity.this);
                alert.setTitle(R.string.merchant_info);
                alert.setPositiveButton(R.string.directions,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                     			Intent intent = new Intent(Intent.ACTION_VIEW);
                     			// https://maps.google.com/?saddr=34.052222,-118.243611&daddr=37.322778,-122.031944
                     			intent.setData(Uri.parse("https://maps.google.com/?saddr=" +
                     					strULat + "," + strULon +
                     					"&daddr=" + b.lat + "," + b.lon
                     					));
                     			startActivity(intent);
                            }
                        });
                if(b.tel != null) {
                    alert.setNeutralButton(R.string.call,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                	Intent intent = new Intent(Intent.ACTION_DIAL);
                                	intent.setData(Uri.parse("tel:" +b.tel));
                                	startActivity(intent);
                                }
                            });
                }
                alert.show();

            }
        });

		setAdapterContent();

    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(this).stopLockTimer();

        if(AppUtil.getInstance(ListActivity.this).isTimedOut() && !AppUtil.getInstance(this).isLocked()) {
            Intent i = new Intent(ListActivity.this, info.blockchain.wallet.PinEntryActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        AppUtil.getInstance(this).updateUserInteractionTime();
    }

    @Override
    protected void onPause() {
        AppUtil.getInstance(this).startLockTimer();
        super.onPause();
    }

	public void setAdapterContent() {
		
		businesses.clear();
		
		for(int i = 0; i < MapActivity.btcb.size(); i++) {
			if(Double.parseDouble(MapActivity.btcb.get(i).distance) < 15.0) {
				businesses.add(MapActivity.btcb.get(i));
			}
		}

		adapter.notifyDataSetChanged();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.merchant_activity_actions, menu);
		menu.findItem(R.id.action_merchant_list).setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.action_merchant_map:
				finish();
				return true;
			case R.id.action_merchant_suggest:
				doSuggest();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}

	}

    private class BTCBusinessAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    BTCBusinessAdapter() {
	        inflater = (LayoutInflater)ListActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return businesses.size();
		}

		@Override
		public String getItem(int position) {
	        return "";
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view = null;

	        if (convertView == null) {
	            view = inflater.inflate(R.layout.directory_item, parent, false);
	        } else {
	            view = convertView;
	        }

			BTCBusiness b = businesses.get(position);

            Double distance = Double.parseDouble(b.distance);
            String strDistance = null;
            if(distance < 1.0) {
            	distance *= 1000;
            	DecimalFormat df = new DecimalFormat("###");
            	strDistance = df.format(distance) + " meters";
            }
            else {
            	DecimalFormat df = new DecimalFormat("#####.#");
            	strDistance = df.format(distance) + "km";
            }

	        ((TextView)view.findViewById(R.id.txt1)).setText(b.name + " (" + strDistance + ")");
	        ((TextView)view.findViewById(R.id.txt2)).setText(b.address + ", " + b.city + " " + b.pcode);
	        ((TextView)view.findViewById(R.id.txt3)).setText(b.tel);
	        ((TextView)view.findViewById(R.id.txt4)).setText(b.desc);
	        
	        ImageView ivHeading = (ImageView)view.findViewById(R.id.heading);
			switch(Integer.parseInt(b.hc)) {
			case HEADING_CAFE:
		        ivHeading.setImageResource(b.flag.equals("1") ? R.drawable.marker_cafe_featured : R.drawable.marker_cafe);
				break;
			case HEADING_BAR:
		        ivHeading.setImageResource(b.flag.equals("1") ? R.drawable.marker_drink_featured : R.drawable.marker_drink);
				break;
			case HEADING_RESTAURANT:
		        ivHeading.setImageResource(b.flag.equals("1") ? R.drawable.marker_eat_featured : R.drawable.marker_eat);
				break;
			case HEADING_SPEND:
		        ivHeading.setImageResource(b.flag.equals("1") ? R.drawable.marker_spend_featured : R.drawable.marker_spend);
				break;
			case HEADING_ATM:
		        ivHeading.setImageResource(b.flag.equals("1") ? R.drawable.marker_atm_featured : R.drawable.marker_atm);
				break;
			default:
		        ivHeading.setImageResource(b.flag.equals("1") ? R.drawable.marker_cafe_featured : R.drawable.marker_cafe);
				break;
			}

	        return view;
		}

    }

    private void doMapView() {
    	finish();
    }

    private void doSuggest() {
    	Intent intent = new Intent(ListActivity.this, SuggestMerchant.class);
		startActivity(intent);
    }

    @Override
    public void onUserLeaveHint() {
        AppUtil.getInstance(this).setInBackground(true);
    }
}
