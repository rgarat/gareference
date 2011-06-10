package com.google.android.apps.analytics;

public class Item
{
  private final String orderId;
  private final String itemSKU;
  private final String itemName;
  private final String itemCategory;
  private final double itemPrice;
  private final long itemCount;

  private Item(Builder builder)
  {
    this.orderId = builder.orderId;
    this.itemSKU = builder.itemSKU;
    this.itemPrice = builder.itemPrice;
    this.itemCount = builder.itemCount;
    this.itemName = builder.itemName;
    this.itemCategory = builder.itemCategory;
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

    public Builder(String orderId, String itemSKU, double itemPrice, long itemCount)
    {
      if ((orderId == null) || (orderId.trim().length() == 0))
        throw new IllegalArgumentException("orderId must not be empty or null");
      if ((itemSKU == null) || (itemSKU.trim().length() == 0))
        throw new IllegalArgumentException("itemSKU must not be empty or null");
      this.orderId = orderId;
      this.itemSKU = itemSKU;
      this.itemPrice = itemPrice;
      this.itemCount = itemCount;
    }

    public Builder setItemName(String itemName)
    {
      this.itemName = itemName;
      return this;
    }

    public Builder setItemCategory(String itemCategory)
    {
      this.itemCategory = itemCategory;
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