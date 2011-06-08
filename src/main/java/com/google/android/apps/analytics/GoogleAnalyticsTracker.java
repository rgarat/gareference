package com.google.android.apps.analytics;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GoogleAnalyticsTracker
{
  public static final String PRODUCT = "GoogleAnalytics";
  public static final String VERSION = "1.2";
  public static final String WIRE_VERSION = "4.6ma";
  private static final GoogleAnalyticsTracker INSTANCE = new GoogleAnalyticsTracker();
  public static final String LOG_TAG = "GoogleAnalyticsTracker";
  private boolean debug = false;
  private boolean dryRun = false;
  private String accountId;
  private Context parent;
  private ConnectivityManager connetivityManager;
  private String userAgentProduct = "GoogleAnalytics";
  private String userAgentVersion = "1.2";
  private int dispatchPeriod;
  private EventStore eventStore;
  private Dispatcher dispatcher;
  private boolean powerSaveMode;
  private boolean dispatcherIsBusy;
  private CustomVariableBuffer customVariableBuffer;
  private Map<String, Transaction> transactionMap = new HashMap();
  private Map<String, Map<String, Item>> itemMap = new HashMap();
  private Handler handler;
  private Runnable dispatchRunner = new Runnable()
  {
    public void run()
    {
      GoogleAnalyticsTracker.this.dispatch();
    }
  };

  public static GoogleAnalyticsTracker getInstance()
  {
    return INSTANCE;
  }

  public void start(String paramString, int paramInt, Context paramContext)
  {
    Object localObject1 = null;
    Object localObject2 = null;
    if (this.eventStore == null)
      localObject1 = new PersistentEventStore(new PersistentEventStore.DataBaseHelper(paramContext));
    else
      localObject1 = this.eventStore;
    if (this.dispatcher == null)
    {
      localObject2 = new NetworkDispatcher(this.userAgentProduct, this.userAgentVersion);
      ((Dispatcher)localObject2).setDryRun(this.dryRun);
    }
    else
    {
      localObject2 = this.dispatcher;
    }
    start(paramString, paramInt, paramContext, (EventStore)localObject1, (Dispatcher)localObject2);
  }

  public void start(String paramString, Context paramContext)
  {
    start(paramString, -1, paramContext);
  }

  void start(String paramString, int paramInt, Context paramContext, EventStore paramEventStore, Dispatcher paramDispatcher)
  {
    start(paramString, paramInt, paramContext, paramEventStore, paramDispatcher, new DispatcherCallbacks());
  }

  void start(String paramString, int paramInt, Context paramContext, EventStore paramEventStore, Dispatcher paramDispatcher, Dispatcher.Callbacks paramCallbacks)
  {
    this.accountId = paramString;
    this.parent = paramContext;
    this.eventStore = paramEventStore;
    this.eventStore.startNewVisit();
    this.dispatcher = paramDispatcher;
    this.dispatcher.init(paramCallbacks, this.eventStore.getReferrer());
    this.dispatcherIsBusy = false;
    if (this.connetivityManager == null)
      this.connetivityManager = ((ConnectivityManager)this.parent.getSystemService("connectivity"));
    if (this.handler == null)
      this.handler = new Handler(paramContext.getMainLooper());
    else
      cancelPendingDispatches();
    setDispatchPeriod(paramInt);
  }

  Dispatcher getDispatcher()
  {
    return this.dispatcher;
  }

  public void setProductVersion(String paramString1, String paramString2)
  {
    this.userAgentProduct = paramString1;
    this.userAgentVersion = paramString2;
  }

  public void trackEvent(String paramString1, String paramString2, String paramString3, int paramInt)
  {
    createEvent(this.accountId, paramString1, paramString2, paramString3, paramInt);
  }

  public void trackPageView(String paramString)
  {
    createEvent(this.accountId, "__##GOOGLEPAGEVIEW##__", paramString, null, -1);
  }

  private void createEvent(String paramString1, String paramString2, String paramString3, String paramString4, int paramInt)
  {
    Event localEvent = new Event(this.eventStore.getStoreId(), paramString1, paramString2, paramString3, paramString4, paramInt, this.parent.getResources().getDisplayMetrics().widthPixels, this.parent.getResources().getDisplayMetrics().heightPixels);
    localEvent.setCustomVariableBuffer(this.customVariableBuffer);
    this.customVariableBuffer = new CustomVariableBuffer();
    this.eventStore.putEvent(localEvent);
    resetPowerSaveMode();
  }

  public void setDispatchPeriod(int paramInt)
  {
    int i = this.dispatchPeriod;
    this.dispatchPeriod = paramInt;
    if (i <= 0)
    {
      maybeScheduleNextDispatch();
    }
    else if (i > 0)
    {
      cancelPendingDispatches();
      maybeScheduleNextDispatch();
    }
  }

  private void maybeScheduleNextDispatch()
  {
    if (this.dispatchPeriod < 0)
      return;
    if ((this.handler.postDelayed(this.dispatchRunner, this.dispatchPeriod * 1000)) && (this.debug))
      Log.v("GoogleAnalyticsTracker", "Scheduled next dispatch");
  }

  private void cancelPendingDispatches()
  {
    this.handler.removeCallbacks(this.dispatchRunner);
  }

  private void resetPowerSaveMode()
  {
    if (this.powerSaveMode)
    {
      this.powerSaveMode = false;
      maybeScheduleNextDispatch();
    }
  }

  public boolean dispatch()
  {
    if (this.debug)
      Log.v("GoogleAnalyticsTracker", "Called dispatch");
    if (this.dispatcherIsBusy)
    {
      if (this.debug)
        Log.v("GoogleAnalyticsTracker", "...but dispatcher was busy");
      maybeScheduleNextDispatch();
      return false;
    }
    NetworkInfo localNetworkInfo = this.connetivityManager.getActiveNetworkInfo();
    if ((localNetworkInfo == null) || (!localNetworkInfo.isAvailable()))
    {
      if (this.debug)
        Log.v("GoogleAnalyticsTracker", "...but there was no network available");
      maybeScheduleNextDispatch();
      return false;
    }
    if (this.eventStore.getNumStoredEvents() != 0)
    {
      Event[] arrayOfEvent = this.eventStore.peekEvents();
      this.dispatcher.dispatchEvents(arrayOfEvent);
      this.dispatcherIsBusy = true;
      maybeScheduleNextDispatch();
      if (this.debug)
        Log.v("GoogleAnalyticsTracker", "Sending " + arrayOfEvent.length + " to dispatcher");
      return true;
    }
    this.powerSaveMode = true;
    if (this.debug)
      Log.v("GoogleAnalyticsTracker", "...but there was nothing to dispatch");
    return false;
  }

  void dispatchFinished()
  {
    this.dispatcherIsBusy = false;
  }

  public void stop()
  {
    this.dispatcher.stop();
    cancelPendingDispatches();
  }

  EventStore getEventStore()
  {
    return this.eventStore;
  }

  public boolean setCustomVar(int paramInt1, String paramString1, String paramString2, int paramInt2)
  {
    try
    {
      CustomVariable localCustomVariable = new CustomVariable(paramInt1, paramString1, paramString2, paramInt2);
      if (this.customVariableBuffer == null)
        this.customVariableBuffer = new CustomVariableBuffer();
      this.customVariableBuffer.setCustomVariable(localCustomVariable);
    }
    catch (IllegalArgumentException localIllegalArgumentException)
    {
      return false;
    }
    return true;
  }

  public boolean setCustomVar(int paramInt, String paramString1, String paramString2)
  {
    return setCustomVar(paramInt, paramString1, paramString2, 3);
  }

  public String getVisitorCustomVar(int paramInt)
  {
    if ((paramInt < 1) || (paramInt > 5))
      throw new IllegalArgumentException("Index must be between 1 and 5 inclusive.");
    return this.eventStore.getVisitorCustomVar(paramInt);
  }

  public void addTransaction(Transaction paramTransaction)
  {
    this.transactionMap.put(paramTransaction.getOrderId(), paramTransaction);
  }

  public void addItem(Item paramItem)
  {
    Transaction localTransaction = (Transaction)this.transactionMap.get(paramItem.getOrderId());
    if (localTransaction == null)
    {
      Log.i("GoogleAnalyticsTracker", "No transaction with orderId " + paramItem.getOrderId() + " found, creating one");
      localTransaction = new Transaction.Builder(paramItem.getOrderId(), 0.0D).build();
      this.transactionMap.put(paramItem.getOrderId(), localTransaction);
    }
    Map<String,Item> localObject = (Map<String,Item> )this.itemMap.get(paramItem.getOrderId());
    if (localObject == null)
    {
      localObject = new HashMap<String,Item>();
      this.itemMap.put(paramItem.getOrderId(), localObject);
    }
    localObject.put(paramItem.getItemSKU(), paramItem);
  }

  public void trackTransactions()
  {
    Iterator localIterator1 = this.transactionMap.values().iterator();
    while (localIterator1.hasNext())
    {
      Transaction localTransaction = (Transaction)localIterator1.next();
      Event localEvent = new Event(this.eventStore.getStoreId(), this.accountId, "__##GOOGLETRANSACTION##__", "", "", 0, this.parent.getResources().getDisplayMetrics().widthPixels, this.parent.getResources().getDisplayMetrics().heightPixels);
      localEvent.setTransaction(localTransaction);
      this.eventStore.putEvent(localEvent);
      Map localMap = (Map)this.itemMap.get(localTransaction.getOrderId());
      if (localMap != null)
      {
        Iterator localIterator2 = localMap.values().iterator();
        while (localIterator2.hasNext())
        {
          Item localItem = (Item)localIterator2.next();
          localEvent = new Event(this.eventStore.getStoreId(), this.accountId, "__##GOOGLEITEM##__", "", "", 0, this.parent.getResources().getDisplayMetrics().widthPixels, this.parent.getResources().getDisplayMetrics().heightPixels);
          localEvent.setItem(localItem);
          this.eventStore.putEvent(localEvent);
        }
      }
    }
    clearTransactions();
    resetPowerSaveMode();
  }

  public void clearTransactions()
  {
    this.transactionMap.clear();
    this.itemMap.clear();
  }

  public void setDebug(boolean paramBoolean)
  {
    this.debug = paramBoolean;
  }

  public boolean getDebug()
  {
    return this.debug;
  }

  public void setDryRun(boolean paramBoolean)
  {
    this.dryRun = paramBoolean;
    if (this.dispatcher != null)
      this.dispatcher.setDryRun(paramBoolean);
  }

  public boolean isDryRun()
  {
    return this.dryRun;
  }

  final class DispatcherCallbacks
    implements Dispatcher.Callbacks
  {
    DispatcherCallbacks()
    {
    }

    public void dispatchFinished()
    {
      GoogleAnalyticsTracker.this.handler.post(new Runnable()
      {
        public void run()
        {
          GoogleAnalyticsTracker.this.dispatchFinished();
        }
      });
    }

    public void eventDispatched(long paramLong)
    {
      GoogleAnalyticsTracker.this.eventStore.deleteEvent(paramLong);
    }
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.GoogleAnalyticsTracker
 * JD-Core Version:    0.6.0
 */