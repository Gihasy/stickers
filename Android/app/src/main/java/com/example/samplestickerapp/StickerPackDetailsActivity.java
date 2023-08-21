/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import games.moisoni.google_iab.BillingConnector;
import games.moisoni.google_iab.BillingEventListener;
import games.moisoni.google_iab.enums.PurchasedResult;
import games.moisoni.google_iab.enums.ProductType;
import games.moisoni.google_iab.enums.SupportState;
import games.moisoni.google_iab.models.BillingResponse;
import games.moisoni.google_iab.models.ProductInfo;
import games.moisoni.google_iab.models.PurchaseInfo;

public class StickerPackDetailsActivity extends AddStickerPackActivity {

    /**
     * Do not change below values of below 3 lines as this is also used by WhatsApp
     */
    public static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";
    public static final String EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority";
    public static final String EXTRA_STICKER_PACK_NAME = "sticker_pack_name";

    public static final String EXTRA_STICKER_PACK_WEBSITE = "sticker_pack_website";
    public static final String EXTRA_STICKER_PACK_EMAIL = "sticker_pack_email";
    public static final String EXTRA_STICKER_PACK_PRIVACY_POLICY = "sticker_pack_privacy_policy";
    public static final String EXTRA_STICKER_PACK_LICENSE_AGREEMENT = "sticker_pack_license_agreement";
    public static final String EXTRA_STICKER_PACK_TRAY_ICON = "sticker_pack_tray_icon";
    public static final String EXTRA_SHOW_UP_BUTTON = "show_up_button";
    public static final String EXTRA_STICKER_PACK_DATA = "sticker_pack";


    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;
    private StickerPreviewAdapter stickerPreviewAdapter;
    private int numColumns;
    private View addButton;
    private TextView addText;
    private View alreadyAddedText;
    private StickerPack stickerPack;
    private View divider;
    private WhiteListCheckAsyncTask whiteListCheckAsyncTask;
    private BillingConnector billingConnector;
    //list for example purposes to demonstrate how to manually acknowledge or consume purchases
    private final List<PurchaseInfo> purchasedInfoList = new ArrayList<>();

    //list for example purposes to demonstrate how to synchronously check a purchase state
    private final List<ProductInfo> fetchedProductInfoList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_pack_details);
        boolean showUpButton = getIntent().getBooleanExtra(EXTRA_SHOW_UP_BUTTON, false);
        stickerPack = getIntent().getParcelableExtra(EXTRA_STICKER_PACK_DATA);
        TextView packNameTextView = findViewById(R.id.pack_name);
        TextView packPublisherTextView = findViewById(R.id.author);
        ImageView packTrayIcon = findViewById(R.id.tray_image);
        TextView packSizeTextView = findViewById(R.id.pack_size);
        SimpleDraweeView expandedStickerView = findViewById(R.id.sticker_details_expanded_sticker);

        addButton = findViewById(R.id.add_to_whatsapp_button);
        addText = findViewById(R.id.add_to_whatsapp_text);
        alreadyAddedText = findViewById(R.id.already_added_text);
        layoutManager = new GridLayoutManager(this, 1);
        recyclerView = findViewById(R.id.sticker_list);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(pageLayoutListener);
        recyclerView.addOnScrollListener(dividerScrollListener);
        divider = findViewById(R.id.divider);
        if (stickerPreviewAdapter == null) {
            stickerPreviewAdapter = new StickerPreviewAdapter(getLayoutInflater(), R.drawable.sticker_error, getResources().getDimensionPixelSize(R.dimen.sticker_pack_details_image_size), getResources().getDimensionPixelSize(R.dimen.sticker_pack_details_image_padding), stickerPack, expandedStickerView);
            recyclerView.setAdapter(stickerPreviewAdapter);
        }
        packNameTextView.setText(stickerPack.name);
        packPublisherTextView.setText(stickerPack.publisher);
        packTrayIcon.setImageURI(StickerPackLoader.getStickerAssetUri(stickerPack.identifier, stickerPack.trayImageFile));
        packSizeTextView.setText(Formatter.formatShortFileSize(this, stickerPack.getTotalSize()));
        // init billing
        initializeBillingClient();

        if(stickerPack.price.equals("FREE")) {
            addButton.setOnClickListener(v -> addStickerPackToWhatsApp(stickerPack.identifier, stickerPack.name));
        }else{
            addButton.setOnClickListener(v ->
                    //billing function
                    billingConnector.purchase(StickerPackDetailsActivity.this, stickerPack.identifier));
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(showUpButton);
            getSupportActionBar().setTitle(showUpButton ? getResources().getString(R.string.title_activity_sticker_pack_details_multiple_pack) : getResources().getQuantityString(R.plurals.title_activity_sticker_packs_list, 1));
        }

        findViewById(R.id.sticker_pack_animation_indicator).setVisibility(stickerPack.animatedStickerPack ? View.VISIBLE : View.GONE);
    }
    private void initializeBillingClient() {
        List<String> nonConsumableIds = new ArrayList<>();
        nonConsumableIds.add(stickerPack.identifier);
        billingConnector = new BillingConnector(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApDBjWRJn7/bXvnEdN+7i3/dVWodbjQejyeYwgE1Y1RBYny+5mmmWRVezNWtqkCHqTkjlCwvVfDrp13Tr+gMh4BfiwqdWe5b+EVyULlrnYYm3QnujIgV8eRIi8gue/v4Hk2M2ezl4DwhwAmi8MWpHdVEsGONOgsRc/fQLDSEEi9XvsMrHu7M8O54RMq1WciB5UBIkPNu6oEMQgcg58GmLW7Y3Kk7cVX5a6oyAR1vbYmmRAAUgegFQq6RMu2lT5f9Q2lKqXQqsn8lfT5CMM8ZOv2lXghW3TJnRUIHYhqYdJ5tTqTnmYrjUjoYtIEy/dESfSb7xKbw0nxyxWMgirIQ4uwIDAQAB")
                .setNonConsumableIds(nonConsumableIds)
                .autoAcknowledge()
                .autoConsume()
                .enableLogging()
                .connect();
        billingConnector.setBillingEventListener(new BillingEventListener() {
            @Override
            public void onProductsFetched(@NonNull List<ProductInfo> productDetails) {
                String product;
                String price;

                for (ProductInfo productInfo : productDetails) {
                    product = productInfo.getProduct();
                    price = productInfo.getOneTimePurchaseOfferFormattedPrice();

                    if (product.equalsIgnoreCase(stickerPack.identifier)) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product fetched: " + product);
//                        Toast.makeText(getApplicationContext(), "Product fetched: " + product, Toast.LENGTH_SHORT).show();

                        //TODO - do something
                        Log.d("BillingConnector", "Product price: " + price);
//                        Toast.makeText(getApplicationContext(), "Product price: " + price, Toast.LENGTH_SHORT).show();
                    }

                    //TODO - similarly check for other ids

                    fetchedProductInfoList.add(productInfo); //check "usefulPublicMethods" to see how to synchronously check a purchase state
                }
            }

            @Override
            public void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases) {
                /*
                 * This will be called even when no purchased products are returned by the API
                 * */

                switch (productType) {
                    case INAPP:
                    case COMBINED:
                        //this will be triggered on activity start
                        //the other two (INAPP and SUBS) will be triggered when the user actually buys a product
                        //TODO - restore purchases
                        //TODO - non-consumable/consumable products
//                        addText.setText("Add to WhatsApp");
//                        addButton.setOnClickListener(v -> addStickerPackToWhatsApp(stickerPack.identifier, stickerPack.name));
                        break;
                    case SUBS:
                        //TODO - subscription products
                        break;
                }

                String product;
                for (PurchaseInfo purchaseInfo : purchases) {
                    product = purchaseInfo.getProduct();

                    if (product.equalsIgnoreCase(stickerPack.identifier)) {
                        //TODO - do something
                        Log.d("BillingConnector", "Purchased product fetched: " + product);
//                        Toast.makeText(getApplicationContext(), "Purchased product fetched: " + product, Toast.LENGTH_SHORT).show();
                        addText.setText("Add to WhatsApp");
                        addButton.setOnClickListener(v -> addStickerPackToWhatsApp(stickerPack.identifier, stickerPack.name));
                    }

                    //TODO - similarly check for other ids
                }
            }

            @Override
            public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
                String product;
                String purchaseToken;

                for (PurchaseInfo purchaseInfo : purchases) {
                    product = purchaseInfo.getProduct();
                    purchaseToken = purchaseInfo.getPurchaseToken();

                    if (product.equalsIgnoreCase(stickerPack.identifier)) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product purchased: " + product);
//                        Toast.makeText(getApplicationContext(), "Product purchased: " + product, Toast.LENGTH_SHORT).show();

                        //TODO - do something
                        Log.d("BillingConnector", "Purchase token: " + purchaseToken);
//                        Toast.makeText(getApplicationContext(), "Purchase token: " + purchaseToken, Toast.LENGTH_SHORT).show();
                        addText.setText("Add to WhatsApp");
                        addButton.setOnClickListener(v -> addStickerPackToWhatsApp(stickerPack.identifier, stickerPack.name));
                    }

                    //TODO - similarly check for other ids

                    purchasedInfoList.add(purchaseInfo); //check "usefulPublicMethods" to see how to acknowledge or consume a purchase manually
                }
            }

            @Override
            public void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase) {
                /*
                 * Grant user entitlement for NON-CONSUMABLE products and SUBSCRIPTIONS here
                 *
                 * Even though onProductsPurchased is triggered when a purchase is successfully made
                 * there might be a problem along the way with the payment and the purchase won't be acknowledged
                 *
                 * Google will refund users purchases that aren't acknowledged in 3 days
                 *
                 * To ensure that all valid purchases are acknowledged the library will automatically
                 * check and acknowledge all unacknowledged products at the startup
                 * */

                String acknowledgedProduct = purchase.getProduct();

                if (acknowledgedProduct.equalsIgnoreCase(stickerPack.identifier)) {
                    //TODO - do something
                    Log.d("BillingConnector", "Acknowledged: " + acknowledgedProduct);
//                    Toast.makeText(getApplicationContext(), "Acknowledged: " + acknowledgedProduct, Toast.LENGTH_SHORT).show();
                    addText.setText("Add to WhatsApp");
                    addButton.setOnClickListener(v -> addStickerPackToWhatsApp(stickerPack.identifier, stickerPack.name));
                }

                //TODO - similarly check for other ids
            }

            @Override
            public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
                /*
                 * Grant user entitlement for CONSUMABLE products here
                 *
                 * Even though onProductsPurchased is triggered when a purchase is successfully made
                 * there might be a problem along the way with the payment and the user will be able consume the product
                 * without actually paying
                 * */

                String consumedProduct = purchase.getProduct();

                if (consumedProduct.equalsIgnoreCase(stickerPack.identifier)) {
                    //TODO - do something
                    Log.d("BillingConnector", "Consumed: " + consumedProduct);
//                    Toast.makeText(getApplicationContext(), "Consumed: " + consumedProduct, Toast.LENGTH_SHORT).show();
                    addText.setText("Add to WhatsApp");
                    addButton.setOnClickListener(v -> addStickerPackToWhatsApp(stickerPack.identifier, stickerPack.name));
                }

                //TODO - similarly check for other ids
            }

            @Override
            public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
                switch (response.getErrorType()) {
                    case CLIENT_NOT_READY:
                        //TODO - client is not ready yet
                        break;
                    case CLIENT_DISCONNECTED:
                        //TODO - client has disconnected
                        break;
                    case PRODUCT_NOT_EXIST:
                        //TODO - product does not exist
                        break;
                    case CONSUME_ERROR:
                        //TODO - error during consumption
                        break;
                    case CONSUME_WARNING:
                        /*
                         * This will be triggered when a consumable purchase has a PENDING state
                         * User entitlement must be granted when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase complete
                         * and to come back later to receive their purchase
                         * */
                        //TODO - warning during consumption
                        break;
                    case ACKNOWLEDGE_ERROR:
                        //TODO - error during acknowledgment
                        break;
                    case ACKNOWLEDGE_WARNING:
                        /*
                         * This will be triggered when a purchase can not be acknowledged because the state is PENDING
                         * A purchase can be acknowledged only when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase complete
                         * and to come back later to receive their purchase
                         * */
                        //TODO - warning during acknowledgment
                        break;
                    case FETCH_PURCHASED_PRODUCTS_ERROR:
                        //TODO - error occurred while querying purchased products
                        break;
                    case BILLING_ERROR:
                        //TODO - error occurred during initialization / querying product details
                        break;
                    case USER_CANCELED:
                        //TODO - user pressed back or canceled a dialog
                        break;
                    case SERVICE_UNAVAILABLE:
                        //TODO - network connection is down
                        break;
                    case BILLING_UNAVAILABLE:
                        //TODO - billing API version is not supported for the type requested
                        break;
                    case ITEM_UNAVAILABLE:
                        //TODO - requested product is not available for purchase
                        break;
                    case DEVELOPER_ERROR:
                        //TODO - invalid arguments provided to the API
                        break;
                    case ERROR:
                        //TODO - fatal error during the API action
                        break;
                    case ITEM_ALREADY_OWNED:
                        //TODO - failure to purchase since item is already owned
                        addText.setText("Add to WhatsApp");
                        addButton.setOnClickListener(v -> addStickerPackToWhatsApp(stickerPack.identifier, stickerPack.name));
                        break;
                    case ITEM_NOT_OWNED:
                        //TODO - failure to consume since item is not owned
                        break;
                }

                Log.d("BillingConnector", "Error type: " + response.getErrorType() +
                        " Response code: " + response.getResponseCode() + " Message: " + response.getDebugMessage()  + " Product: " + stickerPack.identifier );

//                Toast.makeText(getApplicationContext(), "Error type: " + response.getErrorType() +
//                        " Response code: " + response.getResponseCode() + " Message: " + response.getDebugMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void launchInfoActivity(String publisherWebsite, String publisherEmail, String privacyPolicyWebsite, String licenseAgreementWebsite, String trayIconUriString) {
        Intent intent = new Intent(StickerPackDetailsActivity.this, StickerPackInfoActivity.class);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_ID, stickerPack.identifier);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_WEBSITE, publisherWebsite);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_EMAIL, publisherEmail);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_PRIVACY_POLICY, privacyPolicyWebsite);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_LICENSE_AGREEMENT, licenseAgreementWebsite);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_TRAY_ICON, trayIconUriString);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_info && stickerPack != null) {
            Uri trayIconUri = StickerPackLoader.getStickerAssetUri(stickerPack.identifier, stickerPack.trayImageFile);
            launchInfoActivity(stickerPack.publisherWebsite, stickerPack.publisherEmail, stickerPack.privacyPolicyWebsite, stickerPack.licenseAgreementWebsite, trayIconUri.toString());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private final ViewTreeObserver.OnGlobalLayoutListener pageLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            setNumColumns(recyclerView.getWidth() / recyclerView.getContext().getResources().getDimensionPixelSize(R.dimen.sticker_pack_details_image_size));
        }
    };

    private void setNumColumns(int numColumns) {
        if (this.numColumns != numColumns) {
            layoutManager.setSpanCount(numColumns);
            this.numColumns = numColumns;
            if (stickerPreviewAdapter != null) {
                stickerPreviewAdapter.notifyDataSetChanged();
            }
        }
    }

    private final RecyclerView.OnScrollListener dividerScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            updateDivider(recyclerView);
        }

        @Override
        public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
            super.onScrolled(recyclerView, dx, dy);
            updateDivider(recyclerView);
        }

        private void updateDivider(RecyclerView recyclerView) {
            boolean showDivider = recyclerView.computeVerticalScrollOffset() > 0;
            if (divider != null) {
                divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        whiteListCheckAsyncTask = new WhiteListCheckAsyncTask(this);
        whiteListCheckAsyncTask.execute(stickerPack);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (whiteListCheckAsyncTask != null && !whiteListCheckAsyncTask.isCancelled()) {
            whiteListCheckAsyncTask.cancel(true);
        }
    }

    private void updateAddUI(Boolean isWhitelisted) {
        if (isWhitelisted) {
            alreadyAddedText.setVisibility(View.VISIBLE);
            addButton.setVisibility(View.GONE);
            findViewById(R.id.sticker_pack_details_tap_to_preview).setVisibility(View.GONE);
        } else {
            if(stickerPack.price.equals("PAID")){
                addText.setText("Purchase");
            }
            alreadyAddedText.setVisibility(View.GONE);
            findViewById(R.id.sticker_pack_details_tap_to_preview).setVisibility(View.VISIBLE);
        }
    }

    static class WhiteListCheckAsyncTask extends AsyncTask<StickerPack, Void, Boolean> {
        private final WeakReference<StickerPackDetailsActivity> stickerPackDetailsActivityWeakReference;

        WhiteListCheckAsyncTask(StickerPackDetailsActivity stickerPackListActivity) {
            this.stickerPackDetailsActivityWeakReference = new WeakReference<>(stickerPackListActivity);
        }

        @Override
        protected final Boolean doInBackground(StickerPack... stickerPacks) {
            StickerPack stickerPack = stickerPacks[0];
            final StickerPackDetailsActivity stickerPackDetailsActivity = stickerPackDetailsActivityWeakReference.get();
            if (stickerPackDetailsActivity == null) {
                return false;
            }
            return WhitelistCheck.isWhitelisted(stickerPackDetailsActivity, stickerPack.identifier);
        }

        @Override
        protected void onPostExecute(Boolean isWhitelisted) {
            final StickerPackDetailsActivity stickerPackDetailsActivity = stickerPackDetailsActivityWeakReference.get();
            if (stickerPackDetailsActivity != null) {
                stickerPackDetailsActivity.updateAddUI(isWhitelisted);
            }
        }
    }
}
