package piuk.blockchain.android.ui.customviews;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import piuk.blockchain.android.R;

public class PinEntryKeypad extends LinearLayout implements View.OnClickListener {

    private OnPinEntryPadClickedListener listener;

    public PinEntryKeypad(Context context) {
        super(context);
        init();
    }

    public PinEntryKeypad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PinEntryKeypad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PinEntryKeypad(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.BOTTOM);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.pin_entry_keyboard, this, true);

        LinearLayout keypad = (LinearLayout) findViewById(R.id.numericPad);

        keypad.findViewById(R.id.button0).setOnClickListener(this);
        keypad.findViewById(R.id.button1).setOnClickListener(this);
        keypad.findViewById(R.id.button2).setOnClickListener(this);
        keypad.findViewById(R.id.button3).setOnClickListener(this);
        keypad.findViewById(R.id.button4).setOnClickListener(this);
        keypad.findViewById(R.id.button5).setOnClickListener(this);
        keypad.findViewById(R.id.button6).setOnClickListener(this);
        keypad.findViewById(R.id.button7).setOnClickListener(this);
        keypad.findViewById(R.id.button8).setOnClickListener(this);
        keypad.findViewById(R.id.button9).setOnClickListener(this);
        keypad.findViewById(R.id.buttonDeleteBack).setOnClickListener(this);
    }

    public void setPadClickedListener(OnPinEntryPadClickedListener listener) {
        this.listener = listener;
    }

    public void padClicked(View view) {
        if (listener != null) {
            listener.onNumberClicked(view.getTag().toString().substring(0, 1));
        }
    }

    public void deleteClicked() {
        if (listener != null) {
            listener.onDeleteClicked();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button0:
            case R.id.button1:
            case R.id.button2:
            case R.id.button3:
            case R.id.button4:
            case R.id.button5:
            case R.id.button6:
            case R.id.button7:
            case R.id.button8:
            case R.id.button9:
                padClicked(v);
                break;
            case R.id.buttonDeleteBack:
                deleteClicked();
                break;
            default:
                break;
        }
    }

    public interface OnPinEntryPadClickedListener {

        void onNumberClicked(String number);

        void onDeleteClicked();

    }
}
