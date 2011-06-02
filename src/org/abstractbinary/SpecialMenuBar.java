import android.content.Context;
import android.view.View;

public class SpecialMenuBar extends View {
    private static int STATE_NOT_LOGGED_IN = 0;
    private int state;

    public SpecialMenuBar(Context context) {
        super(context);

        state = STATE_NOT_LOGGED_IN;
    }
}
