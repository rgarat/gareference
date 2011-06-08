package com.google.android.apps.analytics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.HashMap;

public class AnalyticsReceiver extends BroadcastReceiver
{
  private static final String INSTALL_ACTION = "com.android.vending.INSTALL_REFERRER";

  public void onReceive(Context paramContext, Intent paramIntent)
  {
    String str1 = paramIntent.getStringExtra("referrer");
    if ((!"com.android.vending.INSTALL_REFERRER".equals(paramIntent.getAction())) || (str1 == null))
      return;
    String str2 = formatReferrer(str1);
    if (str2 != null)
    {
      PersistentEventStore localPersistentEventStore = new PersistentEventStore(new PersistentEventStore.DataBaseHelper(paramContext));
      localPersistentEventStore.setReferrer(str2);
      Log.d("GoogleAnalyticsTracker", "Stored referrer:" + str2);
    }
    else
    {
      Log.w("GoogleAnalyticsTracker", "Badly formatted referrer, ignored");
    }
  }

  static String formatReferrer(String paramString)
  {
    String[] arrayOfString1 = paramString.split("&");
    HashMap<String,String> localHashMap = new HashMap();
    for (String localObject1 : arrayOfString1)
    {
      String[] localObject2 = (localObject1).split("=");
      if (localObject2.length != 2)
        break;
      localHashMap.put(localObject2[0], localObject2[1]);
    }
    int i = localHashMap.get("utm_campaign") != null ? 1 : 0;
    int medium = localHashMap.get("utm_medium") != null ? 1 : 0;
    int source = localHashMap.get("utm_source") != null ? 1 : 0;
    if ((i == 0) || (medium == 0) || (source == 0))
    {
      Log.w("GoogleAnalyticsTracker", "Badly formatted referrer missing campaign, name or source");
      return null;
    }
    String[][] localObject1 = { { "utmcid", (String)localHashMap.get("utm_id") }, { "utmcsr", (String)localHashMap.get("utm_source") }, { "utmgclid", (String)localHashMap.get("gclid") }, { "utmccn", (String)localHashMap.get("utm_campaign") }, { "utmcmd", (String)localHashMap.get("utm_medium") }, { "utmctr", (String)localHashMap.get("utm_term") }, { "utmcct", (String)localHashMap.get("utm_content") } };
    Object localObject2 = new StringBuilder();
    int m = 1;
    for (int n = 0; n < localObject1.length; n++)
    {
      if (localObject1[n][1] == null)
        continue;
      String str = localObject1[n][1].replace("+", "%20");
      str = str.replace(" ", "%20");
      if (m != 0)
        m = 0;
      else
        ((StringBuilder)localObject2).append("|");
      ((StringBuilder)localObject2).append(localObject1[n][0]).append("=").append(str);
    }
    return (String)(String)((StringBuilder)localObject2).toString();
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.AnalyticsReceiver
 * JD-Core Version:    0.6.0
 */