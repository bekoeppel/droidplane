package ch.benediktkoeppel.code.droidplane.helper;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

public class AndroidHelper {

    public static <T extends Activity> T getActivity(Context context, Class<T> clazz) {
        while (context instanceof ContextWrapper) {
            if (clazz.isInstance(context)) {
                return clazz.cast(context);
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }
}
