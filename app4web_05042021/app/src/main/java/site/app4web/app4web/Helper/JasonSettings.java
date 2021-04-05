package site.app4web.app4web.Helper;

import android.content.Context;

import site.app4web.app4web.R;

public class JasonSettings {
    public static boolean isAsync(String className, Context context) {
        String[]asyncActions = context.getResources().getStringArray(R.array.asyncActions);
        for(String s: asyncActions){
            if(s.equalsIgnoreCase(className)) return true;
        }
        return false;
    }
}
