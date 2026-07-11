package com.oneclickvpn;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContentValues;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.VpnService;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.blinkt.openvpn.BuildConfig;
import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.activities.DisconnectVPN;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;

public class MainActivity extends Activity {
    private static final String[] API_URLS = {
            "https://www.vpngate.net/api/iphone/"
    };
    private static final String TAG = "NorenVPN";
    private static final String VPN_STATUS_ACTION = "de.blinkt.openvpn.VPN_STATUS";
    private static final int REQUEST_NOTIFICATIONS = 1001;
    private static final int REQUEST_VPN_PERMISSION = 1002;
    private static final int API_CONNECT_TIMEOUT_MS = 20000;
    private static final int API_READ_TIMEOUT_MS = 45000;
    private static final int API_ATTEMPTS_PER_URL = 3;
    private static final int MAX_SERVER_DIRECTORY_BYTES = 8 * 1024 * 1024;
    private static final int MAX_PUBLIC_IP_RESPONSE_BYTES = 64 * 1024;
    private static final int MAX_SERVER_ROWS = 500;
    private static final int MAX_CSV_COLUMNS = 32;
    private static final int MAX_CSV_LINE_CHARS = VpnGateConfigValidator.MAX_BASE64_CHARS + 16 * 1024;
    private static final long API_RETRY_BASE_DELAY_MS = 1500;
    private static final long SERVER_CACHE_STALE_MS = 6L * 60L * 60L * 1000L;
    private static final String SERVER_CACHE_FILE = "server_api_cache.csv";
    private static final long CONNECT_RETRY_LOCK_MS = 35000;
    private static final int CONNECT_PREFLIGHT_TIMEOUT_MS = 2500;
    private static final int CONNECT_PREFLIGHT_BATCH_TIMEOUT_MS = 6000;
    private static final int CONNECT_CANDIDATE_LIMIT = 16;
    private static final int AUTO_PRIMARY_CANDIDATE_LIMIT = 10;
    private static final int MAX_DISPLAYED_SERVERS = 20;
    private static final int CONNECT_AUTO_RETRY_LIMIT = 8;
    private static final long CONNECT_RETRY_DELAY_MS = 1500;
    private static final String OPENVPN_FOR_ANDROID = "de.blinkt.openvpn";
    private static final String OPENVPN_CONNECT = "net.openvpn.openvpn";
    private static final String OVPN_MIME = "application/x-openvpn-profile";
    private static final String PRO_PRODUCT_ID = "pro_server_selection";
    private static final String VPN_GATE_LOGGING_POLICY_URL = "https://www.vpngate.net/en/about_abuse.aspx";
    private static final String[] PUBLIC_IP_CHECK_URLS = {
            "https://v4.ifconfig.co/json",
            "https://ifconfig.co/json",
            "https://api.ipify.org?format=json"
    };
    private static final String PREF_PRO_UNLOCKED = "oneclickvpn.pro_unlocked";
    private static final String PREF_VPN_DISCLOSURE_ACCEPTED = "oneclickvpn.vpn_disclosure_accepted.v2";
    private static final String PREF_CONNECTED_HOST = "oneclickvpn.connected.host";
    private static final String PREF_CONNECTED_COUNTRY_SHORT = "oneclickvpn.connected.country_short";
    private static final String PREF_CONNECTED_COUNTRY_LONG = "oneclickvpn.connected.country_long";
    private static final int BG = Color.rgb(17, 19, 21);
    private static final int PANEL = Color.rgb(27, 30, 32);
    private static final int PANEL_LIGHT = Color.rgb(38, 49, 46);
    private static final int BLUE = Color.rgb(15, 139, 141);
    private static final int GREEN = Color.rgb(36, 168, 102);
    private static final int AMBER = Color.rgb(244, 185, 66);
    private static final int RED = Color.rgb(226, 93, 93);
    private static final int TEXT = Color.rgb(245, 247, 244);
    private static final int MUTED = Color.rgb(166, 171, 165);
    private static final int BORDER = Color.rgb(52, 57, 58);
    private static final int PALE_BLUE = Color.rgb(28, 52, 52);
    private static final int PALE_GREEN = Color.rgb(28, 54, 42);
    private static final AutoProfile AUTO_VIDEO = new AutoProfile("stream", "Video", "Video",
            180, 25_000_000L, 60, 0, 6L * 60L * 60L * 1000L);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService preflightExecutor = Executors.newFixedThreadPool(8);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Server> allServers = new ArrayList<>();
    private final List<Server> servers = new ArrayList<>();
    private final List<CountryOption> countryOptions = new ArrayList<>();
    private final List<Server> preferredConnectCandidates = new ArrayList<>();
    private final List<Server> attemptedConnectCandidates = new ArrayList<>();
    private LinearLayout table;
    private LinearLayout countryBar;
    private LinearLayout detailSection;
    private LinearLayout recommendCard;
    private TextView statusTitle;
    private TextView status;
    private TextView footerInfo;
    private TextView recommendTitle;
    private TextView recommendMeta;
    private TextView recommendBadge;
    private TextView detail;
    private TextView log;
    private Button speedButton;
    private Button pingButton;
    private Button scoreButton;
    private Button guideButton;
    private Button startButton;
    private Button connectButton;
    private Button detailToggleButton;
    private Button disconnectButton;
    private Button privacyButton;
    private int firstCountryFocusId = View.NO_ID;
    private final AutoProfile autoProfile = AUTO_VIDEO;
    private boolean detailVisible;
    private String selectedCountryShort = "";
    private String selectedCountryLong = "";
    private String sort = "speed";
    private boolean guideFilter = false;
    private Server selectedServer;
    private boolean vpnStatusReceiverRegistered;
    private boolean connecting;
    private boolean vpnConnected;
    private boolean tvMode;
    private boolean serverLoading;
    private Server pendingConnectServer;
    private String connectedServerLabel = "";
    private String connectedCountryShort = "";
    private String connectedCountryLong = "";
    private boolean proUnlocked;
    private boolean billingReady;
    private boolean pendingOpenDetailsAfterPurchase;
    private String proPriceText = "";
    private BillingClient billingClient;
    private ProductDetails proProductDetails;
    private boolean pendingConnectAfterNotificationPermission;
    private VpnProfile pendingVpnProfile;
    private String pendingVpnStartReason;
    private long lastConnectedIpCheckAt;
    private IOpenVPNServiceInternal vpnServiceBinder;
    private final ServiceConnection vpnServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            vpnServiceBinder = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            vpnServiceBinder = null;
        }
    };
    private final Runnable connectTimeoutRunnable = () -> {
        if (!connecting) {
            return;
        }
        if (retryNextConnectCandidate("No connection confirmation within 35 seconds.")) {
            return;
        }
        connecting = false;
        vpnConnected = false;
        pendingConnectServer = null;
        clearConnectedServerState();
        preferredConnectCandidates.clear();
        attemptedConnectCandidates.clear();
        pendingVpnProfile = null;
        pendingVpnStartReason = null;
        updateConnectButtonState(true);
        setStatusCard("No response", "No connection confirmation within 35 seconds.", AMBER);
        logLine("No connection confirmation within 35 seconds. The server may be down - try another server.");
        stopVpnService("connect timeout");
    };
    private final BroadcastReceiver vpnStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !VPN_STATUS_ACTION.equals(intent.getAction())) {
                return;
            }
            String level = intent.getStringExtra("status");
            String state = intent.getStringExtra("detailstatus");
            String message = intent.getStringExtra("message");
            handleVpnStatus(state, level, message);
        }
    };
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        int responseCode = billingResult == null
                ? BillingClient.BillingResponseCode.ERROR
                : billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases, false, true);
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            pendingOpenDetailsAfterPurchase = false;
            logLine("Pro purchase canceled.");
        } else {
            pendingOpenDetailsAfterPurchase = false;
            String message = billingResult == null ? "Unknown billing error" : billingResult.getDebugMessage();
            logLine("Pro purchase failed: " + fallback(message));
            showErrorDialog(R.string.purchase_unavailable_title, billingUnavailableMessage(message));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        initializeBilling();
        registerVpnStatusReceiver();
        loadServers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent bindIntent = new Intent(this, OpenVPNService.class);
        bindIntent.setAction(OpenVPNService.START_SERVICE);
        bindService(bindIntent, vpnServiceConnection, Context.BIND_AUTO_CREATE);
        mainHandler.post(this::syncVpnStateFromSystem);
        mainHandler.postDelayed(this::syncVpnStateFromSystem, 1000);
        if (billingClient != null && billingReady) {
            queryExistingPurchases(false);
        }
    }

    @Override
    protected void onPause() {
        try {
            unbindService(vpnServiceConnection);
        } catch (Exception ignored) {
            // Not bound.
        }
        vpnServiceBinder = null;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        endBillingConnection();
        if (vpnStatusReceiverRegistered) {
            unregisterReceiver(vpnStatusReceiver);
            vpnStatusReceiverRegistered = false;
        }
        executor.shutdownNow();
        preflightExecutor.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        tvMode = isTvMode();
        setContentView(R.layout.activity_main);

        statusTitle = findViewById(R.id.statusTitle);
        status = findViewById(R.id.statusText);
        footerInfo = findViewById(R.id.footerInfo);
        countryBar = findViewById(R.id.countryBar);
        detailSection = findViewById(R.id.detailSection);
        table = findViewById(R.id.table);
        detail = findViewById(R.id.detail);
        log = findViewById(R.id.log);
        disableContainerFocus(findViewById(R.id.pageScroll));
        disableContainerFocus(findViewById(R.id.countryScroll));

        startButton = findViewById(R.id.startButton);
        connectButton = findViewById(R.id.connectButton);
        detailToggleButton = findViewById(R.id.detailToggleButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        privacyButton = findViewById(R.id.privacyButton);
        speedButton = findViewById(R.id.speedButton);
        pingButton = findViewById(R.id.pingButton);
        scoreButton = findViewById(R.id.scoreButton);
        guideButton = findViewById(R.id.guideButton);
        Button refreshButton = findViewById(R.id.refreshButton);
        Button ipButton = findViewById(R.id.ipButton);

        prepareButtons(startButton, connectButton, speedButton, pingButton, scoreButton, guideButton,
                detailToggleButton, disconnectButton, privacyButton, refreshButton, ipButton);
        applyFormFactorLayout();

        if (startButton != null) {
            startButton.setOnClickListener(v -> {
                if (vpnConnected) {
                    disconnectVpn();
                } else {
                    autoConnectSelectedProfile();
                }
            });
        }
        if (connectButton != null) {
            connectButton.setOnClickListener(v -> connectSelectedServerFromManual());
        }
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> loadServers());
        }
        if (ipButton != null) {
            ipButton.setOnClickListener(v -> checkPublicIp());
        }
        if (detailToggleButton != null) {
            detailToggleButton.setOnClickListener(v -> toggleDetailRequested());
        }
        if (disconnectButton != null) {
            disconnectButton.setOnClickListener(v -> disconnectVpn());
        }
        if (privacyButton != null) {
            privacyButton.setOnClickListener(v -> showInfoDialog());
        }

        speedButton.setOnClickListener(v -> {
            sort = "speed";
            applyCurrentFilters();
        });
        pingButton.setOnClickListener(v -> {
            sort = "ping";
            applyCurrentFilters();
        });
        scoreButton.setOnClickListener(v -> {
            sort = "score";
            applyCurrentFilters();
        });
        guideButton.setOnClickListener(v -> {
            guideFilter = !guideFilter;
            guideButton.setText(guideFilter ? R.string.action_quality_on : R.string.action_quality_off);
            applyCurrentFilters();
        });

        updateFooterInfo("checking…");
        updateToolbarColors();
        restoreProState();
        restoreConnectedServerState();
        updateConnectButtonState(true);
        updateProUi();
        wireFocusOrder(Collections.emptyList());
        mainHandler.postDelayed(() -> {
            if (tvMode && startButton != null) {
                startButton.requestFocusFromTouch();
                startButton.requestFocus();
            }
        }, 250);
    }

    private void disableContainerFocus(ViewGroup view) {
        if (view == null) {
            return;
        }
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
        view.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    private void prepareButtons(Button... buttons) {
        if (buttons == null) {
            return;
        }
        for (Button button : buttons) {
            if (button == null) {
                continue;
            }
            button.setAllCaps(false);
            button.setTextColor(TEXT);
            button.setIncludeFontPadding(false);
            applyTvFocusEffect(button);
        }
    }

    private int ensureViewId(View view) {
        if (view == null) {
            return View.NO_ID;
        }
        if (view.getId() == View.NO_ID) {
            view.setId(View.generateViewId());
        }
        return view.getId();
    }

    private void wireFocusOrder(List<Button> countryButtons) {
        int startId = ensureViewId(startButton);
        int firstCountryId = countryButtons == null || countryButtons.isEmpty()
                ? View.NO_ID
                : ensureViewId(countryButtons.get(0));
        firstCountryFocusId = firstCountryId;

        if (startButton != null && firstCountryId != View.NO_ID) {
            startButton.setNextFocusDownId(firstCountryId);
        }
        int detailId = ensureViewId(detailToggleButton);
        if (countryButtons != null) {
            for (Button countryButton : countryButtons) {
                if (countryButton == null) {
                    continue;
                }
                ensureViewId(countryButton);
                if (startId != View.NO_ID) {
                    countryButton.setNextFocusUpId(startId);
                }
                if (detailId != View.NO_ID) {
                    countryButton.setNextFocusDownId(detailId);
                }
            }
        }
        wireHomeActionFocus();
    }

    private void wireHomeActionFocus() {
        int upId = firstCountryFocusId != View.NO_ID ? firstCountryFocusId : ensureViewId(startButton);
        int detailId = ensureViewId(detailToggleButton);
        int disconnectId = ensureViewId(disconnectButton);
        int privacyId = ensureViewId(privacyButton);
        boolean showDisconnect = disconnectButton != null && disconnectButton.getVisibility() == View.VISIBLE;

        if (detailToggleButton != null) {
            if (upId != View.NO_ID) {
                detailToggleButton.setNextFocusUpId(upId);
            }
            detailToggleButton.setNextFocusLeftId(detailId);
            detailToggleButton.setNextFocusRightId(showDisconnect && disconnectId != View.NO_ID ? disconnectId : privacyId);
            detailToggleButton.setNextFocusDownId(detailId);
        }
        if (disconnectButton != null) {
            if (upId != View.NO_ID) {
                disconnectButton.setNextFocusUpId(upId);
            }
            disconnectButton.setNextFocusLeftId(detailId);
            disconnectButton.setNextFocusRightId(privacyId);
            disconnectButton.setNextFocusDownId(disconnectId);
        }
        if (privacyButton != null) {
            if (upId != View.NO_ID) {
                privacyButton.setNextFocusUpId(upId);
            }
            privacyButton.setNextFocusLeftId(showDisconnect && disconnectId != View.NO_ID ? disconnectId : detailId);
            privacyButton.setNextFocusRightId(privacyId);
            privacyButton.setNextFocusDownId(privacyId);
        }
    }

    private void initializeBilling() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build())
                .enableAutoServiceReconnection()
                .build();
        startBillingConnection();
    }

    private void startBillingConnection() {
        BillingClient client = billingClient;
        if (client == null || client.isReady()) {
            return;
        }
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                mainHandler.post(() -> {
                    billingReady = billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
                    if (billingReady) {
                        logLine("Google Play Billing ready.");
                        queryProProductDetails();
                        queryExistingPurchases(false);
                    } else {
                        String message = billingResult.getDebugMessage();
                        logLine("Google Play Billing unavailable: " + fallback(message));
                        updateProUi();
                    }
                });
            }

            @Override
            public void onBillingServiceDisconnected() {
                mainHandler.post(() -> {
                    billingReady = false;
                    logLine("Google Play Billing disconnected.");
                    updateProUi();
                });
            }
        });
    }

    private void endBillingConnection() {
        BillingClient client = billingClient;
        if (client != null) {
            client.endConnection();
            billingClient = null;
        }
        billingReady = false;
    }

    private void queryProProductDetails() {
        BillingClient client = billingClient;
        if (client == null || !client.isReady()) {
            return;
        }
        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();
        client.queryProductDetailsAsync(params, (billingResult, queryResult) -> mainHandler.post(() -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                List<ProductDetails> details = queryResult == null
                        ? Collections.emptyList()
                        : queryResult.getProductDetailsList();
                if (!details.isEmpty()) {
                    proProductDetails = details.get(0);
                    proPriceText = proPriceText(proProductDetails);
                    logLine("Pro product loaded: " + PRO_PRODUCT_ID + " " + fallback(proPriceText));
                } else {
                    proProductDetails = null;
                    proPriceText = "";
                    logLine("Pro product not found in Google Play: " + PRO_PRODUCT_ID);
                }
            } else {
                proProductDetails = null;
                proPriceText = "";
                logLine("Pro product query failed: " + fallback(billingResult.getDebugMessage()));
            }
            updateProUi();
        }));
    }

    private void queryExistingPurchases(boolean notifyUser) {
        BillingClient client = billingClient;
        if (client == null || !client.isReady()) {
            return;
        }
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        client.queryPurchasesAsync(params, (billingResult, purchases) -> mainHandler.post(() -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases, true, notifyUser);
            } else {
                logLine("Pro purchase restore failed: " + fallback(billingResult.getDebugMessage()));
                if (notifyUser) {
                    showErrorDialog(R.string.restore_unavailable_title, R.string.restore_check_failed);
                }
            }
        }));
    }

    private void processPurchases(List<Purchase> purchases, boolean fromRestore, boolean notifyUser) {
        boolean foundProPurchase = false;
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (!purchase.getProducts().contains(PRO_PRODUCT_ID)) {
                    continue;
                }
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    foundProPurchase = true;
                    acknowledgeProPurchaseIfNeeded(purchase);
                } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                    logLine("Pro purchase is pending.");
                    if (notifyUser) {
                        showErrorDialog(R.string.purchase_pending_title, R.string.purchase_pending_message);
                    }
                }
            }
        }
        if (foundProPurchase) {
            setProUnlocked(true);
            if (!fromRestore) {
                logLine("Pro server selection unlocked.");
            }
            if (notifyUser && fromRestore) {
                showErrorDialog(R.string.pro_restored_title, R.string.pro_restored_message);
            }
        } else if (fromRestore) {
            setProUnlocked(false);
            if (notifyUser) {
                showErrorDialog(R.string.no_purchase_title, R.string.no_purchase_message);
            }
        }
    }

    private void acknowledgeProPurchaseIfNeeded(Purchase purchase) {
        BillingClient client = billingClient;
        if (client == null || !client.isReady() || purchase.isAcknowledged()) {
            return;
        }
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        client.acknowledgePurchase(params, billingResult -> mainHandler.post(() -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                logLine("Pro purchase acknowledged.");
            } else {
                logLine("Pro purchase acknowledgement failed: " + fallback(billingResult.getDebugMessage()));
            }
        }));
    }

    private void launchProPurchase() {
        if (proUnlocked) {
            openProDetailsAfterPurchase();
            return;
        }
        BillingClient client = billingClient;
        if (client == null) {
            initializeBilling();
            showErrorDialog(R.string.purchase_unavailable_title, billingUnavailableMessage("Google Play Billing is starting."));
            return;
        }
        if (!client.isReady()) {
            startBillingConnection();
            showErrorDialog(R.string.purchase_unavailable_title, billingUnavailableMessage("Google Play Billing is not ready yet."));
            return;
        }
        if (proProductDetails == null) {
            queryProProductDetails();
            logLine("Pro product not ready. Create and activate Google Play one-time product: " + PRO_PRODUCT_ID);
            showErrorDialog(R.string.purchases_unavailable_title, R.string.product_inactive_message);
            return;
        }
        BillingFlowParams.ProductDetailsParams.Builder productParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(proProductDetails);
        String offerToken = proOfferToken(proProductDetails);
        if (!offerToken.isEmpty()) {
            productParams.setOfferToken(offerToken);
        }
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productParams.build()))
                .build();
        BillingResult result = client.launchBillingFlow(this, params);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            pendingOpenDetailsAfterPurchase = false;
            showErrorDialog(R.string.purchase_unavailable_title, billingUnavailableMessage(result.getDebugMessage()));
            logLine("Could not launch Pro purchase: " + fallback(result.getDebugMessage()));
        }
    }

    private void restoreProPurchase() {
        if (billingClient == null) {
            initializeBilling();
            showErrorDialog(R.string.restore_unavailable_title, R.string.billing_connecting_message);
            return;
        } else if (!billingClient.isReady()) {
            startBillingConnection();
            showErrorDialog(R.string.restore_unavailable_title, R.string.billing_connecting_message);
            return;
        }
        queryExistingPurchases(true);
        logLine("Checking Pro purchase status.");
    }

    private void setProUnlocked(boolean unlocked) {
        proUnlocked = unlocked;
        SharedPreferences.Editor editor = appPreferences().edit();
        if (unlocked) {
            editor.putBoolean(PREF_PRO_UNLOCKED, true);
        } else {
            editor.remove(PREF_PRO_UNLOCKED);
        }
        editor.apply();
        if (!unlocked && detailVisible) {
            setDetailVisible(false);
        }
        updateProUi();
        if (unlocked && pendingOpenDetailsAfterPurchase) {
            openProDetailsAfterPurchase();
        }
    }

    private void restoreProState() {
        proUnlocked = appPreferences().getBoolean(PREF_PRO_UNLOCKED, false);
    }

    private void updateProUi() {
        if (detailToggleButton != null) {
            if (detailVisible) {
                detailToggleButton.setText(R.string.action_close_details);
            } else {
                detailToggleButton.setText(proUnlocked
                        ? R.string.action_choose_server
                        : R.string.action_unlock_server);
            }
        }
    }

    private boolean requireProSelection() {
        if (proUnlocked) {
            return true;
        }
        showProPaywall();
        return false;
    }

    private void showProPaywall() {
        pendingOpenDetailsAfterPurchase = true;
        String price = proPriceText.isEmpty()
                ? getString(R.string.pro_price_pending)
                : getString(R.string.pro_price_one_time, proPriceText);
        new AlertDialog.Builder(this)
                .setTitle(R.string.pro_title)
                .setMessage(getString(R.string.pro_message, price))
                .setPositiveButton(R.string.pro_buy, (dialog, which) -> launchProPurchase())
                .setNegativeButton(R.string.pro_restore, (dialog, which) -> restoreProPurchase())
                .setNeutralButton(R.string.not_now, (dialog, which) -> pendingOpenDetailsAfterPurchase = false)
                .show();
    }

    private void openProDetailsAfterPurchase() {
        pendingOpenDetailsAfterPurchase = false;
        if (!detailVisible) {
            setDetailVisible(true);
        }
    }

    private String proPriceText(ProductDetails productDetails) {
        ProductDetails.OneTimePurchaseOfferDetails offer = firstProOffer(productDetails);
        return offer == null ? "" : cleanPrefValue(offer.getFormattedPrice());
    }

    private String proOfferToken(ProductDetails productDetails) {
        ProductDetails.OneTimePurchaseOfferDetails offer = firstProOffer(productDetails);
        return offer == null ? "" : cleanPrefValue(offer.getOfferToken());
    }

    private ProductDetails.OneTimePurchaseOfferDetails firstProOffer(ProductDetails productDetails) {
        if (productDetails == null || productDetails.getOneTimePurchaseOfferDetailsList() == null
                || productDetails.getOneTimePurchaseOfferDetailsList().isEmpty()) {
            return null;
        }
        return productDetails.getOneTimePurchaseOfferDetailsList().get(0);
    }

    private String billingUnavailableMessage(String debugMessage) {
        String message = getString(R.string.billing_unavailable_message);
        String debug = cleanPrefValue(debugMessage);
        if (!debug.isEmpty() && !"-".equals(debug)) {
            logLine("Billing unavailable detail: " + debug);
        }
        return message;
    }

    private void loadServers() {
        if (serverLoading) {
            logLine("Server directory is already loading.");
            return;
        }
        serverLoading = true;
        setStatusCard("Loading servers", "Checking available countries.", BLUE);
        logLine("Checking server directory...");
        updateToolbarColors();
        executor.execute(() -> {
            try {
                DownloadResult download = downloadApi();
                List<Server> parsed = parseServers(download.body);
                mainHandler.post(() -> {
                    serverLoading = false;
                    allServers.clear();
                    allServers.addAll(parsed);
                    rebuildCountryOptions(parsed);
                    chooseDefaultCountryIfNeeded();
                    renderCountryButtons();
                    applyCurrentFilters();
                    updateFooterInfo(download.fromCache
                            ? "cache " + cacheAgeText(download.cacheAgeMs)
                            : formatClock(System.currentTimeMillis()));
                    if (download.fromCache) {
                        setStatusCard(cacheStatusTitle(download.cacheAgeMs), cacheStatusSubtitle(download.cacheAgeMs), AMBER);
                        logLine("Using cached server list: " + parsed.size() + " servers / " + cacheAgeText(download.cacheAgeMs));
                    } else {
                        logLine("Server directory OK: " + parsed.size() + " servers / " + download.source);
                    }
                });
            } catch (Exception e) {
                debugLog("Server loading failed", e);
                mainHandler.post(() -> {
                    serverLoading = false;
                    setStatusCard("Load failed", "Could not load the server list.", RED);
                    updateFooterInfo("failed");
                    logLine("Server list load failed: " + exceptionText(e));
                    detail.setText(getString(R.string.server_load_failed_detail, exceptionText(e)));
                });
            }
        });
    }

    private DownloadResult downloadApi() throws Exception {
        Exception lastError = null;
        for (String url : API_URLS) {
            for (int attempt = 1; attempt <= API_ATTEMPTS_PER_URL; attempt++) {
                try {
                    String body = downloadText(url, API_CONNECT_TIMEOUT_MS, API_READ_TIMEOUT_MS,
                            MAX_SERVER_DIRECTORY_BYTES, null);
                    ensureLooksLikeServerCsv(body, url);
                    writeServerCache(body);
                    return new DownloadResult(body, url, false, 0);
                } catch (Exception e) {
                    if (lastError != null && lastError != e) {
                        e.addSuppressed(lastError);
                    }
                    lastError = e;
                    debugLog("Server directory failed: " + url + " attempt " + attempt, e);
                    postLogLine("API failed " + shortUrl(url) + " (" + attempt + "/" + API_ATTEMPTS_PER_URL + "): " + exceptionText(e));
                    if (!isLastApiAttempt(url, attempt)) {
                        sleepBeforeRetry(attempt);
                    }
                }
            }
        }
        if (hasServerCache()) {
            long ageMs = serverCacheAgeMs();
            postLogLine("All API attempts failed, using last successful list: " + cacheAgeText(ageMs));
            return new DownloadResult(readServerCache(), "cache", true, ageMs);
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new IllegalStateException("No server directory URL.");
    }

    private boolean isLastApiAttempt(String url, int attempt) {
        return API_URLS[API_URLS.length - 1].equals(url) && attempt >= API_ATTEMPTS_PER_URL;
    }

    private void sleepBeforeRetry(int attempt) throws InterruptedException {
        long delay = API_RETRY_BASE_DELAY_MS * (1L << Math.min(attempt - 1, 2));
        Thread.sleep(delay);
    }

    private void ensureLooksLikeServerCsv(String body, String url) {
        if (body == null
                || !body.contains("HostName")
                || !body.contains("OpenVPN_ConfigData_Base64")) {
            throw new IllegalStateException("Server CSV marker missing from " + url);
        }
    }

    private void writeServerCache(String body) {
        try (FileOutputStream output = new FileOutputStream(serverCacheFile())) {
            output.write(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            debugLog("Failed to write server cache", e);
            postLogLine("Failed to save server cache: " + exceptionText(e));
        }
    }

    private String readServerCache() throws Exception {
        File file = serverCacheFile();
        if (file.length() > MAX_SERVER_DIRECTORY_BYTES) {
            throw new IllegalStateException("Cached server directory is too large.");
        }
        try (InputStream input = new FileInputStream(file)) {
            return readUtf8Limited(input, MAX_SERVER_DIRECTORY_BYTES, "server cache");
        }
    }

    private boolean hasServerCache() {
        File file = serverCacheFile();
        return file.exists() && file.length() > 0 && file.length() <= MAX_SERVER_DIRECTORY_BYTES;
    }

    private long serverCacheAgeMs() {
        File file = serverCacheFile();
        long modified = file.lastModified();
        if (modified <= 0) {
            return Long.MAX_VALUE;
        }
        return Math.max(0, System.currentTimeMillis() - modified);
    }

    private File serverCacheFile() {
        return new File(getFilesDir(), SERVER_CACHE_FILE);
    }

    private String downloadText(String url, int connectTimeoutMs, int readTimeoutMs,
                                int maxBytes, Network network) throws Exception {
        URL target = new URL(url);
        if (!"https".equalsIgnoreCase(target.getProtocol())) {
            throw new IllegalArgumentException("Only HTTPS downloads are allowed.");
        }
        HttpURLConnection connection = (HttpURLConnection) (network == null
                ? target.openConnection()
                : network.openConnection(target));
        try {
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 NorenVPN/1.0");
            connection.setRequestProperty("Accept", "text/csv,text/plain,*/*");
            connection.setRequestProperty("Accept-Encoding", "identity");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code + " from " + url);
            }
            long contentLength = connection.getContentLength();
            if (contentLength > maxBytes) {
                throw new IllegalStateException("Response is too large from " + url);
            }
            try (InputStream input = connection.getInputStream()) {
                return readUtf8Limited(input, maxBytes, url);
            }
        } finally {
            connection.disconnect();
        }
    }

    private String readUtf8Limited(InputStream input, int maxBytes, String source) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 32 * 1024));
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IllegalStateException("Response is too large from " + source);
            }
            output.write(buffer, 0, read);
        }
        return decodeUtf8Strict(output.toByteArray(), source);
    }

    private String decodeUtf8Strict(byte[] bytes, String source) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Invalid UTF-8 from " + source, e);
        }
    }

    private void checkPublicIp() {
        setIpCheckStatus("Checking public IP", BLUE);
        logLine("Checking public IP...");
        executor.execute(() -> {
            try {
                Network vpnNetwork = vpnConnected ? findAppVpnNetwork() : null;
                String body = downloadPublicIpJson(vpnNetwork);
                String ip = jsonValue(body, "ip");
                String countryName = jsonValue(body, "country");
                String countryIso = jsonValue(body, "country_iso");
                String asn = jsonValue(body, "asn");
                String org = jsonValue(body, "asn_org");
                if (countryIso.isEmpty() && countryName.length() == 2) {
                    countryIso = countryName;
                    countryName = "";
                }
                if (org.isEmpty()) {
                    org = jsonValue(body, "org");
                }
                String result = "Public IP: " + fallback(ip)
                        + " / Country: " + fallback(countryName)
                        + " (" + fallback(countryIso) + ")"
                        + " / ASN: " + fallback(asn)
                        + " " + fallback(org);
                if (vpnConnected) {
                    result += vpnNetwork == null ? " / VPN network check unavailable" : " / checked over VPN network";
                }
                String resultText = result;
                mainHandler.post(() -> {
                    setIpCheckStatus("IP check done", vpnConnected ? GREEN : MUTED);
                    logLine(resultText);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setIpCheckStatus("IP check failed", vpnConnected ? GREEN : Color.rgb(248, 113, 113));
                    logLine("IP check failed: " + e.getMessage());
                });
            }
        });
    }

    private String downloadPublicIpJson(Network network) throws Exception {
        Exception lastError = null;
        for (String url : PUBLIC_IP_CHECK_URLS) {
            try {
                String body = downloadText(url, 10000, 15000,
                        MAX_PUBLIC_IP_RESPONSE_BYTES, network);
                if (!jsonValue(body, "ip").isEmpty()) {
                    return body;
                }
                throw new IllegalStateException("Public IP response missing ip field from " + shortUrl(url));
            } catch (Exception e) {
                lastError = e;
                postLogLine("IP check endpoint failed " + shortUrl(url) + ": " + exceptionText(e));
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new IllegalStateException("No public IP check endpoint configured.");
    }

    private Network findAppVpnNetwork() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return null;
        }
        try {
            for (Network network : manager.getAllNetworks()) {
                NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    continue;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (capabilities.getOwnerUid() == getApplicationInfo().uid) {
                        return network;
                    }
                } else if (hasSavedConnectedServerState()) {
                    return network;
                }
            }
        } catch (Exception e) {
            debugLog("Failed to find VPN network for IP check", e);
        }
        return null;
    }

    private void setIpCheckStatus(String suffix, int color) {
        setStatusCard(vpnConnected ? "Connected" : "Disconnected",
                vpnConnected ? "Connected via VPN - " + connectedVpnText() + " / " + suffix : suffix,
                color);
    }

    private List<Server> parseServers(String response) {
        String header = null;
        List<String> data = new ArrayList<>();
        for (String raw : response.split("\\R")) {
            if (raw.length() > MAX_CSV_LINE_CHARS) {
                throw new IllegalStateException("Server CSV line is too large.");
            }
            String line = raw.trim();
            if (!line.isEmpty() && line.charAt(0) == '\ufeff') {
                line = line.substring(1).trim();
            }
            if (line.isEmpty()) {
                continue;
            }
            String maybeHeader = line.startsWith("#") ? line.substring(1) : line;
            if (header == null) {
                if (maybeHeader.contains("HostName")
                        && maybeHeader.contains("IP")
                        && maybeHeader.contains("OpenVPN_ConfigData_Base64")) {
                    header = maybeHeader;
                }
                continue;
            }
            if (line.startsWith("#") || line.startsWith("*")) {
                continue;
            }
            if (data.size() >= MAX_SERVER_ROWS) {
                throw new IllegalStateException("Server CSV has too many rows.");
            }
            data.add(line);
        }
        if (header == null) {
            throw new IllegalStateException("CSV header not found.");
        }
        List<String> columns = parseCsvLine(header);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            if (index.put(columns.get(i), i) != null) {
                throw new IllegalStateException("Duplicate CSV column: " + columns.get(i));
            }
        }
        String[] required = {"HostName", "IP", "CountryLong", "CountryShort", "OpenVPN_ConfigData_Base64"};
        for (String key : required) {
            if (!index.containsKey(key)) {
                throw new IllegalStateException("Missing required CSV column: " + key);
            }
        }

        List<Server> result = new ArrayList<>();
        int rejected = 0;
        for (String line : data) {
            try {
                List<String> cells = parseCsvLine(line);
                String config = cell(cells, index, "OpenVPN_ConfigData_Base64");
                if (config.isEmpty()) {
                    continue;
                }
                Server server = new Server();
                server.hostName = cell(cells, index, "HostName");
                server.ip = cell(cells, index, "IP");
                server.countryLong = cell(cells, index, "CountryLong");
                server.countryShort = cell(cells, index, "CountryShort");
                validateServerMetadata(server);
                server.ping = parseDouble(cell(cells, index, "Ping"), -1);
                server.speed = parseLong(cell(cells, index, "Speed"), 0);
                server.sessions = (int) Math.min(Integer.MAX_VALUE,
                        Math.max(0, parseLong(cell(cells, index, "NumVpnSessions"), 0)));
                server.uptime = parseLong(cell(cells, index, "Uptime"), 0);
                server.score = parseLong(cell(cells, index, "Score"), 0);
                server.configBase64 = config;
                profileConfig(server);
                result.add(server);
            } catch (RuntimeException e) {
                rejected++;
                debugLog("Rejected unsafe server directory row", e);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("Server directory contains no safe VPN profiles.");
        }
        if (rejected > 0) {
            postLogLine("Rejected " + rejected + " invalid server directory rows.");
        }
        return result;
    }

    private void validateServerMetadata(Server server) {
        if (server.hostName.isEmpty() || server.hostName.length() > 63
                || !server.hostName.matches("[A-Za-z0-9][A-Za-z0-9-]*")) {
            throw new IllegalArgumentException("Invalid VPN Gate host name.");
        }
        if (!VpnGateConfigValidator.isIpv4(server.ip)) {
            throw new IllegalArgumentException("Invalid VPN Gate IPv4 address.");
        }
        if (server.countryLong.isEmpty() || server.countryLong.length() > 128
                || !server.countryShort.matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("Invalid VPN Gate country metadata.");
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                if (cells.size() >= MAX_CSV_COLUMNS - 1) {
                    throw new IllegalStateException("Server CSV has too many columns.");
                }
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (quoted) {
            throw new IllegalStateException("Server CSV has an unterminated quoted field.");
        }
        cells.add(current.toString());
        return cells;
    }

    private String cell(List<String> cells, Map<String, Integer> index, String key) {
        Integer i = index.get(key);
        if (i == null || i < 0 || i >= cells.size()) {
            return "";
        }
        return cells.get(i).trim();
    }

    private void applyCurrentFilters() {
        List<Server> filtered = filterCountry(allServers);
        int beforeGuide = filtered.size();
        if (guideFilter) {
            List<Server> guide = new ArrayList<>();
            for (Server server : filtered) {
                if (guideReason(server) == null) {
                    guide.add(server);
                }
            }
            filtered = guide;
        }
        sortServers(filtered);
        int matchingCount = filtered.size();
        if (matchingCount > MAX_DISPLAYED_SERVERS) {
            filtered = new ArrayList<>(filtered.subList(0, MAX_DISPLAYED_SERVERS));
        }
        updateToolbarColors();
        renderCountryButtons();
        showServers(filtered, matchingCount, beforeGuide);
    }

    private List<Server> filterCountry(List<Server> all) {
        List<Server> out = new ArrayList<>();
        for (Server server : all) {
            if (selectedCountryShort.equalsIgnoreCase(server.countryShort)
                    || selectedCountryLong.equalsIgnoreCase(server.countryLong)) {
                out.add(server);
            }
        }
        return out;
    }

    private void rebuildCountryOptions(List<Server> rows) {
        Map<String, CountryOption> byCountry = new HashMap<>();
        for (Server server : rows) {
            String code = server.countryShort == null ? "" : server.countryShort.trim().toUpperCase(Locale.ROOT);
            String name = server.countryLong == null ? "" : server.countryLong.trim();
            if (code.isEmpty() && name.isEmpty()) {
                continue;
            }
            String key = code.isEmpty() ? name.toLowerCase(Locale.ROOT) : code;
            CountryOption option = byCountry.get(key);
            if (option == null) {
                option = new CountryOption();
                option.shortName = code;
                option.longName = name.isEmpty() ? code : name;
                byCountry.put(key, option);
            }
            option.count++;
        }
        countryOptions.clear();
        countryOptions.addAll(byCountry.values());
        Collections.sort(countryOptions, (left, right) -> {
            int byCount = Integer.compare(right.count, left.count);
            if (byCount != 0) {
                return byCount;
            }
            return left.label().toLowerCase(Locale.ROOT)
                    .compareTo(right.label().toLowerCase(Locale.ROOT));
        });
    }

    private void chooseDefaultCountryIfNeeded() {
        if (countryOptions.isEmpty()) {
            selectedCountryShort = "";
            selectedCountryLong = "";
            return;
        }
        for (CountryOption option : countryOptions) {
            if (option.matches(selectedCountryShort, selectedCountryLong)) {
                return;
            }
        }
        selectCountry(countryOptions.get(0));
    }

    private void renderCountryButtons() {
        if (countryBar == null) {
            return;
        }
        countryBar.removeAllViews();
        if (countryOptions.isEmpty()) {
            TextView empty = text("No countries available", 20, MUTED);
            countryBar.addView(empty);
            wireFocusOrder(Collections.emptyList());
            return;
        }
        List<Button> renderedButtons = new ArrayList<>();
        for (CountryOption option : countryOptions) {
            Button button = button(countryCardLabel(option));
            boolean selected = option.matches(selectedCountryShort, selectedCountryLong);
            button.setTextColor(selected ? Color.WHITE : TEXT);
            button.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            button.setBackgroundResource(selected ? R.drawable.tv_focus_card_selected : R.drawable.tv_focus_card);
            button.setOnClickListener(v -> {
                selectCountry(option);
                applyCurrentFilters();
            });
            countryBar.addView(button);
            renderedButtons.add(button);
        }
        wireFocusOrder(renderedButtons);
    }

    private void selectCountry(CountryOption option) {
        selectedCountryShort = option.shortName;
        selectedCountryLong = option.longName;
    }

    private void sortServers(List<Server> rows) {
        if ("ping".equals(sort)) {
            Collections.sort(rows, (left, right) -> {
                int byPing = comparePingAsc(left, right);
                if (byPing != 0) {
                    return byPing;
                }
                return compareSpeedDesc(left, right);
            });
        } else if ("score".equals(sort)) {
            Collections.sort(rows, (left, right) -> {
                int byScore = compareScoreDesc(left, right);
                if (byScore != 0) {
                    return byScore;
                }
                int bySpeed = compareSpeedDesc(left, right);
                if (bySpeed != 0) {
                    return bySpeed;
                }
                return comparePingAsc(left, right);
            });
        } else {
            Collections.sort(rows, (left, right) -> {
                int bySpeed = compareSpeedDesc(left, right);
                if (bySpeed != 0) {
                    return bySpeed;
                }
                return comparePingAsc(left, right);
            });
        }
    }

    private static int compareScoreDesc(Server left, Server right) {
        return Long.compare(right.score, left.score);
    }

    private static int compareSpeedDesc(Server left, Server right) {
        return Long.compare(right.speed, left.speed);
    }

    private static int comparePingAsc(Server left, Server right) {
        return Double.compare(sortablePing(left), sortablePing(right));
    }

    private static double sortablePing(Server server) {
        return server.ping < 0 ? Double.MAX_VALUE : server.ping;
    }

    private void showServers(List<Server> rows, int matchingCount, int beforeGuide) {
        servers.clear();
        servers.addAll(rows);
        List<Server> ranked = autoRankedServers(autoProfile);
        selectedServer = rows.isEmpty() ? null : (ranked.isEmpty() ? rows.get(0) : ranked.get(0));
        table.removeAllViews();
        table.addView(rowView(null, true, 0));
        for (int i = 0; i < rows.size(); i++) {
            table.addView(rowView(rows.get(i), false, i + 1));
        }
        setListStatus(rows.size(), matchingCount, beforeGuide);
        if (rows.isEmpty()) {
            updateRecommendationCard();
            detail.setText(getString(R.string.detail_with_current,
                    connectedDetailText(), getString(R.string.selected_server_none)));
            logLine("No servers for " + countryLabel());
        } else {
            updateDetail();
            logLine("Server list updated: showing " + rows.size() + " of " + matchingCount + " servers");
        }
    }

    private View rowView(Server server, boolean header, int rank) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(!header);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        if (header) {
            row.setBackgroundColor(PANEL);
        } else {
            row.setBackgroundResource(R.drawable.tv_focus_card);
        }
        if (!header) {
            row.setOnFocusChangeListener((v, hasFocus) -> {
                applyFocusTransform(v, hasFocus);
            });
            row.setOnClickListener(v -> {
                selectedServer = server;
                updateDetail();
                if (!requireProSelection()) {
                    return;
                }
                showServerActionDialog(server);
            });
        }
        row.addView(cellText(header ? getString(R.string.table_rank) : String.valueOf(rank), 64, header));
        row.addView(cellText(header ? getString(R.string.table_host) : server.hostName, 220, header));
        row.addView(cellText(header ? getString(R.string.table_ip) : server.ip, 150, header));
        row.addView(cellText(header ? getString(R.string.table_ping) : server.pingText(), 80, header));
        row.addView(cellText(header ? getString(R.string.table_speed) : server.speedText(), 130, header));
        row.addView(cellText(header ? getString(R.string.table_sessions) : String.valueOf(server.sessions), 110, header));
        row.addView(cellText(header ? getString(R.string.table_uptime) : uptimeText(server.uptime), 140, header));
        row.addView(cellText(header ? getString(R.string.table_filter)
                : (guideReason(server) == null ? getString(R.string.filter_ok) : guideReason(server)), 190, header));
        return row;
    }

    private TextView cellText(String value, int widthDp, boolean header) {
        TextView textView = text(value, header ? 14 : 15, header ? Color.WHITE : TEXT);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setSingleLine(true);
        textView.setPadding(dp(6), 0, dp(6), 0);
        textView.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), dp(34)));
        return textView;
    }

    private void updateDetail() {
        updateRecommendationCard();
        if (selectedServer == null) {
            if (detail != null) {
                detail.setText(R.string.selected_server_none);
            }
            return;
        }
        String autoReason = autoRejectReason(selectedServer, autoProfile);
        if (detail != null) {
            String filterReason = guideReason(selectedServer);
            detail.setText(getString(R.string.detail_with_current, connectedDetailText(),
                    getString(R.string.selected_server_detail,
                    selectedServer.hostName,
                    selectedServer.ip,
                    selectedServer.pingText(),
                    selectedServer.speedText(),
                    selectedServer.sessions,
                    uptimeText(selectedServer.uptime),
                    filterReason == null ? getString(R.string.filter_ok) : filterReason,
                    autoReason == null ? getString(R.string.filter_ok) : autoReason)));
        }
    }

    private void updateRecommendationCard() {
        if (recommendTitle == null || recommendMeta == null || recommendBadge == null) {
            return;
        }
        if (selectedServer == null) {
            recommendTitle.setText(R.string.no_recommended_server);
            recommendMeta.setText(R.string.try_another_country);
            recommendBadge.setText(R.string.waiting);
            recommendBadge.setTextColor(MUTED);
            recommendBadge.setBackground(roundedBg(PANEL_LIGHT, Color.TRANSPARENT, 28));
            if (recommendCard != null) {
                recommendCard.setBackground(roundedBg(PANEL, BORDER, 22));
            }
            return;
        }

        String autoReason = autoRejectReason(selectedServer, autoProfile);
        boolean recommended = autoReason == null;
        recommendTitle.setText(getString(R.string.recommend_title,
                serverCountryDisplayName(selectedServer), selectedServer.hostName));
        recommendMeta.setText(getString(R.string.recommend_meta,
                selectedServer.pingText(),
                selectedServer.speedText(),
                selectedServer.sessions,
                uptimeText(selectedServer.uptime)));
        recommendBadge.setText(recommended ? getString(R.string.recommended) : autoReason);
        recommendBadge.setTextColor(recommended ? GREEN : AMBER);
        recommendBadge.setBackground(roundedBg(recommended ? Color.rgb(220, 252, 231) : Color.rgb(255, 247, 237),
                Color.TRANSPARENT, 28));
        if (recommendCard != null) {
            recommendCard.setBackground(roundedBg(recommended ? PALE_GREEN : PANEL, recommended ? Color.rgb(187, 247, 208) : BORDER, 22));
        }
    }

    private void exportSelectedProfile() {
        if (!requireProSelection()) {
            return;
        }
        if (selectedServer == null) {
            logLine("Select a server first.");
            showErrorDialog(R.string.server_required_title, R.string.server_required_message);
            return;
        }
        try {
            File file = writeProfileFile();
            boolean savedToDownloads = saveProfileToDownloads(file);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
            setStatusCard("Profile saved", file.getName(), GREEN);
            logLine("Saved: " + (savedToDownloads ? "Downloads/NorenVPN/" : "app temp storage/") + file.getName());
            showProfileReadyDialog(uri, file.getName(), savedToDownloads);
        } catch (Exception e) {
            setStatusCard("Profile save failed", fallback(e.getMessage()), RED);
            logLine("OVPN save failed: " + e.getMessage());
            showErrorDialog(R.string.profile_save_failed_title, e.getMessage());
        }
    }

    private void showServerActionDialog(Server server) {
        if (!requireProSelection()) {
            return;
        }
        String autoReason = autoRejectReason(server, autoProfile);
        String message = getString(R.string.server_actions_message,
                server.hostName,
                server.ip,
                server.pingText(),
                server.speedText(),
                uptimeText(server.uptime),
                autoReason == null ? getString(R.string.filter_ok) : autoReason);
        new AlertDialog.Builder(this)
                .setTitle(R.string.server_actions_title)
                .setMessage(message)
                .setPositiveButton(R.string.action_connect, (dialog, which) -> {
                    selectedServer = server;
                    updateDetail();
                    connectSelectedServer();
                })
                .setNegativeButton(R.string.action_save_profile, (dialog, which) -> {
                    selectedServer = server;
                    updateDetail();
                    exportSelectedProfile();
                })
                .setNeutralButton(R.string.action_close, null)
                .show();
    }

    private void connectSelectedServer() {
        connectSelectedServer(null);
    }

    private void connectSelectedServerFromManual() {
        if (!requireProSelection()) {
            return;
        }
        connectSelectedServer();
    }

    private void connectSelectedServer(List<Server> preferredCandidates) {
        if (connecting) {
            logLine("Already connecting. Wait for the permission screen or the status log.");
            return;
        }
        if (selectedServer == null) {
            logLine("Select a server first.");
            showErrorDialog(R.string.server_required_title, R.string.server_required_message);
            return;
        }
        if (!ensureVpnDisclosureAccepted(() -> connectSelectedServer(preferredCandidates == null
                ? null
                : new ArrayList<>(preferredCandidates)))) {
            return;
        }
        preferredConnectCandidates.clear();
        attemptedConnectCandidates.clear();
        if (preferredCandidates != null) {
            preferredConnectCandidates.addAll(preferredCandidates);
        }
        if (requestNotificationPermissionIfNeeded()) {
            pendingConnectAfterNotificationPermission = true;
            logLine("Continuing with the VPN permission request after the notification permission check.");
            return;
        }
        beginConnectSelectedServer();
    }

    private void autoConnectSelectedProfile() {
        if (connecting) {
            logLine("Already connecting. Wait for the permission screen or the status log.");
            return;
        }
        if (serverLoading || allServers.isEmpty()) {
            if (!serverLoading) {
                loadServers();
            }
            setStatusCard("Loading servers", "Please wait for the server list.", BLUE);
            logLine("Server list is not ready yet.");
            showErrorDialog(R.string.loading_servers_title, R.string.loading_servers_message);
            return;
        }
        if (selectedCountryShort.isEmpty() && selectedCountryLong.isEmpty()) {
            chooseDefaultCountryIfNeeded();
            applyCurrentFilters();
        }
        List<Server> ranked = autoRankedServers(autoProfile);
        if (ranked.isEmpty()) {
            ranked = fallbackRankedServers();
            if (!ranked.isEmpty()) {
                logLine("No server met the quality criteria - using the fastest available server instead.");
            }
        } else if (ranked.size() > AUTO_PRIMARY_CANDIDATE_LIMIT) {
            ranked = new ArrayList<>(ranked.subList(0, AUTO_PRIMARY_CANDIDATE_LIMIT));
        }
        int candidateCountBeforeFallback = ranked.size();
        appendFallbackConnectCandidates(ranked);
        if (ranked.size() > candidateCountBeforeFallback) {
            logLine("Added fallback candidates for " + countryLabel()
                    + ": " + (ranked.size() - candidateCountBeforeFallback)
                    + " more server(s).");
        }
        if (ranked.isEmpty()) {
            String message = getString(R.string.no_server_message, countryLabel());
            setStatusCard("No server available", "No " + countryLabel() + " servers available.", RED);
            logLine("No auto-connect candidates: " + countryLabel());
            showErrorDialog(R.string.no_server_title, message);
            return;
        }
        selectedServer = ranked.get(0);
        updateDetail();
        logLine("Auto-selected: " + selectedServer.hostName
                + " / Ping " + selectedServer.pingText() + " ms"
                + " / Speed " + selectedServer.speedText() + " Mbps"
                + " / Uptime " + uptimeText(selectedServer.uptime));
        connectSelectedServer(ranked);
    }

    private boolean ensureVpnDisclosureAccepted(Runnable continueAction) {
        if (appPreferences().getBoolean(PREF_VPN_DISCLOSURE_ACCEPTED, false)) {
            return true;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.vpn_disclosure_title)
                .setMessage(R.string.vpn_disclosure_message)
                .setPositiveButton(R.string.agree_continue, (ignoredDialog, which) -> {
                    appPreferences().edit().putBoolean(PREF_VPN_DISCLOSURE_ACCEPTED, true).apply();
                    if (continueAction != null) {
                        continueAction.run();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.logging_policy, (ignoredDialog, which) -> openWebPage(VPN_GATE_LOGGING_POLICY_URL))
                .create();
        dialog.setOnShowListener(shownDialog -> {
            Button continueButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (continueButton != null) {
                continueButton.postDelayed(() -> {
                    continueButton.requestFocusFromTouch();
                    continueButton.requestFocus();
                }, 120);
            }
        });
        dialog.show();
        return false;
    }

    private void beginConnectSelectedServer() {
        if (selectedServer == null) {
            return;
        }
        Server requestedServer = selectedServer;
        connecting = true;
        updateConnectButtonState(false);
        mainHandler.removeCallbacks(connectTimeoutRunnable);
        setStatusCard("Checking server", "Verifying that the selected server responds.", BLUE);
        logLine("Checking server port before connecting...");
        executor.execute(() -> {
            try {
                ConnectTarget target = findReachableConnectTarget(requestedServer);
                mainHandler.post(() -> launchConnectTarget(target));
            } catch (Exception e) {
                mainHandler.post(() -> failConnect("Pre-connect check failed", e.getMessage(), true));
            }
        });
    }

    private void launchConnectTarget(ConnectTarget target) {
        if (!connecting) {
            return;
        }
        try {
            selectedServer = target.server;
            pendingConnectServer = target.server;
            rememberAttemptedConnectCandidate(target.server);
            rememberConnectedServer(target.server);
            updateDetail();
            VpnProfile profile = parseVpnProfile(target.config, target.server);
            Preferences.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean("showlogwindow", false)
                    .putBoolean(LaunchVPN.CLEARLOG, true)
                    .apply();
            ProfileManager.setTemporaryProfile(this, profile);

            String startReason = "Noren VPN selected server";
            setStatusCard("Checking permission", "Checking Android VPN permission.", BLUE);
            String endpointText = target.endpoint == null ? "endpoint unknown" : target.endpoint.label();
            logLine("Internal OpenVPN ready: " + profile.mName + " / " + endpointText);
            requestVpnPermissionOrStart(profile, startReason);
        } catch (Exception e) {
            failConnect("Connection setup failed", e.getMessage(), true);
        }
    }

    private void requestVpnPermissionOrStart(VpnProfile profile, String startReason) {
        int vpnOk = profile.checkProfile(this);
        if (vpnOk != R.string.no_error_found) {
            failConnect("Profile check failed", getString(vpnOk), true);
            return;
        }

        Intent permissionIntent = VpnService.prepare(this);
        if (permissionIntent != null) {
            pendingVpnProfile = profile;
            pendingVpnStartReason = startReason;
            mainHandler.removeCallbacks(connectTimeoutRunnable);
            setStatusCard("Checking permission", "Opening the Android VPN permission screen.", BLUE);
            logLine("Opening the Android VPN permission screen.");
            startActivityForResult(permissionIntent, REQUEST_VPN_PERMISSION);
            return;
        }

        startPreparedVpn(profile, startReason);
    }

    private void startPreparedVpn(VpnProfile profile, String startReason) {
        pendingVpnProfile = null;
        pendingVpnStartReason = null;
        try {
            ProfileManager.updateLRU(this, profile);
            VPNLaunchHelper.startOpenVpn(profile, getApplicationContext(), startReason, true);
            mainHandler.removeCallbacks(connectTimeoutRunnable);
            mainHandler.postDelayed(connectTimeoutRunnable, CONNECT_RETRY_LOCK_MS);
            setStatusCard("Connecting", "Starting the OpenVPN service.", BLUE);
            logLine("Starting OpenVPN service: " + profile.mName);
        } catch (Exception e) {
            failConnect("OpenVPN service start failed", e.getMessage(), true);
        }
    }

    private ConnectTarget findReachableConnectTarget(Server requestedServer) throws Exception {
        List<Server> candidates = new ArrayList<>();
        if (requestedServer != null && !hasAttemptedConnectCandidate(requestedServer)) {
            candidates.add(requestedServer);
        }
        List<Server> fallbackSource = preferredConnectCandidates.isEmpty() ? servers : preferredConnectCandidates;
        for (Server server : fallbackSource) {
            if (candidates.size() >= CONNECT_CANDIDATE_LIMIT) {
                break;
            }
            if (!hasAttemptedConnectCandidate(server)
                    && server != requestedServer
                    && !sameServer(server, requestedServer)) {
                candidates.add(server);
            }
        }

        Exception lastError = null;
        ConnectTarget deferredUdpTarget = null;
        List<ConnectTarget> tcpTargets = new ArrayList<>();
        boolean preferResponsiveTcp = !preferredConnectCandidates.isEmpty();
        for (Server server : candidates) {
            try {
                String config = profileConfig(server);
                Endpoint endpoint = parseEndpoint(config);
                if (endpoint == null) {
                    throw new IllegalArgumentException("VPN profile has no usable remote endpoint.");
                }
                ConnectTarget target = new ConnectTarget(server, config, endpoint);
                if (endpoint.isTcp()) {
                    tcpTargets.add(target);
                    continue;
                }
                if (preferResponsiveTcp) {
                    if (deferredUdpTarget == null) {
                        deferredUdpTarget = target;
                        postLogLine("UDP target cannot be prechecked, trying TCP candidates first: "
                                + endpoint.label());
                    }
                    continue;
                }
                return target;
            } catch (Exception e) {
                lastError = e;
                postLogLine("Candidate check failed: " + server.hostName + " / " + e.getMessage());
            }
        }

        if (!tcpTargets.isEmpty()) {
            postLogLine("Checking " + tcpTargets.size() + " candidate server ports in parallel.");
            List<Callable<Boolean>> checks = new ArrayList<>();
            for (ConnectTarget target : tcpTargets) {
                checks.add(() -> canConnect(target.endpoint));
            }
            List<Future<Boolean>> results = preflightExecutor.invokeAll(
                    checks, CONNECT_PREFLIGHT_BATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            for (int i = 0; i < tcpTargets.size(); i++) {
                Future<Boolean> result = results.get(i);
                try {
                    if (!result.isCancelled() && Boolean.TRUE.equals(result.get())) {
                        ConnectTarget target = tcpTargets.get(i);
                        if (!sameServer(target.server, requestedServer)) {
                            postLogLine("Using a responsive fallback instead of the selected server: "
                                    + target.server.hostName);
                        }
                        return target;
                    }
                } catch (Exception e) {
                    lastError = e;
                }
            }
            postLogLine("No TCP candidate responded within the pre-connect timeout.");
        }
        if (deferredUdpTarget != null) {
            postLogLine("No responsive TCP candidate found; trying UDP server: " + deferredUdpTarget.server.hostName);
            return deferredUdpTarget;
        }
        String message = "The selected server and available candidates are not responding. Refresh the list or try another country.";
        if (lastError != null && lastError.getMessage() != null) {
            message += "\nLast error: " + lastError.getMessage();
        }
        throw new IllegalStateException(message);
    }

    private boolean sameServer(Server a, Server b) {
        return a != null && b != null
                && fallback(a.hostName).equals(fallback(b.hostName))
                && fallback(a.ip).equals(fallback(b.ip));
    }

    private boolean canConnect(Endpoint endpoint) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host, endpoint.port), CONNECT_PREFLIGHT_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Endpoint parseEndpoint(String config) {
        String proto = "tcp";
        for (String raw : config.split("\\R")) {
            String line = raw.trim();
            if (line.startsWith("proto ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    proto = parts[1];
                }
            }
            if (line.startsWith("remote ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    try {
                        return new Endpoint(parts[1], Integer.parseInt(parts[2]), proto);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private void failConnect(String title, String message, boolean showDialog) {
        connecting = false;
        pendingConnectServer = null;
        preferredConnectCandidates.clear();
        attemptedConnectCandidates.clear();
        pendingVpnProfile = null;
        pendingVpnStartReason = null;
        mainHandler.removeCallbacks(connectTimeoutRunnable);
        if (syncVpnStateFromSystem()) {
            setStatusCard(title, fallback(message) + " / Still connected via VPN - " + connectedVpnText(), AMBER);
            logLine(title + ": " + fallback(message) + " / still connected via VPN.");
            if (showDialog) {
                showErrorDialog(title, message);
            }
            return;
        }
        vpnConnected = false;
        clearConnectedServerState();
        updateConnectButtonState(true);
        setStatusCard(title, fallback(message), RED);
        logLine(title + ": " + fallback(message));
        if (showDialog) {
            showErrorDialog(title, message);
        }
    }

    private void disconnectVpn() {
        Preferences.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean("disableconfirmation", true)
                .apply();
        connecting = false;
        vpnConnected = false;
        pendingConnectServer = null;
        preferredConnectCandidates.clear();
        attemptedConnectCandidates.clear();
        pendingVpnProfile = null;
        pendingVpnStartReason = null;
        updateConnectButtonState(true);
        mainHandler.removeCallbacks(connectTimeoutRunnable);
        setStatusCard("Disconnecting", "Requested VPN shutdown.", MUTED);
        logLine("Disconnect requested");
        if (!stopVpnService("user disconnect")) {
            Intent disconnect = new Intent(this, DisconnectVPN.class);
            disconnect.setAction(OpenVPNService.DISCONNECT_VPN);
            startActivity(disconnect);
        }
    }

    private boolean stopVpnService(String reason) {
        IOpenVPNServiceInternal binder = vpnServiceBinder;
        if (binder == null) {
            logLine("VPN service not bound (" + reason + "), using fallback disconnect.");
            return false;
        }
        try {
            ProfileManager.setConntectedVpnProfileDisconnected(this);
            binder.stopVPN(false);
            logLine("VPN service stop requested (" + reason + ").");
            return true;
        } catch (RemoteException e) {
            logLine("VPN service stop failed (" + reason + "): " + e.getMessage());
            return false;
        }
    }

    private boolean retryNextConnectCandidate(String reason) {
        if (preferredConnectCandidates.isEmpty()) {
            return false;
        }
        if (pendingConnectServer != null) {
            rememberAttemptedConnectCandidate(pendingConnectServer);
        } else if (selectedServer != null) {
            rememberAttemptedConnectCandidate(selectedServer);
        }
        if (attemptedConnectCandidates.size() >= CONNECT_AUTO_RETRY_LIMIT) {
            return false;
        }
        Server next = nextUnattemptedConnectCandidate();
        if (next == null) {
            return false;
        }

        mainHandler.removeCallbacks(connectTimeoutRunnable);
        pendingConnectServer = null;
        pendingVpnProfile = null;
        pendingVpnStartReason = null;
        String message = reason + " Trying another " + countryLabel() + " server ("
                + (attemptedConnectCandidates.size() + 1)
                + "/" + Math.min(preferredConnectCandidates.size(), CONNECT_AUTO_RETRY_LIMIT) + ")";
        setStatusCard("Trying next server", message, AMBER);
        logLine(message);
        if (!stopVpnService("auto retry")) {
            Intent disconnect = new Intent(this, DisconnectVPN.class);
            disconnect.setAction(OpenVPNService.DISCONNECT_VPN);
            try {
                startActivity(disconnect);
            } catch (Exception e) {
                logLine("Fallback disconnect before retry failed: " + e.getMessage());
            }
        }
        selectedServer = next;
        updateDetail();
        updateConnectButtonState(false);
        mainHandler.postDelayed(() -> {
            if (!vpnConnected && connecting) {
                beginConnectSelectedServer();
            }
        }, CONNECT_RETRY_DELAY_MS);
        return true;
    }

    private Server nextUnattemptedConnectCandidate() {
        for (Server server : preferredConnectCandidates) {
            if (!hasAttemptedConnectCandidate(server)) {
                return server;
            }
        }
        return null;
    }

    private void rememberAttemptedConnectCandidate(Server server) {
        if (server == null || hasAttemptedConnectCandidate(server)) {
            return;
        }
        attemptedConnectCandidates.add(server);
    }

    private boolean hasAttemptedConnectCandidate(Server candidate) {
        if (candidate == null) {
            return false;
        }
        for (Server attempted : attemptedConnectCandidates) {
            if (sameServer(candidate, attempted)) {
                return true;
            }
        }
        return false;
    }

    private void registerVpnStatusReceiver() {
        IntentFilter filter = new IntentFilter(VPN_STATUS_ACTION);
        ContextCompat.registerReceiver(
                this,
                vpnStatusReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        vpnStatusReceiverRegistered = true;
    }

    private boolean syncVpnStateFromSystem() {
        if (connecting) {
            return vpnConnected;
        }
        boolean wasConnected = vpnConnected;
        if (isAppVpnActive()) {
            restoreConnectedServerState();
            vpnConnected = true;
            updateConnectButtonState(true);
            setStatusCard("Connected", "Connected via VPN - " + connectedVpnText(), GREEN);
            updateDetail();
            if (!wasConnected) {
                logLine("Detected active VPN tunnel from Android: " + connectedVpnText());
            }
            return true;
        }
        if (wasConnected) {
            vpnConnected = false;
            clearConnectedServerState();
            updateConnectButtonState(true);
            setStatusCard("Disconnected", "Android no longer reports an active VPN tunnel.", MUTED);
            updateDetail();
            logLine("Android no longer reports an active VPN tunnel.");
        } else if (hasSavedConnectedServerState()) {
            clearConnectedServerState();
            updateConnectButtonState(true);
        }
        return false;
    }

    private boolean isAppVpnActive() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        try {
            for (Network network : manager.getAllNetworks()) {
                NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    continue;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (capabilities.getOwnerUid() == getApplicationInfo().uid) {
                        return true;
                    }
                } else if (hasSavedConnectedServerState()) {
                    return true;
                }
            }
        } catch (Exception e) {
            debugLog("Failed to inspect VPN network state", e);
        }
        return false;
    }

    private void rememberConnectedServer(Server server) {
        if (server == null) {
            return;
        }
        connectedServerLabel = cleanPrefValue(server.hostName);
        connectedCountryShort = cleanPrefValue(server.countryShort).toUpperCase(Locale.ROOT);
        connectedCountryLong = cleanPrefValue(server.countryLong);
        appPreferences().edit()
                .putString(PREF_CONNECTED_HOST, connectedServerLabel)
                .putString(PREF_CONNECTED_COUNTRY_SHORT, connectedCountryShort)
                .putString(PREF_CONNECTED_COUNTRY_LONG, connectedCountryLong)
                .apply();
    }

    private void restoreConnectedServerState() {
        SharedPreferences preferences = appPreferences();
        connectedServerLabel = cleanPrefValue(preferences.getString(PREF_CONNECTED_HOST, connectedServerLabel));
        connectedCountryShort = cleanPrefValue(preferences.getString(PREF_CONNECTED_COUNTRY_SHORT, connectedCountryShort));
        connectedCountryLong = cleanPrefValue(preferences.getString(PREF_CONNECTED_COUNTRY_LONG, connectedCountryLong));
    }

    private void clearConnectedServerState() {
        connectedServerLabel = "";
        connectedCountryShort = "";
        connectedCountryLong = "";
        appPreferences().edit()
                .remove(PREF_CONNECTED_HOST)
                .remove(PREF_CONNECTED_COUNTRY_SHORT)
                .remove(PREF_CONNECTED_COUNTRY_LONG)
                .apply();
    }

    private boolean hasSavedConnectedServerState() {
        SharedPreferences preferences = appPreferences();
        return !cleanPrefValue(preferences.getString(PREF_CONNECTED_HOST, "")).isEmpty()
                || !cleanPrefValue(preferences.getString(PREF_CONNECTED_COUNTRY_SHORT, "")).isEmpty()
                || !cleanPrefValue(preferences.getString(PREF_CONNECTED_COUNTRY_LONG, "")).isEmpty();
    }

    private SharedPreferences appPreferences() {
        return Preferences.getDefaultSharedPreferences(this);
    }

    private String cleanPrefValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void handleVpnStatus(String state, String level, String message) {
        String stateText = fallback(state);
        String levelText = fallback(level);
        String messageText = fallback(message);
        if ("LEVEL_CONNECTED".equals(level)) {
            connecting = false;
            vpnConnected = true;
            if (pendingConnectServer != null) {
                rememberConnectedServer(pendingConnectServer);
            } else {
                restoreConnectedServerState();
            }
            pendingConnectServer = null;
            preferredConnectCandidates.clear();
            attemptedConnectCandidates.clear();
            updateConnectButtonState(true);
            mainHandler.removeCallbacks(connectTimeoutRunnable);
            setStatusCard("Connected", "Connected via VPN - " + connectedVpnText(), GREEN);
            updateDetail();
            logLine("VPN connected: " + messageText);
            long now = System.currentTimeMillis();
            if (now - lastConnectedIpCheckAt > 30000) {
                lastConnectedIpCheckAt = now;
                checkPublicIp();
            }
            return;
        }
        if ("LEVEL_AUTH_FAILED".equals(level) || "LEVEL_NOTCONNECTED".equals(level)) {
            if (connecting && "NOPROCESS".equals(stateText)) {
                logLine("Ignoring initial VPN state: " + stateText + " / " + messageText);
                return;
            }
            if (connecting && retryNextConnectCandidate("VPN failed: " + stateText + " / " + messageText)) {
                return;
            }
            connecting = false;
            vpnConnected = false;
            pendingConnectServer = null;
            clearConnectedServerState();
            preferredConnectCandidates.clear();
            attemptedConnectCandidates.clear();
            updateConnectButtonState(true);
            mainHandler.removeCallbacks(connectTimeoutRunnable);
            setStatusCard("Disconnected", "VPN connection ended. " + stateText, RED);
            updateDetail();
            logLine("VPN disconnected: " + stateText + " / " + messageText);
            return;
        }
        if ("LEVEL_CONNECTING_NO_SERVER_REPLY_YET".equals(level)
                || "LEVEL_CONNECTING_SERVER_REPLIED".equals(level)
                || "LEVEL_START".equals(level)
                || "LEVEL_WAITING_FOR_USER_INPUT".equals(level)) {
            setStatusCard("Connecting", stateText + " / " + messageText, BLUE);
            logLine("VPN status: " + stateText + " / " + messageText);
            return;
        }
        setStatusCard("VPN status", stateText + " / " + levelText, MUTED);
        logLine("VPN status: " + stateText + " / " + levelText + " / " + messageText);
    }

    private VpnProfile parseVpnProfile(String config, Server server) throws Exception {
        ConfigParser parser = new ConfigParser();
        parser.parseConfig(new StringReader(config));
        VpnProfile profile = parser.convertProfile();
        profile.mName = "Noren VPN " + serverCountryDisplayName(server) + " - " + server.hostName;
        profile.mProfileCreator = getPackageName();
        profile.mTemporaryProfile = true;
        profile.mBlockUnusedAddressFamilies = true;
        profile.mUseDefaultRoute = true;
        profile.mUseDefaultRoutev6 = false;
        return profile;
    }

    private boolean requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS && pendingConnectAfterNotificationPermission) {
            pendingConnectAfterNotificationPermission = false;
            beginConnectSelectedServer();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VPN_PERMISSION) {
            if (resultCode == Activity.RESULT_OK && pendingVpnProfile != null) {
                startPreparedVpn(pendingVpnProfile, pendingVpnStartReason);
            } else {
                pendingVpnProfile = null;
                pendingVpnStartReason = null;
                preferredConnectCandidates.clear();
                attemptedConnectCandidates.clear();
                failConnect("VPN permission denied", "Android VPN permission was not granted.", false);
            }
        }
    }

    private void updateConnectButtonState(boolean enabled) {
        if (startButton != null) {
            startButton.setEnabled(enabled);
            if (!enabled) {
                startButton.setText(R.string.action_connecting);
            } else if (vpnConnected) {
                startButton.setText(R.string.action_disconnect);
            } else {
                startButton.setText(R.string.action_start_vpn);
            }
            startButton.setBackgroundResource(vpnConnected
                    ? R.drawable.tv_focus_action_connected
                    : R.drawable.tv_focus_action);
            startButton.setAlpha(enabled ? 1.0f : 0.55f);
        }
        if (connectButton != null) {
            connectButton.setEnabled(enabled);
            if (!enabled) {
                connectButton.setText(R.string.action_connecting_short);
            } else if (vpnConnected) {
                connectButton.setText(R.string.action_switch_server);
            } else {
                connectButton.setText(R.string.action_connect_selected);
            }
            connectButton.setAlpha(enabled ? 1.0f : 0.55f);
        }
        if (disconnectButton != null) {
            disconnectButton.setEnabled(enabled);
            disconnectButton.setVisibility(vpnConnected || connecting ? View.VISIBLE : View.GONE);
            disconnectButton.setAlpha(enabled ? 1.0f : 0.55f);
        }
        wireHomeActionFocus();
    }

    private File writeProfileFile() throws Exception {
        File dir = new File(getCacheDir(), "ovpn");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Could not create cache directory.");
        }
        File file = new File(dir, safeFileName(selectedServer.hostName) + ".ovpn");
        String config = profileConfig(selectedServer);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(config.getBytes(StandardCharsets.UTF_8));
        }
        logLine("OVPN profile created: " + file.getName());
        return file;
    }

    private boolean saveProfileToDownloads(File source) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            logLine("Saving to Downloads requires Android 10 or later.");
            return false;
        }
        Uri target = null;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, source.getName());
            values.put(MediaStore.Downloads.MIME_TYPE, OVPN_MIME);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NorenVPN");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            target = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (target == null) {
                return false;
            }
            try (java.io.InputStream input = new java.io.FileInputStream(source);
                 java.io.OutputStream output = getContentResolver().openOutputStream(target)) {
                if (output == null) {
                    throw new IllegalStateException("Could not open Downloads output stream.");
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
            ContentValues complete = new ContentValues();
            complete.put(MediaStore.Downloads.IS_PENDING, 0);
            getContentResolver().update(target, complete, null, null);
            logLine("Also saved to Downloads/NorenVPN: " + source.getName());
            return true;
        } catch (Exception e) {
            logLine("Skipped saving to Downloads: " + e.getMessage());
            if (target != null) {
                try {
                    getContentResolver().delete(target, null, null);
                } catch (Exception ignored) {
                    // Best effort cleanup for a failed MediaStore insert.
                }
            }
            return false;
        }
    }

    private void showProfileReadyDialog(Uri uri, String fileName, boolean savedToDownloads) {
        String location = savedToDownloads
                ? "Downloads/NorenVPN/" + fileName
                : getString(R.string.profile_location_app, fileName);
        String message = getString(R.string.profile_saved_message, fileName, location);
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_saved_title)
                .setMessage(message)
                .setPositiveButton(R.string.open_in_openvpn, (dialog, which) -> {
                    if (!openProfileWithFallback(uri)) {
                        logLine("No OpenVPN app found. Import manually from Downloads/NorenVPN.");
                        showInstallDialog();
                    }
                })
                .setNegativeButton(R.string.open_openvpn_app, (dialog, which) -> openOpenVpnApp())
                .setNeutralButton(R.string.action_close, null)
                .show();
    }

    private boolean openProfileWithFallback(Uri uri) {
        if (tryOpenProfile(uri, OVPN_MIME, OPENVPN_FOR_ANDROID)) {
            logLine("Opening profile with OpenVPN for Android");
            return true;
        }
        if (tryOpenProfile(uri, OVPN_MIME, OPENVPN_CONNECT)) {
            logLine("Opening profile with OpenVPN Connect");
            return true;
        }
        if (tryOpenProfile(uri, OVPN_MIME, null)) {
            logLine("Opening profile chooser");
            return true;
        }
        if (tryOpenProfile(uri, "application/octet-stream", null)) {
            logLine("Opening profile with file-type fallback");
            return true;
        }
        if (tryShareProfile(uri)) {
            logLine("Sharing profile via share menu");
            return true;
        }
        return false;
    }

    private String profileConfig(Server server) {
        VpnGateConfigValidator.validateBase64(server.configBase64);
        byte[] decoded;
        try {
            decoded = Base64.decode(server.configBase64, Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("VPN profile Base64 decoding failed.", e);
        }
        if (decoded.length > VpnGateConfigValidator.MAX_DECODED_BYTES) {
            throw new IllegalArgumentException("VPN profile is too large.");
        }
        String config = decodeUtf8Strict(decoded, "VPN profile");
        return VpnGateConfigValidator.validateAndHarden(config, server.ip);
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message == null || message.isEmpty() ? getString(R.string.unknown_error) : message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showErrorDialog(int titleRes, int messageRes) {
        showErrorDialog(getString(titleRes), getString(messageRes));
    }

    private void showErrorDialog(int titleRes, String message) {
        showErrorDialog(getString(titleRes), message);
    }

    private void openWebPage(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException | SecurityException e) {
            showErrorDialog(R.string.unable_open_link_title, url);
        }
    }

    private boolean tryOpenProfile(Uri uri, String mimeType, String packageName) {
        Intent intent = profileViewIntent(uri, mimeType);
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        } catch (SecurityException e) {
            logLine("Profile grant failed: " + e.getMessage());
            return false;
        }
    }

    private Intent profileViewIntent(Uri uri, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newUri(getContentResolver(), "ovpn", uri));
        return intent;
    }

    private boolean tryShareProfile(Uri uri) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(OVPN_MIME);
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newUri(getContentResolver(), "ovpn", uri));
        Intent chooser = Intent.createChooser(send, "Send to OpenVPN app");
        try {
            startActivity(chooser);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    private void openOpenVpnApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("de.blinkt.openvpn");
        if (launch != null) {
            startActivity(launch);
            return;
        }
        showInstallDialog();
    }

    private void showInstallDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.external_openvpn_title)
                .setMessage(R.string.external_openvpn_message)
                .setPositiveButton(R.string.open_play_store, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=de.blinkt.openvpn"));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.action_close, null)
                .show();
    }

    private List<Server> autoRankedServers(AutoProfile profile) {
        List<Server> ranked = new ArrayList<>();
        for (Server server : filterCountry(allServers)) {
            if (autoRejectReason(server, profile) == null) {
                ranked.add(server);
            }
        }
        Collections.sort(ranked, (left, right) -> {
            int byScore = Double.compare(autoScore(right, profile), autoScore(left, profile));
            if (byScore != 0) {
                return byScore;
            }
            int byPing = comparePingAsc(left, right);
            if (byPing != 0) {
                return byPing;
            }
            return compareSpeedDesc(left, right);
        });
        return ranked;
    }

    private List<Server> fallbackRankedServers() {
        List<Server> pool = new ArrayList<>();
        for (Server server : filterCountry(allServers)) {
            if (guideReason(server) == null) {
                pool.add(server);
            }
        }
        if (pool.isEmpty()) {
            pool.addAll(filterCountry(allServers));
        }
        Collections.sort(pool, (left, right) -> {
            int byScore = compareScoreDesc(left, right);
            if (byScore != 0) {
                return byScore;
            }
            int bySpeed = compareSpeedDesc(left, right);
            if (bySpeed != 0) {
                return bySpeed;
            }
            return comparePingAsc(left, right);
        });
        return pool;
    }

    private void appendFallbackConnectCandidates(List<Server> ranked) {
        if (ranked == null || ranked.size() >= CONNECT_CANDIDATE_LIMIT) {
            return;
        }
        for (Server server : fallbackRankedServers()) {
            if (ranked.size() >= CONNECT_CANDIDATE_LIMIT) {
                return;
            }
            if (!containsSameServer(ranked, server)) {
                ranked.add(server);
            }
        }
    }

    private boolean containsSameServer(List<Server> rows, Server candidate) {
        for (Server server : rows) {
            if (sameServer(server, candidate)) {
                return true;
            }
        }
        return false;
    }

    private String autoRejectReason(Server server, AutoProfile profile) {
        String guide = guideReason(server);
        if (guide != null) {
            return guide;
        }
        if (server.ping < 0) {
            return getString(R.string.reason_no_ping);
        }
        if (server.ping > profile.maxPingMs) {
            return getString(R.string.reason_ping_high, server.pingText());
        }
        if (server.speed < profile.minSpeedBps) {
            return getString(R.string.reason_speed_low, profile.minSpeedMbpsText());
        }
        if (server.sessions > profile.maxSessions) {
            return getResources().getQuantityString(
                    R.plurals.reason_sessions_high,
                    profile.maxSessions,
                    profile.maxSessions);
        }
        if (profile.minUptimeMs > 0 && server.uptime < profile.minUptimeMs) {
            return getString(R.string.reason_uptime_short, uptimeText(profile.minUptimeMs));
        }
        if (profile.maxUptimeMs > 0 && server.uptime > profile.maxUptimeMs) {
            return getString(R.string.reason_uptime_long, uptimeText(profile.maxUptimeMs));
        }
        return null;
    }

    private double autoScore(Server server, AutoProfile profile) {
        double ping = server.ping < 0 ? profile.maxPingMs : server.ping;
        double pingScore = clamp01((profile.maxPingMs - ping) / Math.max(1.0, profile.maxPingMs));
        double speedMbps = server.speed / 1_000_000.0;
        double minSpeedMbps = profile.minSpeedBps / 1_000_000.0;
        double speedScore = clamp01(speedMbps / Math.max(1.0, minSpeedMbps * 4.0));
        double sessionScore = clamp01((profile.maxSessions - server.sessions) / Math.max(1.0, (double) profile.maxSessions));
        double uptimeHours = server.uptime <= 0 ? 0 : server.uptime / 3_600_000.0;

        if ("stream".equals(profile.id)) {
            double maxHours = Math.max(1.0, profile.maxUptimeMs / 3_600_000.0);
            double shortUptimeScore = clamp01(1.0 - (uptimeHours / maxHours));
            return shortUptimeScore * 45.0 + speedScore * 25.0 + pingScore * 20.0 + sessionScore * 10.0;
        }
        if ("stable".equals(profile.id)) {
            double uptimeScore = clamp01(uptimeHours / 48.0);
            return uptimeScore * 35.0 + speedScore * 30.0 + pingScore * 20.0 + sessionScore * 15.0;
        }
        if ("speed".equals(profile.id)) {
            return speedScore * 55.0 + pingScore * 20.0 + sessionScore * 15.0 + clamp01(uptimeHours / 6.0) * 10.0;
        }
        return pingScore * 50.0 + speedScore * 25.0 + sessionScore * 20.0 + clamp01(uptimeHours / 12.0) * 5.0;
    }

    private double clamp01(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }

    private String guideReason(Server server) {
        if (server.ping < 0) {
            return getString(R.string.reason_no_ping);
        }
        if (server.speed <= 0) {
            return getString(R.string.reason_no_speed);
        }
        if (server.sessions >= 100) {
            return getString(R.string.reason_crowded);
        }
        return null;
    }

    private String safeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String uptimeText(long milliseconds) {
        if (milliseconds <= 0) {
            return "0m";
        }
        long seconds = milliseconds / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private void setListStatus(int displayedCount, int matchingCount, int beforeGuide) {
        String countText = displayedCount < matchingCount
                ? displayedCount + " of " + matchingCount
                : String.valueOf(matchingCount);
        if (vpnConnected) {
            setStatusCard("Connected", "Connected via VPN - " + connectedVpnText()
                    + " / Selected list: " + countryLabel() + " " + countText + " servers", GREEN);
            return;
        }
        if (!connecting) {
            setStatusCard("Disconnected", countryLabel() + " servers: " + countText
                    + " / before filter: " + beforeGuide, GREEN);
        }
    }

    private String cacheStatusTitle(long ageMs) {
        return ageMs > SERVER_CACHE_STALE_MS ? "List is stale" : "Using cached list";
    }

    private String cacheStatusSubtitle(long ageMs) {
        return "The server directory is not responding; showing the last successful list. " + cacheAgeText(ageMs);
    }

    private String cacheAgeText(long ageMs) {
        if (ageMs == Long.MAX_VALUE) {
            return "saved time unknown";
        }
        long minutes = Math.max(0, ageMs / 60000);
        if (minutes < 1) {
            return "less than 1 minute ago";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        long remainMinutes = minutes % 60;
        if (hours < 24) {
            return hours + "h " + remainMinutes + "m ago";
        }
        long days = hours / 24;
        long remainHours = hours % 24;
        return days + "d " + remainHours + "h ago";
    }

    private String connectedServerText() {
        return connectedServerLabel == null || connectedServerLabel.isEmpty()
                ? getString(R.string.connected_default)
                : connectedServerLabel;
    }

    private String connectedVpnText() {
        String detail = connectedStatusDetail();
        return detail.isEmpty() ? connectedServerText() : detail;
    }

    private String connectedDetailText() {
        return vpnConnected ? getString(R.string.current_vpn, connectedVpnText()) : "";
    }

    private String countryLabel() {
        for (CountryOption option : countryOptions) {
            if (option.matches(selectedCountryShort, selectedCountryLong)) {
                return countryDisplayName(option);
            }
        }
        return selectedCountryLong.isEmpty() ? getString(R.string.selected_country) : selectedCountryLong;
    }

    private String countryCardLabel(CountryOption option) {
        return countryFlag(option.shortName) + " "
                + countryName(option.shortName, option.longName)
                + " · " + option.count;
    }

    private String countryFlag(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 2
                || normalized.charAt(0) < 'A'
                || normalized.charAt(0) > 'Z'
                || normalized.charAt(1) < 'A'
                || normalized.charAt(1) > 'Z') {
            return "🌐";
        }
        int first = 0x1F1E6 + normalized.charAt(0) - 'A';
        int second = 0x1F1E6 + normalized.charAt(1) - 'A';
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

    private String countryDisplayName(CountryOption option) {
        if (option == null) {
            return getString(R.string.selected_country);
        }
        String display = countryName(option.shortName, option.longName);
        if (option.shortName == null || option.shortName.isEmpty()) {
            return display;
        }
        return display + " (" + option.shortName + ")";
    }

    private String serverCountryDisplayName(Server server) {
        if (server == null) {
            return getString(R.string.selected_country);
        }
        String display = countryName(server.countryShort, server.countryLong);
        if (server.countryShort == null || server.countryShort.isEmpty()) {
            return display;
        }
        return display + " (" + server.countryShort.toUpperCase(Locale.ROOT) + ")";
    }

    private String countryName(String code, String fallbackName) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.length() == 2) {
            Locale displayLocale;
            Configuration configuration = getResources().getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                displayLocale = configuration.getLocales().get(0);
            } else {
                displayLocale = configuration.locale;
            }
            String localized = new Locale("", normalizedCode).getDisplayCountry(displayLocale);
            if (localized != null && !localized.isEmpty() && !localized.equalsIgnoreCase(normalizedCode)) {
                return localized;
            }
        }
        return fallbackName == null || fallbackName.isEmpty() ? normalizedCode : fallbackName;
    }

    private void updateToolbarColors() {
        styleChoiceButton(speedButton, "speed".equals(sort));
        styleChoiceButton(pingButton, "ping".equals(sort));
        styleChoiceButton(scoreButton, "score".equals(sort));
        if (guideButton != null) {
            styleChoiceButton(guideButton, guideFilter);
        }
    }

    private void setStatusCard(String title, String subtitle, int color) {
        if (statusTitle != null) {
            statusTitle.setText(statusBadgeText());
            statusTitle.setTextColor(statusBadgeColor(color));
        }
        if (status != null) {
            status.setText(subtitle);
            status.setTextColor(MUTED);
        }
    }

    private String statusBadgeText() {
        if (vpnConnected) {
            if (!tvMode) {
                return getString(R.string.status_connected);
            }
            String detail = connectedStatusDetail();
            return detail.isEmpty()
                    ? getString(R.string.status_connected)
                    : getString(R.string.status_connected_detail, detail);
        }
        if (connecting) {
            return getString(R.string.status_connecting);
        }
        return getString(R.string.status_disconnected);
    }

    private int statusBadgeColor(int fallbackColor) {
        if (vpnConnected) {
            return GREEN;
        }
        if (connecting) {
            return BLUE;
        }
        return fallbackColor == RED ? RED : MUTED;
    }

    private String connectedStatusDetail() {
        String country = countryName(connectedCountryShort, connectedCountryLong);
        String server = connectedServerLabel == null ? "" : connectedServerLabel.trim();
        if (country == null || country.isEmpty()) {
            return server;
        }
        if (server.isEmpty()) {
            return country;
        }
        return country + " " + server;
    }

    private void updateFooterInfo(String refreshText) {
        if (footerInfo == null) {
            return;
        }
        footerInfo.setText(getString(R.string.footer_status, appVersionText(), refreshText));
    }

    private String formatClock(long timeMs) {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(timeMs));
    }

    private TextView sectionLabel(String value) {
        TextView label = text(value, 21, TEXT);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params = matchWrapWithBottom(dp(10));
        label.setLayoutParams(params);
        return label;
    }

    private LinearLayout card(int fillColor, int strokeColor) {
        LinearLayout layout = new LinearLayout(this);
        layout.setBackground(roundedBg(fillColor, strokeColor, 12));
        return layout;
    }

    private GradientDrawable roundedBg(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrapWithBottom(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = bottomMargin;
        return params;
    }

    private LinearLayout.LayoutParams equalButtonParams(int leftMargin, int count) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(64), 1f / Math.max(1, count));
        params.leftMargin = leftMargin;
        return params;
    }

    private Button primaryButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(24);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.tv_focus_action);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setMinHeight(dp(80));
        if (listener != null) {
            button.setOnClickListener(listener);
        }
        applyTvFocusEffect(button);
        return button;
    }

    private Button secondaryButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(TEXT);
        button.setTextSize(22);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.tv_focus_card);
        button.setPadding(dp(10), 0, dp(10), 0);
        if (listener != null) {
            button.setOnClickListener(listener);
        }
        applyTvFocusEffect(button);
        return button;
    }

    private Button iconButton(String label, View.OnClickListener listener) {
        Button button = secondaryButton(label, listener);
        button.setTextSize(14);
        button.setMinWidth(dp(76));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(82), dp(46));
        button.setLayoutParams(params);
        return button;
    }

    private void styleChoiceButton(Button button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setTextColor(selected ? Color.WHITE : TEXT);
        button.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        button.setBackgroundResource(selected ? R.drawable.tv_focus_card_selected : R.drawable.tv_focus_card);
    }

    private void toggleDetailRequested() {
        if (!detailVisible && !requireProSelection()) {
            return;
        }
        setDetailVisible(!detailVisible);
    }

    private void setDetailVisible(boolean visible) {
        detailVisible = visible;
        if (detailSection != null) {
            detailSection.setVisibility(detailVisible ? View.VISIBLE : View.GONE);
        }
        updateProUi();
    }

    private void showInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.privacy_title)
                .setMessage(R.string.privacy_message)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.logging_policy, (ignoredDialog, which) -> openWebPage(VPN_GATE_LOGGING_POLICY_URL))
                .show();
    }

    private Button button(String label) {
        return button(label, null);
    }

    private Button wideButton(String label, View.OnClickListener listener) {
        Button button = button(label, listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1);
        params.rightMargin = dp(10);
        button.setLayoutParams(params);
        button.setTextSize(16);
        return button;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(TEXT);
        button.setTextSize(22);
        button.setAllCaps(false);
        button.setBackgroundResource(R.drawable.tv_focus_card);
        button.setMinWidth(dp(168));
        button.setPadding(dp(18), 0, dp(18), 0);
        if (listener != null) {
            button.setOnClickListener(listener);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(64));
        params.rightMargin = dp(12);
        button.setLayoutParams(params);
        applyTvFocusEffect(button);
        return button;
    }

    private boolean isTvMode() {
        int uiMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK;
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    private void applyFormFactorLayout() {
        if (tvMode) {
            return;
        }
        if (statusTitle != null) {
            statusTitle.setTextSize(18);
        }
        LinearLayout homeActions = findViewById(R.id.homeActions);
        if (homeActions != null) {
            homeActions.setOrientation(LinearLayout.VERTICAL);
            homeActions.setGravity(Gravity.CENTER_HORIZONTAL);
            ViewGroup.LayoutParams params = homeActions.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            homeActions.setLayoutParams(params);
        }
        configurePhoneActionButton(detailToggleButton);
        configurePhoneActionButton(disconnectButton);
        configurePhoneActionButton(privacyButton);
    }

    private void configurePhoneActionButton(Button button) {
        if (button == null) {
            return;
        }
        button.setTextSize(18);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(16), 0, dp(16), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56));
        params.bottomMargin = dp(10);
        params.rightMargin = 0;
        params.setMarginEnd(0);
        button.setLayoutParams(params);
    }

    private void applyTvFocusEffect(View view) {
        if (view == null) {
            return;
        }
        if (!tvMode) {
            view.setFocusable(true);
            view.setFocusableInTouchMode(false);
            view.setOnFocusChangeListener(null);
            view.setScaleX(1f);
            view.setScaleY(1f);
            return;
        }
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnFocusChangeListener((v, hasFocus) -> applyFocusTransform(v, hasFocus));
    }

    private void applyFocusTransform(View view, boolean hasFocus) {
        if (view == null) {
            return;
        }
        view.animate()
                .scaleX(hasFocus ? 1.04f : 1f)
                .scaleY(hasFocus ? 1.04f : 1f)
                .setDuration(90)
                .start();
        view.setElevation(hasFocus ? dp(6) : 0);
    }

    private TextView text(String value, int sp, int color) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        return textView;
    }

    private void logLine(String message) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message);
        }
        if (log == null) {
            return;
        }
        String next = message + "\n" + log.getText();
        if (next.length() > 5000) {
            next = next.substring(0, 5000);
        }
        log.setText(next);
    }

    private void postLogLine(String message) {
        mainHandler.post(() -> logLine(message));
    }

    private void debugLog(String message, Throwable error) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, message, error);
        }
    }

    private String appVersionText() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
            return info.versionName + " (" + code + ")";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String jsonValue(String json, String key) {
        String needle = "\"" + key + "\"";
        int keyIndex = json.indexOf(needle);
        if (keyIndex < 0) {
            return "";
        }
        int colon = json.indexOf(':', keyIndex + needle.length());
        if (colon < 0) {
            return "";
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length()) {
            return "";
        }
        if (json.charAt(start) == '"') {
            StringBuilder out = new StringBuilder();
            boolean escaped = false;
            for (int i = start + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    out.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    return out.toString();
                } else {
                    out.append(c);
                }
            }
            return "";
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim();
    }

    private String fallback(String value) {
        return value == null || value.isEmpty() || "null".equals(value) ? "-" : value;
    }

    private String shortUrl(String url) {
        if (url == null) {
            return "-";
        }
        return url.replace("https://", "").replace("http://", "");
    }

    private String exceptionText(Exception e) {
        if (e == null) {
            return "Unknown error";
        }
        String message = e.getClass().getSimpleName();
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            message += ": " + e.getMessage();
        }
        Throwable[] suppressed = e.getSuppressed();
        if (suppressed.length > 0) {
            message += "\nPrevious error: " + suppressed[0].getClass().getSimpleName();
            if (suppressed[0].getMessage() != null && !suppressed[0].getMessage().isEmpty()) {
                message += ": " + suppressed[0].getMessage();
            }
        }
        return message;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class AutoProfile {
        final String id;
        final String label;
        final String buttonLabel;
        final double maxPingMs;
        final long minSpeedBps;
        final int maxSessions;
        final long minUptimeMs;
        final long maxUptimeMs;

        AutoProfile(String id, String label, String buttonLabel, double maxPingMs, long minSpeedBps,
                    int maxSessions, long minUptimeMs, long maxUptimeMs) {
            this.id = id;
            this.label = label;
            this.buttonLabel = buttonLabel;
            this.maxPingMs = maxPingMs;
            this.minSpeedBps = minSpeedBps;
            this.maxSessions = maxSessions;
            this.minUptimeMs = minUptimeMs;
            this.maxUptimeMs = maxUptimeMs;
        }

        String minSpeedMbpsText() {
            return String.format(Locale.US, "%.0f", minSpeedBps / 1_000_000.0);
        }

        String summary() {
            String text = "Ping <= " + String.format(Locale.US, "%.0f", maxPingMs) + "ms"
                    + " / speed >= " + minSpeedMbpsText() + "Mbps"
                    + " / sessions <= " + maxSessions;
            if (minUptimeMs > 0) {
                text += " / uptime >= " + uptimeTextStatic(minUptimeMs);
            }
            if (maxUptimeMs > 0) {
                text += " / uptime <= " + uptimeTextStatic(maxUptimeMs);
            }
            return text;
        }

        private static String uptimeTextStatic(long milliseconds) {
            long seconds = Math.max(0, milliseconds / 1000);
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            long minutes = (seconds % 3600) / 60;
            if (days > 0) {
                return days + "d " + hours + "h";
            }
            if (hours > 0) {
                return hours + "h " + minutes + "m";
            }
            return minutes + "m";
        }
    }

    private static final class Server {
        String hostName;
        String ip;
        String countryLong;
        String countryShort;
        double ping;
        long speed;
        int sessions;
        long uptime;
        long score;
        String configBase64;

        String speedText() {
            return String.format(Locale.US, "%.2f", speed / 1_000_000.0);
        }

        String pingText() {
            return ping < 0 ? "-" : String.format(Locale.US, "%.0f", ping);
        }
    }

    private static final class DownloadResult {
        final String body;
        final String source;
        final boolean fromCache;
        final long cacheAgeMs;

        DownloadResult(String body, String source, boolean fromCache, long cacheAgeMs) {
            this.body = body;
            this.source = source;
            this.fromCache = fromCache;
            this.cacheAgeMs = cacheAgeMs;
        }
    }

    private static final class CountryOption {
        String shortName;
        String longName;
        int count;

        String label() {
            if (longName == null || longName.isEmpty()) {
                return shortName;
            }
            if (shortName == null || shortName.isEmpty()) {
                return longName;
            }
            return longName + " (" + shortName + ")";
        }

        boolean matches(String code, String name) {
            boolean codeMatches = shortName != null
                    && !shortName.isEmpty()
                    && code != null
                    && !code.isEmpty()
                    && shortName.equalsIgnoreCase(code);
            boolean nameMatches = longName != null
                    && !longName.isEmpty()
                    && name != null
                    && !name.isEmpty()
                    && longName.equalsIgnoreCase(name);
            return codeMatches || nameMatches;
        }
    }

    private static final class Endpoint {
        final String host;
        final int port;
        final String proto;

        Endpoint(String host, int port, String proto) {
            this.host = host;
            this.port = port;
            this.proto = proto == null ? "" : proto;
        }

        boolean isTcp() {
            return proto.toLowerCase(Locale.ROOT).startsWith("tcp");
        }

        String label() {
            return proto + " " + host + ":" + port;
        }
    }

    private static final class ConnectTarget {
        final Server server;
        final String config;
        final Endpoint endpoint;

        ConnectTarget(Server server, String config, Endpoint endpoint) {
            this.server = server;
            this.config = config;
            this.endpoint = endpoint;
        }
    }
}
