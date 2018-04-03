package com.palebluedot.pcarstimetrial;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DEBUG";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;  // permission

    private TextView infoTView;
    private TextView sessionTView, positionTView;
    private TextView fastestDriverTView, fastestDriverNameTView;
    private TextView currentLapTView, lastLapTView, bestLapTView, recordLapTView;
    private TextView trackTView, carTView;
    private TextView currentLapNumberTView;
    private ToggleButton onoffTGButton;
    private TextView gameStateTView;
    private TextView gapTView;

    private String broadcast;
    private boolean timerOn;
    private String trackName, carName, carClass;
    private float recordLap, bestLaptime, bestSector1, bestSector2, bestSector3;
    private float fastestDriver;
    private String fastestDriverName;

    private int playerIndex;
    private int numParticipants;
    private boolean gameStateChanged;


    private Timer timer = null;
    private SQLiteDatabase database;
    private TreeMap<Integer, FastLap> lapMap;   // session laps
    private WifiManager.MulticastLock mLock;
    private int gameState;
    private boolean permissionGranted;  // permission to read/write to external storage api 23+

    private SessionFragment mSessionFragment;
    private MulticlassFragment mMultiFragment;
    private VersusFragment mVersusFragment;

    private AdView mAdView;     /* Google AdMob */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // test network
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            InetAddress inet = getBroadcastAddress();
            if (inet != null) {
                broadcast = inet.getHostAddress();
                Log.d(TAG, broadcast); // finds broadcast address
            } else {
                AlertDialog.Builder noNetworkBuilder = new AlertDialog.Builder(MainActivity.this);
                noNetworkBuilder.setTitle("Network problem");
                noNetworkBuilder.setMessage(R.string.no_network)
                        .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // exit application
                                finish();

                            }
                        }).create().show();
            }
        }

        // permission to read/write to external storage api 23+
        verifyStoragePermissions(MainActivity.this);

        // keeps screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // find toolbar view in activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // set toolbar to act as the ActionBar for this activity window
        setSupportActionBar(toolbar);

        // tab layout: session laps and multi class laps
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(3); // pages to be retained to either side of the current page
        viewPager.setAdapter(new MyFragmentPagerAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        // references to ListViews in TabLayout formed by 3 fragments
        mSessionFragment = SessionFragment.getInstance();
        mMultiFragment = MulticlassFragment.getInstance();
        mVersusFragment = VersusFragment.getInstance();

        // session and database laps
        lapMap = new TreeMap<>();

        infoTView = (TextView) findViewById(R.id.network_info_textview);
        sessionTView = (TextView) findViewById(R.id.timing_session_tv);
        positionTView = (TextView) findViewById(R.id.timing_position_tv);
        fastestDriverTView = (TextView) findViewById(R.id.timing_fastest_time_tv);
        fastestDriverNameTView = (TextView) findViewById(R.id.timing_fastest_name_tv);
        currentLapTView = (TextView) findViewById(R.id.timing_currentlap_tv);
        lastLapTView = (TextView) findViewById(R.id.timing_lastlap_tv);
        bestLapTView = (TextView) findViewById(R.id.timing_bestlap_tv);
        recordLapTView = (TextView) findViewById(R.id.timing_recordlap_tv);
        trackTView = (TextView) findViewById(R.id.track_textview);
        carTView = (TextView) findViewById(R.id.car_textview);
        currentLapNumberTView = (TextView) findViewById(R.id.timing_currentlapnumber_tv);
        gameStateTView = (TextView) findViewById(R.id.gamestate_textview);
        onoffTGButton = (ToggleButton) findViewById(R.id.onoff_button);
        onoffTGButton.setChecked(true);
        gapTView = (TextView) findViewById(R.id.timing_gapbestrecord_tv);
        hardReset();

        //
        // Google AdMob
        //
        MobileAds.initialize(this, "ca-app-pub-9903715050900661~9687444415");
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_database_action:
                //Log.d(TAG, "GameState: " + gameState);
                if (Mode.MENU.equals(gameState) || Mode.BOOTING.equals(gameState)) {
                    Intent intent = new Intent(this, DatabaseActivity.class);
                    intent.putExtra("permissionGranted", permissionGranted);
                    startActivity(intent);
                } else {
                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                    dialog.setTitle("Edit database");
                    dialog.setMessage("Please exit race to be able to edit database.");
                    dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
                return true;
            case R.id.exit_action:
                AlertDialog.Builder exitBuilder = new AlertDialog.Builder(MainActivity.this);
                exitBuilder.setMessage(R.string.exit_message_dialog)
                        .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // exit application
                                finish();

                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                exitBuilder.create().show();
                return true;
            case R.id.about_main:
                ImageView image = new ImageView(this);
                image.setImageResource(R.drawable.about_image);
                AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this)
                        .setMessage(R.string.about_message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).setView(image);
                aboutBuilder.create().show();
                return true;
            case R.id.pcars_ta_action:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.pbluedotsoft.pcarstimeattacklite"));
                if(intent.resolveActivity(getPackageManager()) != null)
                    startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // timer
        timerOn = false;
        timer.cancel();
        timer.purge();
        timer = null;
        // database
        //database.close();
        Log.d(TAG, "MainActivity.onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // database
        database = GlobalApplication.getDatabase();

        // wifi lock (need it for motorola 3g)
        WifiManager wifi;
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mLock = wifi.createMulticastLock("lock");
        // task and timer
        TimerTask mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (timerOn)
                    new PacketGrabber().execute();  // AsyncTask class
            }
        };
        timer = new Timer();
        timer.schedule(mTimerTask, 0, 50); // run task every 50 ms
        timerOn = true;
        Log.d(TAG, "MainActivity.onResume()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    /**
     * Ask user for permission to read/write external storage and dialog response
     * gets handled by onRequestPermissionsResult()
     *
     * @param activity
     */

    public void verifyStoragePermissions(Activity activity) {
        String[] PERMISSIONS_STORAGE = {
                //Manifest.permission.READ_EXTERNAL_STORAGE,    // requires api 16+
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        // check if we have write permission
        int permission = ActivityCompat
                .checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have automatic permission, so prompt the user.
            // onRequestPermissionsResult handles the answer from user.
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        } else {
            permissionGranted = true;
        }
    }

    /**
     * Handles user response to dialog ActivityCompat.requestPermissions
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                } else {
                    permissionGranted = false;
                }
                break;
        }
    }

    /**
     * Reset when in main menu
     */
    private void hardReset() {
        softReset();
        // variables
        recordLap = 0;      // force first query on database after race restart
        trackName = null;
        // ui
        recordLapTView.setText("--:--:---");
        trackTView.setText("Track location");
        carTView.setText("Car");
        Log.d(TAG, "HARD Reset: main menu");
    }

    /**
     * Reset between game states, except main menu
     * It does not reset track, car, record
     */
    private void softReset() {
        // variables
        lapMap.clear();
        // ui
        sessionTView.setText("");
        positionTView.setText("-");
        currentLapTView.setText("--:--:---");
        lastLapTView.setText("--:--:---");
        bestLapTView.setText("--:--:---");
        currentLapNumberTView.setText("-");
        gapTView.setText("    -.---");
        fastestDriver = Float.MAX_VALUE;
        fastestDriverName = null;
        fastestDriverTView.setText("--:--:---");
        fastestDriverNameTView.setText("Fastest lap");
        Log.d(TAG, "SOFT Reset: between game states");
    }

    /**
     * Finds out broadcast address in network
     *
     * @return
     */
    private InetAddress getBroadcastAddress() {
        InetAddress broadcastAddress = null;
        try {
            Enumeration<NetworkInterface> networkInterface = NetworkInterface
                    .getNetworkInterfaces();

            while (broadcastAddress == null
                    && networkInterface.hasMoreElements()) {
                NetworkInterface singleInterface = networkInterface.nextElement();
                String interfaceName = singleInterface.getName();
                if (interfaceName.contains("wlan0") || interfaceName.contains("eth0")) {
                    for (InterfaceAddress interfaceAddress : singleInterface.getInterfaceAddresses()) {
                        broadcastAddress = interfaceAddress.getBroadcast();
                        if (broadcastAddress != null) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return broadcastAddress;
    }

    /**
     * Inner class
     */
    private class PacketGrabber extends AsyncTask<Void, Void, Boolean> {
        private float currentLaptime, lastLaptime;
        private float lastSector;
        private String packetType;
        private int currentLapNumber, totalLapNumber;
        private int position;
        private int currentSectorNumber;
        private int invalidLap;
        private boolean sessionFragmentOutdated;    // when fastlap object changes ui gets updated
        private boolean multiFragmentOutdated;      // when record gets beaten

        public PacketGrabber() {
            //Log.d(TAG, "PacketGrabber created...");
            //gameState = -1;
        }

        /**
         * Background processing for asynchronous task
         *
         * @param params
         * @return
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            // is activity running and database ready?
            if (!timerOn || database == null)
                return false;

            boolean packetIsGood = true;
            int port = 5606;
            byte[] messageByte = new byte[5000];
            DatagramSocket socket = null;
            try {
                DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length);
                // todo: when running on emulator configure network adapter as bridge for the virtual
                // todo: device in genymotion and enter broadcast ip in the variable broadcast under
//                broadcast = "192.168.1.255";    // todo: remove when running on real device
                mLock.acquire();
                socket = new DatagramSocket(port, InetAddress.getByName(broadcast));
                socket.setBroadcast(true);
                socket.receive(packet);
                if (mLock.isHeld())
                    mLock.release();

                byte[] packetBytes = packet.getData();

                // analyze packet. Note: if [3] is null then packet is corrupted.
                if (packet.getLength() == 1347 && packetBytes[3] != '\0') {
                    // ---------------------------------------------- struct sParticipantInfoStrings
                    //Log.d(TAG, "1347 PACKET...");
                    packetType = "1347";    // used to update only UI widgets related to "1347"
                    // car name
                    carName = new String(Arrays.copyOfRange(packetBytes, 3, 67), "UTF-8");
                    int pos = carName.indexOf('\0');
                    if (pos > 0) {
                        carName = carName.substring(0, pos);
                    }

                    // car class
                    carClass = new String(Arrays.copyOfRange(packetBytes, 67, 131), "UTF-8");
                    pos = carClass.indexOf('\0');
                    if (pos > 0) {
                        carClass = carClass.substring(0, pos);
                    }

                    // track name
                    trackName = new String(Arrays.copyOfRange(packetBytes, 131, 195), "UTF-8");
                    pos = trackName.indexOf('\0');    // find first null in the string
                    if (pos > 0) {
//                        Log.d(TAG, "trackName: " + trackName);
                        trackName = trackName.substring(0, pos);
                    }

                    String variationName = new String(Arrays.copyOfRange(packetBytes, 195, 259), "UTF-8");
                    pos = variationName.indexOf('\0');
                    if (pos > 0) {
//                        Log.d(TAG, "variationName: " + variationName);
                        variationName = variationName.substring(0, pos);
                    }
                    trackName = trackName + " " + variationName;

                    // get drivers names and fastest lap
                    String[] drivers = new String[numParticipants];
                    for (int i = 0; i < numParticipants; i++) {
                        String temp = new String(Arrays
                                .copyOfRange(packetBytes, 259 + (i * 64), 259 + (i * 64) + 64), "UTF-8");

                        float lap = ByteBuffer.wrap(packetBytes, 1283 + (i * 4), 4)
                                .order(ByteOrder.LITTLE_ENDIAN).getFloat();

                        pos = temp.indexOf('\0');    // find first null in the string
                        if (pos > 0) {
                            drivers[i] = temp.substring(0, pos);
                        }

                        if (lap > 0 && lap < fastestDriver) {
                            fastestDriver = lap;
                            fastestDriverName = drivers[i];
                        }

                        if (fastestDriverName  == null) {
                            fastestDriverName = "Fastest lap";
                        }

                        //Log.d(TAG, String.format("#%02d: %-32s%10s", i, drivers[i], FastLap.format(lap)));
                    }

                } else if (packet.getLength() == 1367) {
                    // ------------------------------------------------------- struct sTelemetryData
                    // start measure performance
                    //long before = System.currentTimeMillis();

                    // Under development: find position car in track
                    // http://forum.projectcarsgame.com/showthread.php?39269-Provide-GPS-coordinates-of-all-track-reference-points-GPS-calculation-in-javascript
//                    float posX = packetBytes[464 + (playerIndex * 16)] & 0xFFFF;
//                    float posY = packetBytes[464 + (playerIndex * 16) + 2] & 0xFFFF;
//                    float posZ = packetBytes[464 + (playerIndex * 16) + 4] & 0xFFFF;
//
//                    Log.d(TAG, "World position: " + posX + ", " + posY + ", " + posZ);

                    packetType = "1367";

                    // check when gameState changes to perform UI/variables resets
                    if ((packetBytes[3] & 0xFF) != gameState) {
                        gameState = packetBytes[3] & 0xFF;
                        gameStateChanged = true;    // flag helps reset UI and other variables
                    }

                    // fixed bug: every now and then a packet will give 0 as index
                    if (playerIndex != 0 && playerIndex != 255 && (packetBytes[4] & 0xFF) == 0) {
                        // jump corrupted 1367 that gives 0 as index
                        Log.d(TAG, "corrupted......................................................");
                    } else {
                        playerIndex = packetBytes[4] & 0xFF;
                    }

                    //Log.d(TAG, " playerIndex: " + playerIndex);

                    //
                    totalLapNumber = packetBytes[11] & 0xFF;
                    position = packetBytes[464 + (playerIndex * 16) + 8] & 0x7F;
                    numParticipants = packetBytes[5] & 0xFF;
                    currentLapNumber = packetBytes[464 + (playerIndex * 16) + 10] & 0xFF;
                    currentSectorNumber = packetBytes[464 + (playerIndex * 16) + 11] & 0x07;

                    // lap times
                    currentLaptime = ByteBuffer.wrap(packetBytes, 20, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    bestLaptime = ByteBuffer.wrap(packetBytes, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    lastLaptime = ByteBuffer.wrap(packetBytes, 16, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                    // last sector
                    lastSector = ByteBuffer.wrap(packetBytes, 464 + (playerIndex * 16) + 12, 4)
                            .order(ByteOrder.LITTLE_ENDIAN).getFloat();

                    // best lap sectors
                    bestSector1 = ByteBuffer.wrap(packetBytes, 60, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    bestSector2 = ByteBuffer.wrap(packetBytes, 64, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    bestSector3 = ByteBuffer.wrap(packetBytes, 68, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                    // invalid lap
                    invalidLap = ((packetBytes[464 + (playerIndex * 16) + 9] & 0xFF) >> 7);

                    // extra race info (race tab - work in progress)
                    /*
                    String session = Mode.getString(gameState);
                    String worldFastest = FastLap.format(ByteBuffer.wrap(packetBytes, 44, 4)
                            .order(ByteOrder.LITTLE_ENDIAN).getFloat());
                    float splitAhead = ByteBuffer.wrap(packetBytes, 24, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    float splitBehind = ByteBuffer.wrap(packetBytes, 28, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    float split = ByteBuffer.wrap(packetBytes, 32, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    int[] tyreTemp = new int[4];
                    tyreTemp[0] = packetBytes[276] & 0xFF;
                    tyreTemp[1] = packetBytes[277] & 0xFF;
                    tyreTemp[2] = packetBytes[278] & 0xFF;
                    tyreTemp[3] = packetBytes[279] & 0xFF;
                    int[] tyreWear = new int[4];
                    tyreWear[0] = packetBytes[316] & 0xFF;
                    tyreWear[1] = packetBytes[317] & 0xFF;
                    tyreWear[2] = packetBytes[318] & 0xFF;
                    tyreWear[3] = packetBytes[319] & 0xFF;
                    */

                    //Log.d(TAG, "Index: " + playerIndex + " Position: " + position + " Lap: " + currentLapNumber + " Sector: " + currentSectorNumber);

                    // end measure performance
                    //long after = System.currentTimeMillis();
                    //Log.d(TAG, "Delay: " + (after - before));

                    /*
                    Log.d(TAG, session + " " +
                            position + " " +
                            totalLapNumber + " " +
                            numParticipants + " " +
                            worldFastest + " " +
                            splitAhead + " " +
                            splitBehind + " " +
                            split + " " +
                            tyreTemp[0] + " " +
                            tyreWear[0] + " "
                        );
                    */

                } else {
                    // another packet size or damaged
                    packetType = "another";
                    packetIsGood = false;
                    Log.d(TAG, "Another PACKET with size: " + packet.getLength());
                }
                socket.close();

            } catch (Exception ex) {
                if (socket != null) {
                    socket.close();
                }
                packetIsGood = false;
                Log.d(TAG, "Exception: " + ex);
                ex.printStackTrace();
            }
            return packetIsGood;
        }

        /**
         * Post onBackground asynchronous task processing. Updates widgets in UI
         *
         * @param packetIsGood
         */
        @Override
        protected void onPostExecute(Boolean packetIsGood) {
            super.onPostExecute(packetIsGood);
            if (!packetIsGood || !timerOn)
                return;

            if (packetType.equals("1367")) {
                onPostExecute1367();

            } else if (packetType.equals("1347")) {
                // update session mode
                sessionTView.setText(Mode.getString(gameState));
                fastestDriverTView.setText(FastLap.format(fastestDriver));
                fastestDriverNameTView.setText(fastestDriverName);
                if (mVersusFragment.getTrackName() == null ||
                        !mVersusFragment.getTrackName().equals(trackName)) {

                    // Attention: TrackName has just changed.
                    mVersusFragment.setTrackName(trackName);    // send track name to fragment
                    Log.d(TAG, "onPostExecute() -> track name available: " + trackName);
                    onPostExecute1347();
                }
            }

            /*
             * Resolve flags that got activated while processing packet
             */

            // update sessionListView
            if (sessionFragmentOutdated) {
                updateSessionListView();
            }

            // update multiListView when track changes and when new record
            if (multiFragmentOutdated) {
                updateMultiListView(trackName);
                mVersusFragment.updateCarSpinner(); // keep spinner in versus updated
            }
        }

        /**
         * Process packet 1367
         * Information about telemetry (lap times, sectors, game state, etc.)
         */
        private void onPostExecute1367() {
            gameStateTView.setText(gameState + " Sector: " + currentSectorNumber);

            // reset between different game states
            if (gameStateChanged) {
                gameStateChanged = false;
                // back to main menu?
                if (Mode.MENU.equals(gameState)) {
                    hardReset();
                    mVersusFragment.reset();    // full reset versus fragment
                    mSessionFragment.update(null);
                    mMultiFragment.update(null);
                    return; // no more processing needed when in menu
                } else {
                    // keep track, car and record
                    softReset();
                }
            }

            // first lap reset
            if (currentLaptime == -1 && currentLapNumber == 1) {
                //Log.d(TAG, "Starting race......" + playerIndex);
                if (!mSessionFragment.isEmpty()) {
                    softReset();
                    sessionFragmentOutdated = true;
                    mVersusFragment.restartRace();  // reset UI
                }
            }

            // set lap as invalid if red flag in race
            if (invalidLap == 1 && lapMap.containsKey(currentLapNumber)) {
                FastLap fLap = lapMap.get(currentLapNumber);
                // only update once
                if (fLap.isValid()) {
                    fLap.setValid(false);
                    lapMap.put(currentLapNumber, fLap);
                    sessionFragmentOutdated = true;
                }
            }

            // perform actions according to the sector where the car is located
            if (currentSectorNumber == 1 && invalidLap == 0) {
                // insert new FastLap object in list with current lap as key
                if (!lapMap.containsKey(currentLapNumber)) {
                    lapMap.put(currentLapNumber, new FastLap(currentLapNumber));
                    sessionFragmentOutdated = true;
                    //Log.d(TAG, "S1 -> new lap added with key: " + currentLapNumber);
                } else if (!lapMap.get(currentLapNumber).isValid()) {
                    // reset current lap (after pit box)
                    Log.d(TAG, "RESET currentLap after pit box");
                    lapMap.get(currentLapNumber).setValid(true);
                    lapMap.get(currentLapNumber).setLaptime(1, 0);  // s1
                    lapMap.get(currentLapNumber).setLaptime(2, 0);  // s2
                    sessionFragmentOutdated = true;
                }

                // here it is where we update sector 3 and lap time for previous lap
                if (lapMap.containsKey(currentLapNumber - 1) && lapMap.get(currentLapNumber - 1).isValid()) {
                    FastLap fLap = lapMap.get(currentLapNumber - 1);
                    // update only once
                    if (fLap.getLaptime(0) == 0) {
                        //Log.d(TAG, "S1 -> Update sector 3 for lap: " + (currentLapNumber - 1));
                        fLap.setLaptime(3, lastSector);
                        fLap.setLaptime(0, lastLaptime);
                        lapMap.put(currentLapNumber - 1, fLap);
                        sessionFragmentOutdated = true;

                        if (fLap.isValid() && fLap.getLaptime(0) == bestLaptime) {
                            if (trackName != null) {
                                mVersusFragment.setBest(fLap);
                            }
                            // should we update database? record is -1 when db already queried but no record found
                            if (fLap.getLaptime(0) < recordLap || recordLap == -1) {
                                recordLap = fLap.getLaptime(0);
                                fLap.setTrack(trackName);  // trackName != null since lapRecord != 0
                                fLap.setCar(carName);
                                fLap.setClassGroup(carClass);
                                updateDatabase(fLap);
                                multiFragmentOutdated = true;
                                Log.d(TAG, "onPostExecute1367 -> recordLap inserted in database: " + recordLap);
                            } else {
                                Log.d(TAG, "onPostExecute1367 -> bestlap doesn't improve record/no track info yet");
                            }
                        }
                    }
                }

                // snicky best lap times that do not update database?
                if (bestLaptime > 0 && bestLaptime < recordLap) {
                    Log.d(TAG, "SNICKY best: " + bestLaptime + " unINFORMED record: " + recordLap);
                }


            } else if (currentSectorNumber == 2 && lapMap.containsKey(currentLapNumber) && invalidLap == 0) {
                // update sector 1 for current lap
                FastLap fLap = lapMap.get(currentLapNumber);
                // has already been updated?
                if (fLap.getLaptime(1) == 0) {
                    fLap.setLaptime(1, lastSector);
                    lapMap.put(currentLapNumber, fLap);
                    sessionFragmentOutdated = true;
                    //Log.d(TAG, "S2 -> update sector 1 for lap: " + currentLapNumber);
                }

            } else if (currentSectorNumber == 3 && lapMap.containsKey(currentLapNumber) && invalidLap == 0) {
                FastLap fLap = lapMap.get(currentLapNumber);
                // currentSectorNumber is 3 before green light -> gotta check that s1 has time
                // so it is not the starting lap AND update only once
                if (fLap.getLaptime(1) != 0 && fLap.getLaptime(2) == 0) {
                    fLap.setLaptime(2, lastSector);
                    lapMap.put(currentLapNumber, fLap);
                    sessionFragmentOutdated = true;
                    //Log.d(TAG, "S3 -> update sector 2 for lap: " + currentLapNumber);
                }
            }

            // update UI
            currentLapTView.setText(FastLap.format(currentLaptime));
            if (invalidLap == 1) {
                currentLapTView.setTextColor(ContextCompat.getColor(getApplicationContext(),
                        R.color.invalidLap));
            } else {
                currentLapTView.setTextColor(ContextCompat.getColor(getApplicationContext(),
                        R.color.validLap));
            }

            positionTView.setText(String
                    .format(Locale.ENGLISH, "%2d/%-2d", position,
                            (numParticipants == 255) ? 0 : numParticipants));

            if (Mode.RACE.equals(gameState)) {
                currentLapNumberTView.setText(String
                        .format(Locale.ENGLISH, "%2d/%-2d", currentLapNumber, totalLapNumber));
            } else {
                currentLapNumberTView.setText(String
                        .format(Locale.ENGLISH, "%2d", currentLapNumber));
            }
            lastLapTView.setText(FastLap.format(lastLaptime));
            bestLapTView.setText(FastLap.format(bestLaptime));
            recordLapTView.setText(FastLap.format(recordLap));
            if (recordLap > 0 && bestLaptime > 0) {
                float gap = Math.abs(BigDecimal.valueOf(bestLaptime - recordLap).setScale(3,
                        BigDecimal.ROUND_HALF_UP).floatValue());
                gapTView.setText(String.format(Locale.ENGLISH, "%9s", "+" + Float.toString(gap)));
            }
        }

        /**
         * Process packet 1347
         * Track and car names used in the current session
         */
        private void onPostExecute1347() {
            // get record from DB, returns -1 if not found
            recordLap = queryRecord();

            // App started in the MIDDLE of a race? check that bestLap is not better than database's
            if (bestLaptime > 0) {
                FastLap fLap = new FastLap();
                fLap.setTrack(trackName);
                fLap.setCar(carName);
                fLap.setClassGroup(carClass);
                fLap.setLaptime(0, bestLaptime);
                fLap.setLaptime(1, bestSector1);
                fLap.setLaptime(2, bestSector2);
                fLap.setLaptime(3, bestSector3);
                mVersusFragment.setBest(fLap);  // inform fragment

                if (bestLaptime < recordLap || recordLap == -1) {
                    recordLap = bestLaptime;
                    updateDatabase(fLap);
                    Log.d(TAG, "onPostExecute1347 -> App started in the MIDDLE and bestLap is better than database's");
                } else {
                    Log.d(TAG, "onPostExecute1347 -> db record: " + recordLap);
                }
            }

            // update UI
            carTView.setText(carName);
            trackTView.setText(trackName);
            multiFragmentOutdated = true;   // update multiFragment
        }

        /**
         * Update record or insert new record in database
         */
        private void updateDatabase(FastLap laptime) throws SQLiteException {
            // if no track name then wait until it is available.
            if (laptime.getTrack().equals("") || laptime.getLaptime(0) < 1)
                return;

            // new value
            ContentValues values = new ContentValues();
            values.put(DBTable.COLUMN_LAPTIME, laptime.getLaptime(0));
            values.put(DBTable.COLUMN_SECTOR1, laptime.getLaptime(1));
            values.put(DBTable.COLUMN_SECTOR2, laptime.getLaptime(2));
            values.put(DBTable.COLUMN_SECTOR3, laptime.getLaptime(3));
            // query
            String selection = DBTable.COLUMN_TRACK + " LIKE ? AND " +
                    DBTable.COLUMN_CAR + " LIKE ? ";
            String[] args = {laptime.getTrack(), laptime.getCar()};
            int result = database.update(DBTable.TABLE_NAME, values, selection, args);
            // if there was no record for this car/track insert new row
            if (result == 0) {
                values.put(DBTable.COLUMN_TRACK, laptime.getTrack());
                values.put(DBTable.COLUMN_CAR, laptime.getCar());
                values.put(DBTable.COLUMN_CLASS, laptime.getClassGroup());
                database.insert(DBTable.TABLE_NAME, null, values);
                Log.d(TAG, "DB: " + laptime.getTrack() + "/" + laptime.getCar() + "/ " +
                        laptime.getClassGroup() + " inserted: " + FastLap.format(laptime.getLaptime(0)));
            } else {
                Log.d(TAG, "DB: " + laptime.getTrack() + "/" + laptime.getCar() + " " + "/ " +
                        laptime.getClassGroup() + FastLap.format(laptime.getLaptime(0)) + " updated");
            }
        }

        /**
         * Updates session laps ListView
         */
        private void updateSessionListView() {
            FastLap[] temp = lapMap.values().toArray(new FastLap[lapMap.size()]);
            SessionAdapter adapter = new SessionAdapter(getApplicationContext(), temp);
            mSessionFragment.update(adapter);
            //Log.d(TAG, "sessionListView UI updated...");
        }

        /**
         * Updates best in class and multiclass ListViews on this track
         * It should also be called after restarts, since user can change track
         */
        private void updateMultiListView(String track) {
            // Fragment multi class
            MulticlassAdapter multiAdapter = null;
            if (track != null) {
                String selection = DBTable.COLUMN_TRACK + "  LIKE ?";
                String[] args = {track};
                String sortOrder = DBTable.COLUMN_LAPTIME + " ASC";
                Cursor cursor = database.query(DBTable.TABLE_NAME,
                        null, selection, args, null, null, sortOrder);

                multiAdapter = new MulticlassAdapter(getApplicationContext(), cursor, 0);

                // inform adapter about the car in this session so adapter can highlight it
                multiAdapter.setActualCar(carName);
                Log.d(TAG, "multiListView - multi: " + track);
            } else {
                Log.d(TAG, "multiListView - multi with null track");
            }
            mMultiFragment.update(multiAdapter);
        }

        /**
         * @return -1 when there is not record for current track on this track in database yet,
         * so doInBackground stops asking for it
         */
        private float queryRecord() {
            float record = -1;
            String selection = DBTable.COLUMN_TRACK + " LIKE ? AND " +
                    DBTable.COLUMN_CAR + " LIKE ? ";
            String[] args = {trackName, carName};
            Cursor cursor = database.query(DBTable.TABLE_NAME, null, selection, args, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                record = cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_LAPTIME));
            }

            return record;
        }

        /**
         * @param bytes
         * @return
         */
        private String bytesToHex(byte[] bytes) {
            final char[] hexArray = "0123456789ABCDEF".toCharArray();
            char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }

    }

    /**
     * Exit dialog fragment
     */
    public static class ExitDialogFragment extends DialogFragment {

        public ExitDialogFragment() {
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.exit_message_dialog)
                    .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // exit application
                            ((Activity) getContext()).finish();

                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            // create the AlertDialog object and return it
            return builder.create();
        }
    }
}
