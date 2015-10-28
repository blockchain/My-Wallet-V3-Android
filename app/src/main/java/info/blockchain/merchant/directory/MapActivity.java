package info.blockchain.merchant.directory;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
//import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import info.blockchain.wallet.OnSwipeTouchListener;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ToastCustom;
import info.blockchain.wallet.util.TypefaceUtil;
import info.blockchain.wallet.util.WebUtil;
import piuk.blockchain.android.R;

public class MapActivity extends ActionBarActivity implements LocationListener	{

	private GoogleMap map = null;
	private LocationManager locationManager = null;
	private String provider = null;
    private Location location = null;
    private Location currLocation = null;
	private static final long MIN_TIME = 400;
	private static final float MIN_DISTANCE = 1000;
//	private Marker mSelf = null;
	
	private static float Z00M_LEVEL_DEFAULT = 13.0f;
	private static float Z00M_LEVEL_CLOSE = 18.0f;
	private static float Z00M_LEVEL_FAR = 10.0f;
	private float saveZ00mLevel = Z00M_LEVEL_DEFAULT;
	private boolean changeZoom = false;
	
	private int color_category_selected = 0xffFFFFFF;
    private int color_category_unselected = 0xffF1F1F1;
    
	private int color_cafe_selected = 0xffc12a0c;
	private int color_drink_selected = 0xffb65db1;
	private int color_eat_selected = 0xfffd7308;
	private int color_spend_selected = 0xff5592ae;
	private int color_atm_selected = 0xff4dad5c;

    private ImageView imgCafe = null;
    private LinearLayout layoutCafe = null;
    private LinearLayout dividerCafe = null;
    private ImageView imgDrink = null;
    private LinearLayout layoutDrink = null;
    private LinearLayout dividerDrink = null;
    private ImageView imgEat = null;
    private LinearLayout layoutEat = null;
    private LinearLayout dividerEat = null;
    private ImageView imgSpend = null;
    private LinearLayout layoutSpend = null;
    private LinearLayout dividerSpend = null;
    private ImageView imgATM = null;
    private LinearLayout layoutATM = null;
    private LinearLayout dividerATM = null;
    
    private TextView tvName = null;
    private TextView tvAddress = null;
//    private TextView tvCity = null;
    private TextView tvTel = null;
    private TextView tvWeb = null;
    private TextView tvDesc = null;

    private boolean cafeSelected = true;
    private boolean drinkSelected = true;
    private boolean eatSelected = true;
    private boolean spendSelected = true;
    private boolean atmSelected = true;

	private ProgressDialog progress = null;
	
	private HashMap<String,BTCBusiness> markerValues = null;
    private LatLngBounds bounds = null;
	
	private String strJSONData = null;
	public static ArrayList<BTCBusiness> btcb = null;
	
	private LinearLayout infoLayout = null;

    private static final int radius = 40000;

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
		toolbar.setTitle(R.string.merchant_map);
		setSupportActionBar(toolbar);

    	markerValues = new HashMap<String,BTCBusiness>();
    	btcb = new ArrayList<BTCBusiness>();

		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
		
		infoLayout = ((LinearLayout)findViewById(R.id.info));
		infoLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
		    public void onSwipeBottom() {
            	if(infoLayout.getVisibility() == View.VISIBLE) {
            		infoLayout.setVisibility(View.GONE);
            		map.animateCamera(CameraUpdateFactory.zoomTo(saveZ00mLevel));
            	}
		    }
		});
		infoLayout.setVisibility(View.GONE);
		
        tvName = (TextView)findViewById(R.id.tv_name);
        tvName.setTypeface(TypefaceUtil.getInstance(this).getRobotoLightTypeface());
        tvAddress = (TextView)findViewById(R.id.tv_address);
        tvAddress.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());
//        tvCity = (TextView)findViewById(R.id.tv_city);
//        tvCity.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());
        tvTel = (TextView)findViewById(R.id.tv_tel);
        tvTel.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());
        tvWeb = (TextView)findViewById(R.id.tv_web);
        tvWeb.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());
        tvDesc = (TextView)findViewById(R.id.tv_desc);
        tvDesc.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());

		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
		map.setMyLocationEnabled(true);
        map.setOnMarkerClickListener(new OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
            	
                if(marker == null) {
                	return true;
                }

                if(markerValues == null || markerValues.size() < 1) {
                	return true;
                }
                
                ((LinearLayout)findViewById(R.id.row_call)).setVisibility(View.VISIBLE);
                ((LinearLayout)findViewById(R.id.row_web)).setVisibility(View.VISIBLE);

                LatLng latLng = marker.getPosition();
                                
                BTCBusiness b = markerValues.get(marker.getId());

				String url = "http://maps.google.com/?saddr=" +
	 	     	    	currLocation.getLatitude() + "," + currLocation.getLongitude() +
	 	     	     	"&daddr=" + markerValues.get(marker.getId()).lat + "," + markerValues.get(marker.getId()).lon;
                tvAddress.setText(Html.fromHtml("<a href=\"" + url + "\">" + b.address + ", " + b.city + " " + b.pcode + "</a>"));
                tvAddress.setMovementMethod(LinkMovementMethod.getInstance());

                if(b.tel != null && b.tel.length() > 0)	{
                    tvTel.setText(b.tel);
                    Linkify.addLinks(tvTel, Linkify.PHONE_NUMBERS);
                }
                else	{
                    ((LinearLayout)findViewById(R.id.row_call)).setVisibility(View.GONE);
                }
                
                if(b.web != null && b.web.length() > 0)	{
                    tvWeb.setText(b.web);
                    Linkify.addLinks(tvWeb, Linkify.WEB_URLS);
                }
                else	{
                    ((LinearLayout)findViewById(R.id.row_web)).setVisibility(View.GONE);
                }

                tvDesc.setText(b.desc);

                Double distance = Double.parseDouble(markerValues.get(marker.getId()).distance);
                String strDistance = null;
                if(distance < 1.0) {
                	distance *= 1000;
                	DecimalFormat df = new DecimalFormat("###");
                	strDistance = df.format(distance) + " meters";
                }
                else {
                	DecimalFormat df = new DecimalFormat("#####.#");
                	strDistance = df.format(distance) + " km";
                }

                tvName.setText(b.name);
    			switch(Integer.parseInt(b.hc)) {
				case BTCBusiness.HEADING_CAFE:
					tvName.setTextColor(color_cafe_selected);
					break;
				case BTCBusiness.HEADING_BAR:
					tvName.setTextColor(color_drink_selected);
					break;
				case BTCBusiness.HEADING_RESTAURANT:
					tvName.setTextColor(color_eat_selected);
					break;
				case BTCBusiness.HEADING_SPEND:
					tvName.setTextColor(color_spend_selected);
					break;
				case BTCBusiness.HEADING_ATM:
					tvName.setTextColor(color_atm_selected);
					break;
				default:
					tvName.setTextColor(color_cafe_selected);
					break;
				}

     			infoLayout.setVisibility(View.VISIBLE);
     			
     			saveZ00mLevel = map.getCameraPosition().zoom;
     			if(map.getCameraPosition().zoom < Z00M_LEVEL_CLOSE) {
         			map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), Z00M_LEVEL_CLOSE));
     			}
     			else {
         			map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), map.getCameraPosition().zoom));
     			}

            	return true;
            }
        });

	    imgCafe = ((ImageView)findViewById(R.id.cafe));
	    layoutCafe = ((LinearLayout)findViewById(R.id.layout_cafe));
	    dividerCafe = ((LinearLayout)findViewById(R.id.divider_cafe));
	    imgDrink = ((ImageView)findViewById(R.id.drink));
	    layoutDrink = ((LinearLayout)findViewById(R.id.layout_drink));
	    dividerDrink = ((LinearLayout)findViewById(R.id.divider_drink));
	    imgEat = ((ImageView)findViewById(R.id.eat));
	    layoutEat = ((LinearLayout)findViewById(R.id.layout_eat));
	    dividerEat = ((LinearLayout)findViewById(R.id.divider_eat));
	    imgSpend = ((ImageView)findViewById(R.id.spend));
	    layoutSpend = ((LinearLayout)findViewById(R.id.layout_spend));
	    dividerSpend = ((LinearLayout)findViewById(R.id.divider_spend));
	    imgATM = ((ImageView)findViewById(R.id.atm));
	    layoutATM = ((LinearLayout)findViewById(R.id.layout_atm));
	    dividerATM = ((LinearLayout)findViewById(R.id.divider_atm));
	    imgCafe.setBackgroundColor(color_category_selected);
	    layoutCafe.setBackgroundColor(color_category_selected);
	    dividerCafe.setBackgroundColor(color_cafe_selected);
	    imgDrink.setBackgroundColor(color_category_selected);
	    layoutDrink.setBackgroundColor(color_category_selected);
	    dividerDrink.setBackgroundColor(color_drink_selected);
	    imgEat.setBackgroundColor(color_category_selected);
	    layoutEat.setBackgroundColor(color_category_selected);
	    dividerEat.setBackgroundColor(color_eat_selected);
	    imgSpend.setBackgroundColor(color_category_selected);
	    layoutSpend.setBackgroundColor(color_category_selected);
	    dividerSpend.setBackgroundColor(color_spend_selected);
	    imgATM.setBackgroundColor(color_category_selected);
	    layoutATM.setBackgroundColor(color_category_selected);
	    dividerATM.setBackgroundColor(color_atm_selected);
	    
        layoutCafe.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
     			saveZ00mLevel = map.getCameraPosition().zoom;
     			changeZoom = false;
            	imgCafe.setImageResource(cafeSelected ? R.drawable.marker_cafe_off : R.drawable.marker_cafe);
            	dividerCafe.setBackgroundColor(cafeSelected ? color_category_unselected : color_cafe_selected);
            	cafeSelected = cafeSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutDrink.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
     			saveZ00mLevel = map.getCameraPosition().zoom;
     			changeZoom = false;
            	imgDrink.setImageResource(drinkSelected ? R.drawable.marker_drink_off : R.drawable.marker_drink);
            	dividerDrink.setBackgroundColor(drinkSelected ? color_category_unselected : color_drink_selected);
            	drinkSelected = drinkSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutEat.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
     			saveZ00mLevel = map.getCameraPosition().zoom;
     			changeZoom = false;
            	imgEat.setImageResource(eatSelected ? R.drawable.marker_eat_off : R.drawable.marker_eat);
            	dividerEat.setBackgroundColor(eatSelected ? color_category_unselected : color_eat_selected);
            	eatSelected = eatSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutSpend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
     			saveZ00mLevel = map.getCameraPosition().zoom;
     			changeZoom = false;
            	imgSpend.setImageResource(spendSelected ? R.drawable.marker_spend_off : R.drawable.marker_spend);
            	dividerSpend.setBackgroundColor(spendSelected ? color_category_unselected : color_spend_selected);
            	spendSelected = spendSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutATM.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
     			saveZ00mLevel = map.getCameraPosition().zoom;
     			changeZoom = false;
            	imgATM.setImageResource(atmSelected ? R.drawable.marker_atm_off : R.drawable.marker_atm);
            	dividerATM.setBackgroundColor(atmSelected ? color_category_unselected : color_atm_selected);
            	atmSelected = atmSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

		currLocation = new Location(LocationManager.NETWORK_PROVIDER);
        Location lastKnownByGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastKnownByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(lastKnownByGps == null && lastKnownByNetwork == null) {
    		currLocation.setLatitude(0.0);
    		currLocation.setLongitude(0.0);
        }
        else if(lastKnownByGps != null && lastKnownByNetwork == null) {
        	currLocation  = lastKnownByGps;
        }
        else if(lastKnownByGps == null && lastKnownByNetwork != null) {
        	currLocation  = lastKnownByNetwork;
        }
        else {
        	currLocation = (lastKnownByGps.getAccuracy() <= lastKnownByNetwork.getAccuracy()) ? lastKnownByGps : lastKnownByNetwork;
        }

		map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()), Z00M_LEVEL_DEFAULT));
		drawData(true);
	}

	@Override
	public void onLocationChanged(Location location) {

		LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

		currLocation = location;
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, map.getCameraPosition().zoom);
		map.animateCamera(cameraUpdate);
		locationManager.removeUpdates(this);
		
		setProperZoomLevel(latLng, radius, 1);
		drawData(true);

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { }

	@Override
	public void onProviderEnabled(String provider) { }

	@Override
	public void onProviderDisabled(String provider) { }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(this).stopLockTimer();

        if(AppUtil.getInstance(MapActivity.this).isTimedOut() && !AppUtil.getInstance(this).isLocked()) {
            Intent i = new Intent(MapActivity.this, info.blockchain.wallet.PinEntryActivity.class);
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

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.merchant_activity_actions, menu);
		menu.findItem(R.id.action_merchant_map).setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.action_merchant_list:
				doListView();
				return true;
			case R.id.action_merchant_suggest:
				doSuggest();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 
        if(keyCode == KeyEvent.KEYCODE_BACK) {
        	if(infoLayout.getVisibility() == View.VISIBLE) {
        		infoLayout.setVisibility(View.GONE);
        	}
        	else {
        		finish();
        	}
        }

        return false;
    }

    private void drawData(final boolean fetch) {
    	
		map.clear();

		final Handler handler = new Handler(Looper.getMainLooper());

		new Thread(new Runnable() {
			@Override
			public void run() {
				
				Looper.prepare();

				try {
					if(fetch) {
						final String url = "https://merchant-directory.blockchain.info/cgi-bin/btcd.pl?ULAT=" + currLocation.getLatitude() + "&ULON=" + currLocation.getLongitude() + "&D=" + radius + "&K=1";
  	         			strJSONData = WebUtil.getInstance().getURL(url);
					}

					handler.post(new Runnable() {
						@Override
						public void run() {

							try {
								ArrayList<BTCBusiness> tmp = null; 
								tmp = ParseData.parse(strJSONData);
								if(tmp != null && tmp.size() > 0) {
									btcb = tmp;
								}
								else {
									btcb = null;
								}

								if(btcb != null && btcb.size() > 0) {

									BTCBusiness b = null;

				         			for(int i = 0; i < btcb.size(); i++) {
				         				
				         				b = btcb.get(i);

				            			BitmapDescriptor bmd = null;

                                        int hc = BTCBusiness.HEADING_CAFE;
                                        try {
                                            hc = Integer.parseInt(b.hc);
                                        }
                                        catch(Exception e) {
                                            ;
                                        }

				            			switch(hc) {
				            				case BTCBusiness.HEADING_CAFE:
				            					if(cafeSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				case BTCBusiness.HEADING_BAR:
				            					if(drinkSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_drink_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_drink);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				case BTCBusiness.HEADING_RESTAURANT:
				            					if(eatSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_eat_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_eat);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				case BTCBusiness.HEADING_SPEND:
				            					if(spendSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_spend_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_spend);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				case BTCBusiness.HEADING_ATM:
				            					if(atmSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_atm_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_atm);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				default:
				            					if(cafeSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				}
				            			
				            			if(bmd != null) {
					         				Marker marker = map.addMarker(new MarkerOptions()
					         		        .position(new LatLng(Double.parseDouble(b.lat), Double.parseDouble(b.lon)))
					         		        .icon(bmd));
					         				
					         				markerValues.put(marker.getId(), b);
				            			}

				         			}
									
								}

							} catch (Exception e) {
								e.printStackTrace();
							}

							if(changeZoom) {
								setProperZoomLevel(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()), radius, 1);
							}
							else {
								changeZoom = true;
							}

						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Looper.loop();

			}
		}).start();
	}

	void setProperZoomLevel(LatLng loc, int radius, int nbPoi) {

        if(btcb == null || btcb.size() < 1) {
        	return;
        }

        float currentZoomLevel = 21;
        int currentFoundPoi = 0;
        LatLngBounds bounds = null;
        List<LatLng> found = new ArrayList<LatLng>();
        Location location = new Location("");
        location.setLatitude(loc.latitude);
        location.setLongitude(loc.longitude);
        
        boolean continueZooming = true;
        boolean continueSearchingInsideRadius = true;

        while (continueZooming) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, currentZoomLevel--));
            bounds = map.getProjection().getVisibleRegion().latLngBounds;
            Location swLoc = new Location("");
            swLoc.setLatitude(bounds.southwest.latitude);
            swLoc.setLongitude(bounds.southwest.longitude);
            continueSearchingInsideRadius = (Math.round(location.distanceTo(swLoc) / 100) > radius) ? false : true;
            
            for (BTCBusiness b : btcb) {
            	
            	LatLng pos = new LatLng(Double.parseDouble(b.lat), Double.parseDouble(b.lon));

            	if (bounds.contains(pos)) {
                    if (!found.contains(pos)) {
                        currentFoundPoi++;
                        found.add(pos);
//                		Toast.makeText(MapActivity.this, "Found position", Toast.LENGTH_SHORT).show();
                    }
                }

                if (continueSearchingInsideRadius) {
                    if (currentFoundPoi > nbPoi) {
                    	continueZooming = false;
                        break;
                    }
                }
                else if (currentFoundPoi > 0) {
                	continueZooming = false;
                    break;
                }
                else if (currentZoomLevel < 3) {
                	continueZooming = false;
                    break;
                }

            }
            continueZooming = ((currentZoomLevel > 0) && continueZooming) ? true : false;

        }
    }

    private void doListView() {

		boolean doList = false;
		
		if(btcb != null && btcb.size() > 0) {
	    	for(int i = 0; i < btcb.size(); i++) {
				if(Double.parseDouble(btcb.get(i).distance) < 15.0) {
					doList = true;
					break;
				}
			}
		}

    	if(doList) {
        	Intent intent = new Intent(MapActivity.this, ListActivity.class);
        	intent.putExtra("ULAT", Double.toString(currLocation.getLatitude()));
        	intent.putExtra("ULON", Double.toString(currLocation.getLongitude()));
    		startActivity(intent);
    	}
    	else {
            ToastCustom.makeText(MapActivity.this, getString(R.string.no_merchants_in_range), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
    	}
    }

    private void doSuggest() {
    	Intent intent = new Intent(MapActivity.this, SuggestMerchant.class);
        intent.putExtra("ULAT", currLocation.getLatitude());
        intent.putExtra("ULON", currLocation.getLongitude());
		startActivity(intent);
    }

    @Override
    public void onUserLeaveHint() {
        AppUtil.getInstance(this).setInBackground(true);
    }

}