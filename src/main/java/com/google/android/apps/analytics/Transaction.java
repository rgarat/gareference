package com.google.android.apps.analytics;

public class Transaction
{
  private final String orderId;
  private final String storeName;
  private final double totalCost;
  private final double totalTax;
  private final double shippingCost;

  private Transaction(Builder paramBuilder)
  {
    this.orderId = paramBuilder.orderId;
    this.totalCost = paramBuilder.totalCost;
    this.storeName = paramBuilder.storeName;
    this.totalTax = paramBuilder.totalTax;
    this.shippingCost = paramBuilder.shippingCost;
  }

  String getOrderId()
  {
    return this.orderId;
  }

  String getStoreName()
  {
    return this.storeName;
  }

  double getTotalCost()
  {
    return this.totalCost;
  }

  double getTotalTax()
  {
    return this.totalTax;
  }

  double getShippingCost()
  {
    return this.shippingCost;
  }

  public static class Builder
  {
    private final String orderId;
    private String storeName = null;
    private final double totalCost;
    private double totalTax = 0.0D;
    private double shippingCost = 0.0D;

    public Builder(String paramString, double paramDouble)
    {
      if ((paramString == null) || (paramString.trim().length() == 0))
        throw new IllegalArgumentException("orderId must not be empty or null");
      this.orderId = paramString;
      this.totalCost = paramDouble;
    }

    public Builder setStoreName(String paramString)
    {
      this.storeName = paramString;
      return this;
    }

    public Builder setTotalTax(double paramDouble)
    {
      this.totalTax = paramDouble;
      return this;
    }

    public Builder setShippingCost(double paramDouble)
    {
      this.shippingCost = paramDouble;
      return this;
    }

    public Transaction build()
    {
      return new Transaction(this);
    }
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.Transaction
 * JD-Core Version:    0.6.0
 */