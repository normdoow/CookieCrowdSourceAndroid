package shinzzerz.cookiecrowdsource;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

/**
 * Created by noahbragg on 11/22/17.
 */

public class EditTextPopUpKeyboard extends AppCompatEditText {

    public EditTextPopUpKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        addKeyboardOptions();
    }
    public EditTextPopUpKeyboard(Context context) {
        super(context);
        addKeyboardOptions();
    }

    public EditTextPopUpKeyboard(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        addKeyboardOptions();
    }

    private void addKeyboardOptions() {
        this.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI|EditorInfo.IME_ACTION_DONE);
    }
}
