package piuk.blockchain.android.ui.directory;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import info.blockchain.api.MerchantDirectory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.base.BaseAuthActivity;

//import android.util.Log;

public class MapActivity extends BaseAuthActivity implements LocationListener, OnMapReadyCallback {

    private final String TAG = getClass().getSimpleName();

    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    private static final int radius = 40000;
    public static ArrayList<MerchantDirectory.Merchant> merchantList = null;
    private static float Z00M_LEVEL_DEFAULT = 13.0f;
    private static float Z00M_LEVEL_CLOSE = 18.0f;
    private static boolean launchedList = false;
    private GoogleMap map = null;
    private LocationManager locationManager = null;
    private Location currLocation = null;
    private float saveZ00mLevel = Z00M_LEVEL_DEFAULT;
    private boolean changeZoom = false;
    private final int color_category_selected = 0xffFFFFFF;
    private final int color_category_unselected = 0xffF1F1F1;
    private final int color_cafe_selected = 0xffc12a0c;
    private final int color_drink_selected = 0xffb65db1;
    private final int color_eat_selected = 0xfffd7308;
    private final int color_spend_selected = 0xff5592ae;
    private final int color_atm_selected = 0xff4dad5c;
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
    private TextView tvTel = null;
    private TextView tvWeb = null;
    private TextView tvDesc = null;
    private boolean cafeSelected = true;
    private boolean drinkSelected = true;
    private boolean eatSelected = true;
    private boolean spendSelected = true;
    private boolean atmSelected = true;
    private HashMap<String, MerchantDirectory.Merchant> markerValues = null;
    private LatLngBounds bounds = null;
    private LinearLayout infoLayout = null;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setTitle(R.string.merchant_map);
        setSupportActionBar(toolbar);

        markerValues = new HashMap<>();
        merchantList = new ArrayList<>();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ?
                LocationManager.GPS_PROVIDER :
                LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

        infoLayout = ((LinearLayout) findViewById(R.id.info));
        infoLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeBottom() {
                if (infoLayout.getVisibility() == View.VISIBLE) {
                    infoLayout.setVisibility(View.GONE);
                    map.animateCamera(CameraUpdateFactory.zoomTo(saveZ00mLevel));
                }
            }
        });
        infoLayout.setVisibility(View.GONE);

        tvName = (TextView) findViewById(R.id.tv_name);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        tvTel = (TextView) findViewById(R.id.tv_tel);
        tvWeb = (TextView) findViewById(R.id.tv_web);
        tvDesc = (TextView) findViewById(R.id.tv_desc);

        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;

        map.setMyLocationEnabled(true);
        map.setOnMarkerClickListener(marker -> {

            if (marker == null) {
                return true;
            }

            if (markerValues == null || markerValues.size() < 1) {
                return true;
            }

            findViewById(R.id.row_call).setVisibility(View.VISIBLE);
            findViewById(R.id.row_web).setVisibility(View.VISIBLE);

            LatLng latLng = marker.getPosition();

            MerchantDirectory.Merchant b = markerValues.get(marker.getId());

            String url = "http://maps.google.com/?saddr=" +
                    currLocation.getLatitude() + "," + currLocation.getLongitude() +
                    "&daddr=" + markerValues.get(marker.getId()).latitude + "," + markerValues.get(marker.getId()).longitude;
            tvAddress.setText(Html.fromHtml("<a href=\"" + url + "\">" + b.address + ", " + b.city + " " + b.postal_code + "</a>"));
            tvAddress.setMovementMethod(LinkMovementMethod.getInstance());

            if (b.phone != null && b.phone.trim().length() > 0) {
                tvTel.setText(b.phone);
                Linkify.addLinks(tvTel, Linkify.PHONE_NUMBERS);
            } else {
                findViewById(R.id.row_call).setVisibility(View.GONE);
            }

            if (b.website != null && b.website.trim().length() > 0) {
                tvWeb.setText(b.website);
                Linkify.addLinks(tvWeb, Linkify.WEB_URLS);
            } else {
                findViewById(R.id.row_web).setVisibility(View.GONE);
            }

            tvDesc.setText(b.description);

            tvName.setText(b.name);
            int category;
            try {
                category = b.category_id;
            } catch (Exception e) {
                category = 0;
            }
            switch (category) {
                case MerchantDirectory.Merchant.HEADING_CAFE:
                    tvName.setTextColor(color_cafe_selected);
                    break;
                case MerchantDirectory.Merchant.HEADING_BAR:
                    tvName.setTextColor(color_drink_selected);
                    break;
                case MerchantDirectory.Merchant.HEADING_RESTAURANT:
                    tvName.setTextColor(color_eat_selected);
                    break;
                case MerchantDirectory.Merchant.HEADING_SPEND:
                    tvName.setTextColor(color_spend_selected);
                    break;
                case MerchantDirectory.Merchant.HEADING_ATM:
                    tvName.setTextColor(color_atm_selected);
                    break;
                default:
                    tvName.setTextColor(color_cafe_selected);
                    break;
            }

            infoLayout.setVisibility(View.VISIBLE);

            saveZ00mLevel = map.getCameraPosition().zoom;
            if (map.getCameraPosition().zoom < Z00M_LEVEL_CLOSE) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), Z00M_LEVEL_CLOSE));
            } else {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), map.getCameraPosition().zoom));
            }

            return true;
        });

        imgCafe = ((ImageView) findViewById(R.id.cafe));
        layoutCafe = ((LinearLayout) findViewById(R.id.layout_cafe));
        dividerCafe = ((LinearLayout) findViewById(R.id.divider_cafe));
        imgDrink = ((ImageView) findViewById(R.id.drink));
        layoutDrink = ((LinearLayout) findViewById(R.id.layout_drink));
        dividerDrink = ((LinearLayout) findViewById(R.id.divider_drink));
        imgEat = ((ImageView) findViewById(R.id.eat));
        layoutEat = ((LinearLayout) findViewById(R.id.layout_eat));
        dividerEat = ((LinearLayout) findViewById(R.id.divider_eat));
        imgSpend = ((ImageView) findViewById(R.id.spend));
        layoutSpend = ((LinearLayout) findViewById(R.id.layout_spend));
        dividerSpend = ((LinearLayout) findViewById(R.id.divider_spend));
        imgATM = ((ImageView) findViewById(R.id.atm));
        layoutATM = ((LinearLayout) findViewById(R.id.layout_atm));
        dividerATM = ((LinearLayout) findViewById(R.id.divider_atm));
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

        layoutCafe.setOnTouchListener((v, event) -> {
            saveZ00mLevel = map.getCameraPosition().zoom;
            changeZoom = false;
            imgCafe.setImageResource(cafeSelected ? R.drawable.marker_cafe_off : R.drawable.marker_cafe);
            dividerCafe.setBackgroundColor(cafeSelected ? color_category_unselected : color_cafe_selected);
            cafeSelected = cafeSelected ? false : true;
            drawData(false, null, null, false);
            return false;
        });

        layoutDrink.setOnTouchListener((v, event) -> {
            saveZ00mLevel = map.getCameraPosition().zoom;
            changeZoom = false;
            imgDrink.setImageResource(drinkSelected ? R.drawable.marker_drink_off : R.drawable.marker_drink);
            dividerDrink.setBackgroundColor(drinkSelected ? color_category_unselected : color_drink_selected);
            drinkSelected = drinkSelected ? false : true;
            drawData(false, null, null, false);
            return false;
        });

        layoutEat.setOnTouchListener((v, event) -> {
            saveZ00mLevel = map.getCameraPosition().zoom;
            changeZoom = false;
            imgEat.setImageResource(eatSelected ? R.drawable.marker_eat_off : R.drawable.marker_eat);
            dividerEat.setBackgroundColor(eatSelected ? color_category_unselected : color_eat_selected);
            eatSelected = eatSelected ? false : true;
            drawData(false, null, null, false);
            return false;
        });

        layoutSpend.setOnTouchListener((v, event) -> {
            saveZ00mLevel = map.getCameraPosition().zoom;
            changeZoom = false;
            imgSpend.setImageResource(spendSelected ? R.drawable.marker_spend_off : R.drawable.marker_spend);
            dividerSpend.setBackgroundColor(spendSelected ? color_category_unselected : color_spend_selected);
            spendSelected = spendSelected ? false : true;
            drawData(false, null, null, false);
            return false;
        });

        layoutATM.setOnTouchListener((v, event) -> {
            saveZ00mLevel = map.getCameraPosition().zoom;
            changeZoom = false;
            imgATM.setImageResource(atmSelected ? R.drawable.marker_atm_off : R.drawable.marker_atm);
            dividerATM.setBackgroundColor(atmSelected ? color_category_unselected : color_atm_selected);
            atmSelected = atmSelected ? false : true;
            drawData(false, null, null, false);
            return false;
        });

        currLocation = new Location(LocationManager.NETWORK_PROVIDER);
        Location lastKnownByGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastKnownByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnownByGps == null && lastKnownByNetwork == null) {
            currLocation.setLatitude(0.0);
            currLocation.setLongitude(0.0);
        } else if (lastKnownByGps != null && lastKnownByNetwork == null) {
            currLocation = lastKnownByGps;
        } else if (lastKnownByGps == null) {
            currLocation = lastKnownByNetwork;
        } else {
            currLocation = (lastKnownByGps.getAccuracy() <= lastKnownByNetwork.getAccuracy()) ? lastKnownByGps : lastKnownByNetwork;
        }

        launchedList = false;

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()), Z00M_LEVEL_DEFAULT));
        drawData(true, null, null, false);
    }

    @Override
    public void onLocationChanged(Location location) {

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        currLocation = location;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, map.getCameraPosition().zoom);
        map.animateCamera(cameraUpdate);
        // TODO: 04/08/2016 This needs permission checking, if only for Lint checks
        locationManager.removeUpdates(this);

        setProperZoomLevel(latLng, radius, 1);
//		drawData(true, null, null);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        launchedList = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.merchant_activity_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (infoLayout.getVisibility() == View.VISIBLE) {
                infoLayout.setVisibility(View.GONE);
            } else {
                finish();
            }
        }

        return false;
    }

    private void drawData(final boolean fetch, final Double lat, final Double lng, final boolean doListView) {

        if (map != null) {
            map.clear();
        }

        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(() -> {

            Looper.prepare();

            try {
                if (fetch) {

                    merchantList = new MerchantDirectory().getAllMerchants();
                }

                handler.post(() -> {

                    try {

                        if (merchantList != null && merchantList.size() > 0) {

                            MerchantDirectory.Merchant merchant = null;

                            for (int i = 0; i < merchantList.size(); i++) {

                                merchant = merchantList.get(i);

                                BitmapDescriptor bmd = null;

                                int category = merchant.category_id;

                                switch (category) {
                                    case MerchantDirectory.Merchant.HEADING_CAFE:
                                        if (cafeSelected) {
                                            bmd = merchant.featured_merchant ?
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe_featured) :
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe);
                                        } else {
                                            bmd = null;
                                        }
                                        break;
                                    case MerchantDirectory.Merchant.HEADING_BAR:
                                        if (drinkSelected) {
                                            bmd = merchant.featured_merchant ?
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_drink_featured) :
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_drink);
                                        } else {
                                            bmd = null;
                                        }
                                        break;
                                    case MerchantDirectory.Merchant.HEADING_RESTAURANT:
                                        if (eatSelected) {
                                            bmd = merchant.featured_merchant ?
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_eat_featured) :
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_eat);
                                        } else {
                                            bmd = null;
                                        }
                                        break;
                                    case MerchantDirectory.Merchant.HEADING_SPEND:
                                        if (spendSelected) {
                                            bmd = merchant.featured_merchant ?
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_spend_featured) :
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_spend);
                                        } else {
                                            bmd = null;
                                        }
                                        break;
                                    case MerchantDirectory.Merchant.HEADING_ATM:
                                        if (atmSelected) {
                                            bmd = merchant.featured_merchant ?
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_atm_featured) :
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_atm);
                                        } else {
                                            bmd = null;
                                        }
                                        break;
                                    default:
                                        if (cafeSelected) {
                                            bmd = merchant.featured_merchant ?
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe_featured) :
                                                    BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe);
                                        } else {
                                            bmd = null;
                                        }
                                        break;
                                }

                                if (bmd != null) {
                                    Marker marker = map.addMarker(new MarkerOptions()
                                            .position(new LatLng(merchant.latitude, merchant.longitude))
                                            .icon(bmd));

                                    markerValues.put(marker.getId(), merchant);
                                }

                            }

                            if (doListView) {
                                Intent intent = new Intent(MapActivity.this, ListActivity.class);
                                intent.putExtra("ULAT", Double.toString(lat));
                                intent.putExtra("ULON", Double.toString(lng));
                                startActivity(intent);
                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (changeZoom) {
                        setProperZoomLevel(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()), radius, 1);
                    } else {
                        changeZoom = true;
                    }

                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            Looper.loop();

        }).start();
    }

    void setProperZoomLevel(LatLng loc, int radius, int nbPoi) {

        if (merchantList == null || merchantList.size() < 1) {
            return;
        }

        float currentZoomLevel = 21;
        int currentFoundPoi = 0;
        LatLngBounds bounds = null;
        List<LatLng> found = new ArrayList<>();
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

            for (MerchantDirectory.Merchant merchant : merchantList) {

                LatLng pos = new LatLng(merchant.latitude, merchant.longitude);

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
                } else if (currentFoundPoi > 0) {
                    continueZooming = false;
                    break;
                } else if (currentZoomLevel < 3) {
                    continueZooming = false;
                    break;
                }

            }
            continueZooming = ((currentZoomLevel > 0) && continueZooming) ? true : false;

        }
    }

    private void doSuggest() {
        Intent intent = new Intent(MapActivity.this, SuggestMerchantActivity.class);
        intent.putExtra("ULAT", currLocation.getLatitude());
        intent.putExtra("ULON", currLocation.getLongitude());
        startActivity(intent);
    }
}