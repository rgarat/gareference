package com.google.android.apps.analytics;

import java.util.Locale;

class NetworkRequestUtil
{
  private static final String GOOGLE_ANALYTICS_GIF_PATH = "/__utm.gif";
  private static final String FAKE_DOMAIN_HASH = "999";
  private static final int X10_PROJECT_NAMES = 8;
  private static final int X10_PROJECT_VALUES = 9;
  private static final int X10_PROJECT_SCOPES = 11;

  public static String constructPageviewRequestPath(Event event, String referrer)
  {
    String page = "";
    if (event.action != null)
      page = event.action;
    if (!page.startsWith("/"))
      page = "/" + page;
    page = encode(page);
    String customVariables = getCustomVariableParams(event);
    Locale localLocale = Locale.getDefault();
    StringBuilder builder = new StringBuilder();
    builder.append(GOOGLE_ANALYTICS_GIF_PATH);
    builder.append("?utmwv=4.6ma");
    builder.append("&utmn=").append(event.randomVal);
    if (customVariables.length() > 0)
      builder.append("&utme=").append(customVariables);
    builder.append("&utmcs=UTF-8");
    builder.append(String.format("&utmsr=%dx%d", new Object[] { Integer.valueOf(event.screenWidth), Integer.valueOf(event.screenHeight) }));
    builder.append(String.format("&utmul=%s-%s", new Object[] { localLocale.getLanguage(), localLocale.getCountry() }));
    builder.append("&utmp=").append(page);
    builder.append("&utmac=").append(event.accountId);
    builder.append("&utmcc=").append(getEscapedCookieString(event, referrer));
    return builder.toString();
  }

  public static String constructEventRequestPath(Event event, String paramString)
  {
    Locale localLocale = Locale.getDefault();
    StringBuilder builder = new StringBuilder();
    StringBuilder eventParam = new StringBuilder();
    eventParam.append(String.format("5(%s*%s", new Object[] { encode(event.category), encode(event.action) }));
    if (event.label != null)
      eventParam.append("*").append(encode(event.label));
    eventParam.append(")");
    if (event.value > -1)
      eventParam.append(String.format("(%d)", new Object[] { Integer.valueOf(event.value) }));
    eventParam.append(getCustomVariableParams(event));
    builder.append(GOOGLE_ANALYTICS_GIF_PATH);
    builder.append("?utmwv=4.6ma");
    builder.append("&utmn=").append(event.randomVal);
    builder.append("&utmt=event");
    builder.append("&utme=").append(eventParam.toString());
    builder.append("&utmcs=UTF-8");
    builder.append(String.format("&utmsr=%dx%d", new Object[] { Integer.valueOf(event.screenWidth), Integer.valueOf(event.screenHeight) }));
    builder.append(String.format("&utmul=%s-%s", new Object[] { localLocale.getLanguage(), localLocale.getCountry() }));
    builder.append("&utmac=").append(event.accountId);
    builder.append("&utmcc=").append(getEscapedCookieString(event, paramString));
    return builder.toString();
  }

  private static void appendStringValue(StringBuilder builder, String key, String value)
  {
    builder.append(key).append("=");
    if ((value != null) && (value.trim().length() > 0))
      builder.append(AnalyticsParameterEncoder.encode(value));
  }

  static void appendCurrencyValue(StringBuilder builder, String key, double value)
  {
    builder.append(key).append("=");
    double d = Math.floor(value * 1000000.0D + 0.5D) / 1000000.0D;
    if (d != 0.0D)
      builder.append(Double.toString(d));
  }

  public static String constructTransactionRequestPath(Event event, String referrer)
  {
    StringBuilder builder = new StringBuilder();
    builder.append(GOOGLE_ANALYTICS_GIF_PATH);
    builder.append("?utmwv=4.6ma");
    builder.append("&utmn=").append(event.randomVal);
    builder.append("&utmt=tran");
    Transaction transaction = event.getTransaction();
    if (transaction != null)
    {
      appendStringValue(builder, "&utmtid", transaction.getOrderId());
      appendStringValue(builder, "&utmtst", transaction.getStoreName());
      appendCurrencyValue(builder, "&utmtto", transaction.getTotalCost());
      appendCurrencyValue(builder, "&utmttx", transaction.getTotalTax());
      appendCurrencyValue(builder, "&utmtsp", transaction.getShippingCost());
      appendStringValue(builder, "&utmtci", "");
      appendStringValue(builder, "&utmtrg", "");
      appendStringValue(builder, "&utmtco", "");
    }
    builder.append("&utmac=").append(event.accountId);
    builder.append("&utmcc=").append(getEscapedCookieString(event, referrer));
    return builder.toString();
  }

  public static String constructItemRequestPath(Event event, String referrer)
  {
    StringBuilder builder = new StringBuilder();
    builder.append(GOOGLE_ANALYTICS_GIF_PATH);
    builder.append("?utmwv=4.6ma");
    builder.append("&utmn=").append(event.randomVal);
    builder.append("&utmt=item");
    Item item = event.getItem();
    if (item != null)
    {
      appendStringValue(builder, "&utmtid", item.getOrderId());
      appendStringValue(builder, "&utmipc", item.getItemSKU());
      appendStringValue(builder, "&utmipn", item.getItemName());
      appendStringValue(builder, "&utmiva", item.getItemCategory());
      appendCurrencyValue(builder, "&utmipr", item.getItemPrice());
      builder.append("&utmiqt=");
      if (item.getItemCount() != 0L)
        builder.append(item.getItemCount());
    }
    builder.append("&utmac=").append(event.accountId);
    builder.append("&utmcc=").append(getEscapedCookieString(event, referrer));
    return builder.toString();
  }

  public static String getCustomVariableParams(Event event)
  {
    StringBuilder builder = new StringBuilder();
    CustomVariableBuffer customVariableBuffer = event.getCustomVariableBuffer();
    if (customVariableBuffer == null)
      return "";
    if (!customVariableBuffer.hasCustomVariables())
      return "";
    CustomVariable[] customVariables = customVariableBuffer.getCustomVariableArray();
    createX10Project(customVariables, builder, X10_PROJECT_NAMES);
    createX10Project(customVariables, builder, X10_PROJECT_VALUES);
    createX10Project(customVariables, builder, X10_PROJECT_SCOPES);
    return builder.toString();
  }

  private static void createX10Project(CustomVariable[] customVariables, StringBuilder builder, int X10id)
  {
    int i = 1;
    builder.append(X10id).append("(");
    for (int j = 0; j < customVariables.length; j++)
    {
      if (customVariables[j] == null)
        continue;
      CustomVariable localCustomVariable = customVariables[j];
      if (i == 0)
        builder.append("*");
      else
        i = 0;
      builder.append(localCustomVariable.getIndex()).append("!");
      switch (X10id)
      {
      case X10_PROJECT_NAMES:
        builder.append(x10Escape(encode(localCustomVariable.getName())));
        break;
      case X10_PROJECT_VALUES:
        builder.append(x10Escape(encode(localCustomVariable.getValue())));
        break;
      case X10_PROJECT_SCOPES:
        builder.append(localCustomVariable.getScope());
      case 10:
      }
    }
    builder.append(")");
  }

  private static String x10Escape(String paramString)
  {
    return paramString.replace("'", "'0").replace(")", "'1").replace("*", "'2").replace("!", "'3");
  }

  public static String getEscapedCookieString(Event event, String referrer)
  {
    StringBuilder builder = new StringBuilder();
    builder.append("__utma=");
    builder.append(FAKE_DOMAIN_HASH).append(".");
    builder.append(event.userId).append(".");
    builder.append(event.timestampFirst).append(".");
    builder.append(event.timestampPrevious).append(".");
    builder.append(event.timestampCurrent).append(".");
    builder.append(event.visits);
    if (referrer != null)
    {
      builder.append("+__utmz=");
      builder.append(FAKE_DOMAIN_HASH).append(".");
      builder.append(event.timestampFirst).append(".");
      builder.append("1.1.");
      builder.append(referrer);
    }
    return encode(builder.toString());
  }

  private static String encode(String paramString)
  {
    return AnalyticsParameterEncoder.encode(paramString);
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.NetworkRequestUtil
 * JD-Core Version:    0.6.0
 */