package com.google.android.apps.analytics;

import java.util.Locale;

class NetworkRequestUtil
{
  private static final String GOOGLE_ANALYTICS_GIF_PATH = "/__utm.gif";
  private static final String FAKE_DOMAIN_HASH = "999";
  private static final int X10_PROJECT_NAMES = 8;
  private static final int X10_PROJECT_VALUES = 9;
  private static final int X10_PROJECT_SCOPES = 11;

  public static String constructPageviewRequestPath(Event paramEvent, String paramString)
  {
    String str1 = "";
    if (paramEvent.action != null)
      str1 = paramEvent.action;
    if (!str1.startsWith("/"))
      str1 = "/" + str1;
    str1 = encode(str1);
    String str2 = getCustomVariableParams(paramEvent);
    Locale localLocale = Locale.getDefault();
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("/__utm.gif");
    localStringBuilder.append("?utmwv=4.6ma");
    localStringBuilder.append("&utmn=").append(paramEvent.randomVal);
    if (str2.length() > 0)
      localStringBuilder.append("&utme=").append(str2);
    localStringBuilder.append("&utmcs=UTF-8");
    localStringBuilder.append(String.format("&utmsr=%dx%d", new Object[] { Integer.valueOf(paramEvent.screenWidth), Integer.valueOf(paramEvent.screenHeight) }));
    localStringBuilder.append(String.format("&utmul=%s-%s", new Object[] { localLocale.getLanguage(), localLocale.getCountry() }));
    localStringBuilder.append("&utmp=").append(str1);
    localStringBuilder.append("&utmac=").append(paramEvent.accountId);
    localStringBuilder.append("&utmcc=").append(getEscapedCookieString(paramEvent, paramString));
    return localStringBuilder.toString();
  }

  public static String constructEventRequestPath(Event paramEvent, String paramString)
  {
    Locale localLocale = Locale.getDefault();
    StringBuilder localStringBuilder1 = new StringBuilder();
    StringBuilder localStringBuilder2 = new StringBuilder();
    localStringBuilder2.append(String.format("5(%s*%s", new Object[] { encode(paramEvent.category), encode(paramEvent.action) }));
    if (paramEvent.label != null)
      localStringBuilder2.append("*").append(encode(paramEvent.label));
    localStringBuilder2.append(")");
    if (paramEvent.value > -1)
      localStringBuilder2.append(String.format("(%d)", new Object[] { Integer.valueOf(paramEvent.value) }));
    localStringBuilder2.append(getCustomVariableParams(paramEvent));
    localStringBuilder1.append("/__utm.gif");
    localStringBuilder1.append("?utmwv=4.6ma");
    localStringBuilder1.append("&utmn=").append(paramEvent.randomVal);
    localStringBuilder1.append("&utmt=event");
    localStringBuilder1.append("&utme=").append(localStringBuilder2.toString());
    localStringBuilder1.append("&utmcs=UTF-8");
    localStringBuilder1.append(String.format("&utmsr=%dx%d", new Object[] { Integer.valueOf(paramEvent.screenWidth), Integer.valueOf(paramEvent.screenHeight) }));
    localStringBuilder1.append(String.format("&utmul=%s-%s", new Object[] { localLocale.getLanguage(), localLocale.getCountry() }));
    localStringBuilder1.append("&utmac=").append(paramEvent.accountId);
    localStringBuilder1.append("&utmcc=").append(getEscapedCookieString(paramEvent, paramString));
    return localStringBuilder1.toString();
  }

  private static void appendStringValue(StringBuilder paramStringBuilder, String paramString1, String paramString2)
  {
    paramStringBuilder.append(paramString1).append("=");
    if ((paramString2 != null) && (paramString2.trim().length() > 0))
      paramStringBuilder.append(AnalyticsParameterEncoder.encode(paramString2));
  }

  static void appendCurrencyValue(StringBuilder paramStringBuilder, String paramString, double paramDouble)
  {
    paramStringBuilder.append(paramString).append("=");
    double d = Math.floor(paramDouble * 1000000.0D + 0.5D) / 1000000.0D;
    if (d != 0.0D)
      paramStringBuilder.append(Double.toString(d));
  }

  public static String constructTransactionRequestPath(Event paramEvent, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("/__utm.gif");
    localStringBuilder.append("?utmwv=4.6ma");
    localStringBuilder.append("&utmn=").append(paramEvent.randomVal);
    localStringBuilder.append("&utmt=tran");
    Transaction localTransaction = paramEvent.getTransaction();
    if (localTransaction != null)
    {
      appendStringValue(localStringBuilder, "&utmtid", localTransaction.getOrderId());
      appendStringValue(localStringBuilder, "&utmtst", localTransaction.getStoreName());
      appendCurrencyValue(localStringBuilder, "&utmtto", localTransaction.getTotalCost());
      appendCurrencyValue(localStringBuilder, "&utmttx", localTransaction.getTotalTax());
      appendCurrencyValue(localStringBuilder, "&utmtsp", localTransaction.getShippingCost());
      appendStringValue(localStringBuilder, "&utmtci", "");
      appendStringValue(localStringBuilder, "&utmtrg", "");
      appendStringValue(localStringBuilder, "&utmtco", "");
    }
    localStringBuilder.append("&utmac=").append(paramEvent.accountId);
    localStringBuilder.append("&utmcc=").append(getEscapedCookieString(paramEvent, paramString));
    return localStringBuilder.toString();
  }

  public static String constructItemRequestPath(Event paramEvent, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("/__utm.gif");
    localStringBuilder.append("?utmwv=4.6ma");
    localStringBuilder.append("&utmn=").append(paramEvent.randomVal);
    localStringBuilder.append("&utmt=item");
    Item localItem = paramEvent.getItem();
    if (localItem != null)
    {
      appendStringValue(localStringBuilder, "&utmtid", localItem.getOrderId());
      appendStringValue(localStringBuilder, "&utmipc", localItem.getItemSKU());
      appendStringValue(localStringBuilder, "&utmipn", localItem.getItemName());
      appendStringValue(localStringBuilder, "&utmiva", localItem.getItemCategory());
      appendCurrencyValue(localStringBuilder, "&utmipr", localItem.getItemPrice());
      localStringBuilder.append("&utmiqt=");
      if (localItem.getItemCount() != 0L)
        localStringBuilder.append(localItem.getItemCount());
    }
    localStringBuilder.append("&utmac=").append(paramEvent.accountId);
    localStringBuilder.append("&utmcc=").append(getEscapedCookieString(paramEvent, paramString));
    return localStringBuilder.toString();
  }

  public static String getCustomVariableParams(Event paramEvent)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    CustomVariableBuffer localCustomVariableBuffer = paramEvent.getCustomVariableBuffer();
    if (localCustomVariableBuffer == null)
      return "";
    if (!localCustomVariableBuffer.hasCustomVariables())
      return "";
    CustomVariable[] arrayOfCustomVariable = localCustomVariableBuffer.getCustomVariableArray();
    createX10Project(arrayOfCustomVariable, localStringBuilder, 8);
    createX10Project(arrayOfCustomVariable, localStringBuilder, 9);
    createX10Project(arrayOfCustomVariable, localStringBuilder, 11);
    return localStringBuilder.toString();
  }

  private static void createX10Project(CustomVariable[] paramArrayOfCustomVariable, StringBuilder paramStringBuilder, int paramInt)
  {
    int i = 1;
    paramStringBuilder.append(paramInt).append("(");
    for (int j = 0; j < paramArrayOfCustomVariable.length; j++)
    {
      if (paramArrayOfCustomVariable[j] == null)
        continue;
      CustomVariable localCustomVariable = paramArrayOfCustomVariable[j];
      if (i == 0)
        paramStringBuilder.append("*");
      else
        i = 0;
      paramStringBuilder.append(localCustomVariable.getIndex()).append("!");
      switch (paramInt)
      {
      case 8:
        paramStringBuilder.append(x10Escape(encode(localCustomVariable.getName())));
        break;
      case 9:
        paramStringBuilder.append(x10Escape(encode(localCustomVariable.getValue())));
        break;
      case 11:
        paramStringBuilder.append(localCustomVariable.getScope());
      case 10:
      }
    }
    paramStringBuilder.append(")");
  }

  private static String x10Escape(String paramString)
  {
    return paramString.replace("'", "'0").replace(")", "'1").replace("*", "'2").replace("!", "'3");
  }

  public static String getEscapedCookieString(Event paramEvent, String paramString)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append("__utma=");
    localStringBuilder.append("999").append(".");
    localStringBuilder.append(paramEvent.userId).append(".");
    localStringBuilder.append(paramEvent.timestampFirst).append(".");
    localStringBuilder.append(paramEvent.timestampPrevious).append(".");
    localStringBuilder.append(paramEvent.timestampCurrent).append(".");
    localStringBuilder.append(paramEvent.visits);
    if (paramString != null)
    {
      localStringBuilder.append("+__utmz=");
      localStringBuilder.append("999").append(".");
      localStringBuilder.append(paramEvent.timestampFirst).append(".");
      localStringBuilder.append("1.1.");
      localStringBuilder.append(paramString);
    }
    return encode(localStringBuilder.toString());
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