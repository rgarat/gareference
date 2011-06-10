package com.google.android.apps.analytics;

class Event
{
  static final String PAGEVIEW_EVENT_CATEGORY = "__##GOOGLEPAGEVIEW##__";
  static final String INSTALL_EVENT_CATEGORY = "__##GOOGLEINSTALL##__";
  static final String TRANSACTION_CATEGORY = "__##GOOGLETRANSACTION##__";
  static final String ITEM_CATEGORY = "__##GOOGLEITEM##__";
  final long eventId;
  final int userId;
  final String accountId;
  final int randomVal;
  final int timestampFirst;
  final int timestampPrevious;
  final int timestampCurrent;
  final int visits;
  final String category;
  final String action;
  final String label;
  final int value;
  final int screenWidth;
  final int screenHeight;
  CustomVariableBuffer customVariableBuffer;
  private Transaction transaction;
  private Item item;

  Event(long eventId, int userId, String accountId, int randomVal, int timestampFirst, int timestampPrevious, int timestampCurrent, int visits, String category, String action, String label, int value, int screenWidth, int screenHeight)
  {
    this.eventId = eventId;
    this.userId = userId;
    this.accountId = accountId;
    this.randomVal = randomVal;
    this.timestampFirst = timestampFirst;
    this.timestampPrevious = timestampPrevious;
    this.timestampCurrent = timestampCurrent;
    this.visits = visits;
    this.category = category;
    this.action = action;
    this.label = label;
    this.value = value;
    this.screenHeight = screenHeight;
    this.screenWidth = screenWidth;
  }

  Event(int userId, String accountId, String category, String action, String label, int value, int screenWidth, int screenHeight)
  {
    this(-1L, userId, accountId, -1, -1, -1, -1, -1, category, action, label, value, screenWidth, screenHeight);
  }

  public String toString()
  {
    return "id:" + this.eventId + " " + "random:" + this.randomVal + " " + "timestampCurrent:" + this.timestampCurrent + " " + "timestampPrevious:" + this.timestampPrevious + " " + "timestampFirst:" + this.timestampFirst + " " + "visits:" + this.visits + " " + "value:" + this.value + " " + "category:" + this.category + " " + "action:" + this.action + " " + "label:" + this.label + " " + "width:" + this.screenWidth + " " + "height:" + this.screenHeight;
  }

  public CustomVariableBuffer getCustomVariableBuffer()
  {
    return this.customVariableBuffer;
  }

  public void setCustomVariableBuffer(CustomVariableBuffer customVariableBuffer)
  {
    this.customVariableBuffer = customVariableBuffer;
  }

  public Transaction getTransaction()
  {
    return this.transaction;
  }

  public void setTransaction(Transaction paramTransaction)
  {
    if (!this.category.equals("__##GOOGLETRANSACTION##__"))
      throw new IllegalStateException("Attempted to add a transction to an event of type " + this.category);
    this.transaction = paramTransaction;
  }

  public Item getItem()
  {
    return this.item;
  }

  public void setItem(Item item)
  {
    if (!this.category.equals("__##GOOGLEITEM##__"))
      throw new IllegalStateException("Attempted to add an item to an event of type " + this.category);
    this.item = item;
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.Event
 * JD-Core Version:    0.6.0
 */