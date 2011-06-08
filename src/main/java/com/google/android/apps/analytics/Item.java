package com.google.android.apps.analytics;

public class Item
{
  private final String orderId;
  private final String itemSKU;
  private final String itemName;
  private final String itemCategory;
  private final double itemPrice;
  private final long itemCount;

  private Item(Builder paramBuilder)
  {
    this.orderId = paramBuilder.orderId;
    this.itemSKU = paramBuilder.itemSKU;
    this.itemPrice = paramBuilder.itemPrice;
    this.itemCount = paramBuilder.itemCount;
    this.itemName = paramBuilder.itemName;
    this.itemCategory = paramBuilder.itemCategory;
  }

  String getOrderId()
  {
    return this.orderId;
  }

  String getItemSKU()
  {
    return this.itemSKU;
  }

  String getItemName()
  {
    return this.itemName;
  }

  String getItemCategory()
  {
    return this.itemCategory;
  }

  double getItemPrice()
  {
    return this.itemPrice;
  }

  long getItemCount()
  {
    return this.itemCount;
  }

  public static class Builder
  {
    private final String orderId;
    private final String itemSKU;
    private final double itemPrice;
    private final long itemCount;
    private String itemName = null;
    private String itemCategory = null;

    public Builder(String paramString1, String paramString2, double paramDouble, long paramLong)
    {
      if ((paramString1 == null) || (paramString1.trim().length() == 0))
        throw new IllegalArgumentException("orderId must not be empty or null");
      if ((paramString2 == null) || (paramString2.trim().length() == 0))
        throw new IllegalArgumentException("itemSKU must not be empty or null");
      this.orderId = paramString1;
      this.itemSKU = paramString2;
      this.itemPrice = paramDouble;
      this.itemCount = paramLong;
    }

    public Builder setItemName(String paramString)
    {
      this.itemName = paramString;
      return this;
    }

    public Builder setItemCategory(String paramString)
    {
      this.itemCategory = paramString;
      return this;
    }

    public Item build()
    {
      return new Item(this);
    }
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.Item
 * JD-Core Version:    0.6.0
 */