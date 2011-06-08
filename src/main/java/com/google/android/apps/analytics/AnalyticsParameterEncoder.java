package com.google.android.apps.analytics;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AnalyticsParameterEncoder
{
  public static String encode(String paramString)
  {
    return encode(paramString, "UTF-8");
  }

  static String encode(String paramString1, String paramString2)
  {
    try
    {
      return URLEncoder.encode(paramString1, paramString2).replace("+", "%20");
    }
    catch (UnsupportedEncodingException localUnsupportedEncodingException)
    {
    }
    throw new AssertionError("URL encoding failed for: " + paramString1);
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.AnalyticsParameterEncoder
 * JD-Core Version:    0.6.0
 */