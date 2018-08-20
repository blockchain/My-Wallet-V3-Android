package piuk.blockchain.android.ui.contacts.list;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity;
import piuk.blockchain.androidcoreui.ui.base.UiState;
import piuk.blockchain.android.ui.contacts.detail.ContactDetailActivity;
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.androidcoreui.utils.ViewUtils;

import static piuk.blockchain.androidcoreui.ui.base.UiState.CONTENT;
import static piuk.blockchain.androidcoreui.ui.base.UiState.EMPTY;
import static piuk.blockchain.androidcoreui.ui.base.UiState.FAILURE;
import static piuk.blockchain.androidcoreui.ui.base.UiState.LOADING;


public class ContactsListActivity extends BaseMvpActivity<ContactsListView, ContactsListPresenter>
        implements ContactsListView, ContactsListAdapter.ContactsClickListener {

    public static final String EXTRA_METADATA_URI = "metadata_uri";
    public static final String KEY_BUNDLE_CONTACT_ID = "contact_id";

    private static final int REQUEST_PAIRING = 98;
    private static final int NAME_MAX_LENGTH = 17;
    private final int CHOOSER_REQUEST = 3510;

    @Inject
    ContactsListPresenter contactsListPresenter;
    private ContactsListAdapter contactsListAdapter;
    private MaterialProgressDialog progressDialog;

    private SwipeRefreshLayout swipeRefreshLayout;
    private View fab;
    private View layoutFailure;
    private View layoutNoContacts;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        setupToolbar(findViewById(R.id.toolbar_general), R.string.contacts_title);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Buttons
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> showRecipientNameDialog());
        findViewById(R.id.buttonRetry).setOnClickListener(view -> getPresenter().onViewReady());
        // Swipe to refresh layout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_blue_accent);
        swipeRefreshLayout.setOnRefreshListener(() -> getPresenter().onViewReady());
        // Contacts list
        contactsListAdapter = new ContactsListAdapter(new ArrayList<>(), new StringUtils(this));
        contactsListAdapter.setContactsClickListener(this);
        final RecyclerView recyclerView = findViewById(R.id.recyclerviewContacts);
        recyclerView.setAdapter(contactsListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.buttonInviteContact).setOnClickListener(view -> showRecipientNameDialog());

        layoutFailure = findViewById(R.id.layoutFailure);
        layoutNoContacts = findViewById(R.id.layoutNoContacts);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onViewReady();
    }

    @Override
    public void onContactsLoaded(@NonNull List<ContactsListItem> contacts) {
        contactsListAdapter.onContactsUpdated(contacts);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    /**
     * Static method to assist with launching this activity
     */
    public static void start(Context context, @Nullable Bundle extras) {
        Intent starter = new Intent(context, ContactsListActivity.class);
        if (extras != null) starter.putExtras(extras);
        context.startActivity(starter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PAIRING && resultCode == RESULT_OK) {
            getPresenter().onViewReady();
        } else if (requestCode == CHOOSER_REQUEST) {
            AccessState.getInstance().enableAutoLogout();
        }
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showSecondPasswordDialog() {
        AppCompatEditText editText = new AppCompatEditText(this);
        editText.setHint(R.string.password);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        FrameLayout frameLayout = ViewUtils.getAlertDialogPaddedView(this, editText);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_second_password_prompt)
                .setView(frameLayout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> getPresenter().decryptHDWalletAndinitContactsService(editText.getText().toString()))
                .create()
                .show();
    }

    @Override
    public void showProgressDialog() {
        progressDialog = new MaterialProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(R.string.please_wait);
        progressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void setUiState(@UiState.UiStateDef int uiState) {
        switch (uiState) {
            case LOADING:
                swipeRefreshLayout.setRefreshing(true);
                layoutFailure.setVisibility(View.GONE);
                layoutNoContacts.setVisibility(View.GONE);
                fab.setVisibility(View.GONE);
                break;
            case CONTENT:
                swipeRefreshLayout.setRefreshing(false);
                swipeRefreshLayout.setVisibility(View.VISIBLE);
                layoutFailure.setVisibility(View.GONE);
                layoutNoContacts.setVisibility(View.GONE);
                fab.setVisibility(View.VISIBLE);
                break;
            case FAILURE:
                swipeRefreshLayout.setRefreshing(false);
                swipeRefreshLayout.setVisibility(View.GONE);
                layoutFailure.setVisibility(View.VISIBLE);
                layoutNoContacts.setVisibility(View.GONE);
                fab.setVisibility(View.GONE);
                break;
            case EMPTY:
                swipeRefreshLayout.setRefreshing(false);
                swipeRefreshLayout.setVisibility(View.GONE);
                layoutFailure.setVisibility(View.GONE);
                layoutNoContacts.setVisibility(View.VISIBLE);
                fab.setVisibility(View.GONE);
                break;
        }
    }

    public void showRecipientNameDialog() {
        AppCompatEditText editText = getNameEditText();

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_invite_contact)
                .setMessage(R.string.contacts_who_are_you_inviting_header)
                .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
                .setCancelable(false)
                .setPositiveButton(R.string.next, null)
                .setNegativeButton(R.string.cancel, null).create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String name = editText.getText().toString();
                if (name.isEmpty()) {
                    editText.setError(getString(R.string.contacts_field_error_empty));
                } else {
                    getPresenter().setNameOfRecipient(name);
                    showSenderNameDialog();
                    dialog.dismiss();
                }
            });
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(view -> {
                getPresenter().clearContactNames();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    @Override
    public void showSenderNameDialog() {
        AppCompatEditText editText = getNameEditText();

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_invite_contact)
                .setMessage(getString(R.string.contacts_how_are_you_known_header, getPresenter().getRecipient()))
                .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
                .setCancelable(false)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null).create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String name = editText.getText().toString();
                if (name.isEmpty()) {
                    editText.setError(getString(R.string.contacts_field_error_empty));
                } else {
                    getPresenter().setNameOfSender(name);
                    getPresenter().createLink();
                    dialog.dismiss();
                }
            });
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(view -> {
                getPresenter().clearContactNames();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    @NonNull
    private AppCompatEditText getNameEditText() {
        AppCompatEditText editText = new AppCompatEditText(this);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(NAME_MAX_LENGTH)});
        editText.setHint(R.string.name);
        return editText;
    }

    @Override
    public void onLinkGenerated(Intent intent) {
        AccessState.getInstance().disableAutoLogout();
        startActivityForResult(Intent.createChooser(intent, getString(R.string.contacts_share_invitation)), CHOOSER_REQUEST);
    }

    @Override
    protected ContactsListPresenter createPresenter() {
        return contactsListPresenter;
    }

    @Override
    protected ContactsListView getView() {
        return this;
    }

    @Override
    public void onContactClick(String id) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_BUNDLE_CONTACT_ID, id);
        ContactDetailActivity.start(this, bundle);
    }

    @Override
    public void onMoreClick(String id) {
        CharSequence actions[] = new CharSequence[]{getString(R.string.contacts_resend_invite), getString(R.string.contacts_delete)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(actions, (dialog, which) -> {
            // the user clicked on colors[which]
            switch (which) {
                case 0:
                    getPresenter().resendInvite(id);
                    break;
                case 1:
                    showDeleteUserConfirmationDialog(id);
                    break;
            }
        });
        builder.show();
    }

    public void showDeleteUserConfirmationDialog(String id) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.contacts_delete) + "?")
                .setMessage(R.string.contacts_delete_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> getPresenter().onDeleteContactConfirmed(id))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }
}
