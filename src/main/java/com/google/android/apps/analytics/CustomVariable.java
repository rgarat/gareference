package com.google.android.apps.analytics;

class CustomVariable
{
  public static final int MAX_CUSTOM_VARIABLES = 5;
  public static final String INDEX_ERROR_MSG = "Index must be between 1 and 5 inclusive.";
  public static final int VISITOR_SCOPE = 1;
  public static final int SESSION_SCOPE = 2;
  public static final int PAGE_SCOPE = 3;
  public static final int MAX_CUSTOM_VARIABLE_LENGTH = 64;
  private final int scope;
  private final String name;
  private final String value;
  private final int index;

  public CustomVariable(int paramInt1, String paramString1, String paramString2, int paramInt2)
  {
    if ((paramInt2 != 1) && (paramInt2 != 2) && (paramInt2 != 3))
      throw new IllegalArgumentException("Invalid Scope:" + paramInt2);
    if ((paramInt1 < 1) || (paramInt1 > 5))
      throw new IllegalArgumentException("Index must be between 1 and 5 inclusive.");
    if ((paramString1 == null) || (paramString1.length() == 0))
      throw new IllegalArgumentException("Invalid argument for name:  Cannot be empty");
    if ((paramString2 == null) || (paramString2.length() == 0))
      throw new IllegalArgumentException("Invalid argument for value:  Cannot be empty");
    int i = 0;
    i = AnalyticsParameterEncoder.encode(paramString1 + paramString2).length();
    if (i > 64)
      throw new IllegalArgumentException("Encoded form of name and value must not exceed 64 characters combined.  Character length: " + i);
    this.index = paramInt1;
    this.scope = paramInt2;
    this.name = paramString1;
    this.value = paramString2;
  }

  public CustomVariable(int paramInt, String paramString1, String paramString2)
  {
    this(paramInt, paramString1, paramString2, 3);
  }

  public int getScope()
  {
    return this.scope;
  }

  public String getName()
  {
    return this.name;
  }

  public String getValue()
  {
    return this.value;
  }

  public int getIndex()
  {
    return this.index;
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.CustomVariable
 * JD-Core Version:    0.6.0
 */