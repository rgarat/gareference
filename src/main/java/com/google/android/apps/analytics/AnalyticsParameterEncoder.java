package com.google.android.apps.analytics;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AnalyticsParameterEncoder
{
  public static String encode(String string)
  {
    return encode(string, "UTF-8");
  }

  static String encode(String string, String characterEncoding)
  {
    try
    {
      return URLEncoder.encode(string, characterEncoding).replace("+", "%20");
    }
    catch (UnsupportedEncodingException localUnsupportedEncodingException)
    {
    }
    throw new AssertionError("URL encoding failed for: " + string);
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.AnalyticsParameterEncoder
 * JD-Core Version:    0.6.0
 */