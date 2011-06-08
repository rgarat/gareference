package com.google.android.apps.analytics;

abstract interface EventStore
{
  public abstract void putEvent(Event paramEvent);

  public abstract Event[] peekEvents();

  public abstract Event[] peekEvents(int paramInt);

  public abstract void deleteEvent(long paramLong);

  public abstract int getNumStoredEvents();

  public abstract int getStoreId();

  public abstract void setReferrer(String paramString);

  public abstract String getReferrer();

  public abstract void startNewVisit();

  public abstract String getVisitorCustomVar(int paramInt);
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.EventStore
 * JD-Core Version:    0.6.0
 */