package piuk.blockchain.android.ui.customviews;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.ArrayList;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.ViewUtils;
import piuk.blockchain.android.util.annotations.Thunk;

@SuppressWarnings("WeakerAccess")
public class CustomKeypad extends LinearLayout implements View.OnClickListener {

    private ArrayList<EditText> viewList;
    private String decimalSeparator = ".";
    @Thunk TableLayout numpad;
    @Thunk CustomKeypadCallback callback;

    public CustomKeypad(Context context) {
        super(context);
        init();
    }

    public CustomKeypad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomKeypad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomKeypad(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.BOTTOM);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_numeric_keyboard, this, true);

        numpad = (TableLayout) findViewById(R.id.numericPad);
        numpad.findViewById(R.id.button1).setOnClickListener(this);
        numpad.findViewById(R.id.button2).setOnClickListener(this);
        numpad.findViewById(R.id.button3).setOnClickListener(this);
        numpad.findViewById(R.id.button4).setOnClickListener(this);
        numpad.findViewById(R.id.button5).setOnClickListener(this);
        numpad.findViewById(R.id.button6).setOnClickListener(this);
        numpad.findViewById(R.id.button7).setOnClickListener(this);
        numpad.findViewById(R.id.button8).setOnClickListener(this);
        numpad.findViewById(R.id.button9).setOnClickListener(this);
        numpad.findViewById(R.id.button10).setOnClickListener(this);
        numpad.findViewById(R.id.button0).setOnClickListener(this);
        numpad.findViewById(R.id.buttonDeleteBack).setOnClickListener(this);
        numpad.findViewById(R.id.buttonDone).setOnClickListener(this);

        viewList = new ArrayList<>();
    }

    public void enableOnView(final EditText view) {

        if (!viewList.contains(view)) viewList.add(view);

        view.setTextIsSelectable(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                View view1 = ((Activity) getContext()).getCurrentFocus();
                if (view1 != null) {
                    InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(view1.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                setNumpadVisibility(View.VISIBLE);
            }
        });
        view.setOnClickListener(v -> {
            ((TextView) numpad.findViewById(R.id.decimal_point)).setText(decimalSeparator);
            setNumpadVisibility(View.VISIBLE);
        });
    }

    public void setCallback(CustomKeypadCallback callback) {
        this.callback = callback;
    }

    public void setNumpadVisibility(@ViewUtils.Visibility int visibility) {
        if (visibility == View.VISIBLE) {
            showKeyboard();
        } else {
            hideKeyboard();
        }
    }

    private void showKeyboard() {
        if (!isVisible()) {
            Animation bottomUp = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_up);
            startAnimation(bottomUp);
            bottomUp.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (callback != null) callback.onKeypadOpenCompleted();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            setVisibility(View.VISIBLE);
            if (callback != null) callback.onKeypadOpen();
        }
    }

    private void hideKeyboard() {
        if (isVisible()) {
            Animation topDown = AnimationUtils.loadAnimation(getContext(), R.anim.top_down);
            startAnimation(topDown);
            setVisibility(View.GONE);
            if (callback != null) callback.onKeypadClose();
        }
    }

    @Override
    public void onClick(View v) {

        String pad = "";
        switch (v.getId()) {
            case R.id.button10:
                pad = decimalSeparator;
                break;
            case R.id.buttonDeleteBack:
                deleteFromFocusedView();
                return;
            case R.id.buttonDone:
                setNumpadVisibility(View.GONE);
                break;
            default:
                pad = v.getTag().toString().substring(0, 1);
                break;
        }

        // Append tapped #
        if (pad != null) {
            appendToFocusedView(pad);
        }
    }

    private void appendToFocusedView(String pad) {
        for (final EditText view : viewList) {
            if (view.hasFocus()) {

                //Don't allow multiple decimals
                if (pad.equals(decimalSeparator) && view.getText().toString().contains(decimalSeparator))
                    continue;

                int startSelection = view.getSelectionStart();
                int endSelection = view.getSelectionEnd();
                if (endSelection - startSelection > 0) {
                    String selectedText = view.getText().toString().substring(startSelection, endSelection);
                    view.setText(view.getText().toString().replace(selectedText, pad));
                } else {
                    view.append(pad);
                }

                if (view.getText().length() > 0) {
                    view.post(() -> view.setSelection(view.getText().toString().length()));
                }
            }
        }
    }

    private void deleteFromFocusedView() {
        for (final EditText view : viewList) {
            if (view.hasFocus() && view.getText().length() > 0) {
                view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
        }
    }

    public void setDecimalSeparator(String passedDecimalSeparator) {
        decimalSeparator = passedDecimalSeparator;
    }

    public boolean isVisible() {
        return (getVisibility() == View.VISIBLE);
    }
}
