package ru.zzzzzzerg.linden;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.opengl.GLSurfaceView;
import java.util.ArrayList;

import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;

import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.achievement.OnAchievementsLoadedListener;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.achievement.Achievement;

import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.appstate.AppStateBuffer;
import com.google.android.gms.appstate.AppState;
import com.google.android.gms.appstate.OnStateListLoadedListener;
import com.google.android.gms.appstate.OnStateDeletedListener;
import com.google.android.gms.appstate.OnStateLoadedListener;

import org.haxe.lime.HaxeObject;
import org.haxe.extension.Extension;

public class GooglePlay extends Extension
{
  public GooglePlay()
  {
    Log.d(tag, "Construct LindenGooglePlay");
  }

  /**
   * Called when an activity you launched exits, giving you the requestCode
   * you started it with, the resultCode it returned, and any additional data
   * from it.
   */
  public boolean onActivityResult (int requestCode, int resultCode, Intent data)
  {
    Log.i(tag, "onActivityResult");
    handleActivityResult(requestCode, resultCode, data);
    return true;
  }


  /**
   * Called when the activity is starting.
   */
  public void onCreate(Bundle savedInstanceState)
  {
  }


  /**
   * Perform any final cleanup before an activity is destroyed.
   */
  public void onDestroy()
  {
  }


  /**
   * Called as part of the activity lifecycle when an activity is going into
   * the background, but has not (yet) been killed.
   */
  public void onPause()
  {
  }


  /**
   * Called after {@link #onStop} when the current activity is being
   * re-displayed to the user (the user has navigated back to it).
   */
  public void onRestart()
  {
  }


  /**
   * Called after {@link #onRestart}, or {@link #onPause}, for your activity
   * to start interacting with the user.
   */
  public void onResume()
  {
  }


  /**
   * Called after {@link #onCreate} &mdash; or after {@link #onRestart} when
   * the activity had been stopped, but is now again being displayed to the
   * user.
   */
  public void onStart()
  {
    Log.i(tag, "Starting LindenGooglePlay.Games");
    ConnectionHandler gamesConnection = new ConnectionHandler("GAMES_CLIENT", tag,
          GOOGLE_PLAY_SIGN_IN_REQUEST, mainActivity);
    gamesClient = new GamesClient.Builder(mainContext, gamesConnection, gamesConnection)
      .setGravityForPopups(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
      .setScopes(Scopes.GAMES)
      .create();

    Log.i(tag, "Starting LindenGooglePlay.AppStates");
    ConnectionHandler statesConnection = new ConnectionHandler("APP_STATE_CLIENT", tag,
        GOOGLE_PLAY_APP_STATE_SIGN_IN_REQUEST, mainActivity);
    appStateClient = new AppStateClient.Builder(mainContext, statesConnection, statesConnection)
      .setScopes(Scopes.APP_STATE)
      .create();
  }


  /**
   * Called when the activity is no longer visible to the user, because
   * another activity has been resumed and is covering this one.
   */
  public void onStop()
  {
    Log.i(tag, "Stoping LindenGooglePlay");
    if(gamesClient != null)
    {
      gamesClient.disconnect();
      gamesClient = null;
      Log.i(tag, "LindenGooglePlay.Games stopped");
    }

    if(appStateClient != null)
    {
      appStateClient.disconnect();
      appStateClient = null;
      Log.i(tag, "LindenGooglePlay.AppStates stopped");
    }
  }

  private static HaxeObject callback = null;

  public static GamesClient gamesClient = null;
  public static AppStateClient appStateClient = null;

  public static String tag = "LindenGooglePlay";

  public static int result = ConnectionResult.DEVELOPER_ERROR;

  public static int GOOGLE_PLAY_SIGN_IN_REQUEST = 20201;
  public static int GOOGLE_PLAY_APP_STATE_SIGN_IN_REQUEST = 20202;
  public static int GOOGLE_PLAY_SHOW_ACHIEVEMENTS_REQUEST = 20203;
  public static int GOOGLE_PLAY_SHOW_LEADERBOARD_REQUEST = 20204;

  public static void handleException(Exception e, String where)
  {
    callHaxe("onException", new Object[] {e.getMessage(), where});
    e.printStackTrace();
    if(callback == null)
    {
      Log.w(tag, "Exception at " + where + ": " + e.toString());
    }
  }

  public static void gamesClientError(int code, String where)
  {
    callHaxe("onError", new Object[] {"GAMES_CLIENT", code, where});
    if(callback == null)
    {
      Log.w(tag, "Error at " + where + " with code = " + code);
    }
  }
  public static void appStateClientError(int code, String where)
  {
    callHaxe("onError", new Object[] {"APP_STATE_CLIENT", code, where});
    if(callback == null)
    {
      Log.w(tag, "Error at " + where + " with code = " + code);
    }
  }

  public static void callHaxe(final String name, final Object[] args)
  {
    if(callback != null)
    {
      callbackHandler.post(new Runnable()
          {
            public void run()
            {
              Log.d(tag, "Calling " + name + " from java");
              callback.call(name, args);
            }
          });
    }
  }

  public static void start(HaxeObject haxeCallback)
  {
    callback = haxeCallback;
  }

  public static boolean signInGamesClient()
  {
    boolean failed = false;

    Log.i(tag, "Signing In to GooglePlayGames");
    try
    {
      if(!gamesClient.isConnected() && !gamesClient.isConnecting())
      {
        callbackHandler.post(new Runnable()
            {
              public void run()
              {
                Log.i(tag, "Connecting to GamesClient");
                gamesClient.connect();
              }
            });
      }
      else if(gamesClient.isConnecting())
      {
        Log.w(tag, "GamesClient already connecting");
      }
      else if(gamesClient.isConnected())
      {
        Log.i(tag, "GamesClient already connected");
        connectionEstablished("GAMES_CLIENT");
      }
    }
    catch(Exception e)
    {
      failed = true;
      handleException(e, "signInGames");
    }

    return !failed;
  }

  public static boolean signInAppStateClient()
  {
    boolean failed = false;

    Log.i(tag, "Signing In to GooglePlay CloudSave");
    try
    {
      if(!appStateClient.isConnected() && !appStateClient.isConnected())
      {
        callbackHandler.post(new Runnable()
            {
              public void run()
              {
                Log.i(tag, "Connecting to AppStateClient");
                appStateClient.connect();
              }
            });
      }
      else if(appStateClient.isConnecting())
      {
        Log.w(tag, "AppStateClient already connecting");
      }
      else if(appStateClient.isConnected())
      {
        Log.i(tag, "AppStateClient already connected");
        connectionEstablished("APP_STATE_CLIENT");
      }
    }
    catch(Exception e)
    {
      failed = true;
      handleException(e, "signInCloudSave");
    }

    return !failed;
  }

  public static void signOutGamesClient()
  {
    Log.i(tag, "Signing Out from GooglePlayGames");

    if(gamesClient != null && gamesClient.isConnected())
    {
      gamesClient.signOut(); // FIXME: sign out with listener
      callHaxe("onSignedOut", new Object[] {"GAMES_CLIENT"});
    }
  }

  public static void signOutAppStateClient()
  {
    Log.i(tag, "Signing Out from GooglePlay CloudSave");

    if(appStateClient != null && appStateClient.isConnected())
    {
      appStateClient.signOut();
      callHaxe("onSignedOut", new Object[] {"APP_STATE_CLIENT"});
    }
    else if(appStateClient == null)
    {
      Log.e(tag, "appStateClient is null");
    }
    else if(appStateClient.isConnecting())
    {
      Log.e(tag, "appStateClient is connecting");
    }
    else
    {
      Log.e(tag, "appStateClient is not connected");
    }
  }

  public static void unlockAchievement(final String achievementId)
  {
    callbackHandler.post(new Runnable()
        {
          public void run()
          {
            try
            {
              Log.i(tag, "Unlocking achievement: " + achievementId);
              if(gamesClient != null && gamesClient.isConnected())
              {
                gamesClient.unlockAchievement(achievementId);
                Log.i(tag, "Achievement unlocked: " + achievementId);
              }
              else
              {
                Log.w(tag, "GamesClient not connected to unlock achievement");
              }
            }
            catch(Exception e)
            {
              handleException(e, "unlockAchievement");
            }
          }
        });
  }

  public static void incrementAchievement(final String achievementId, final int steps)
  {
    callbackHandler.post(new Runnable()
        {
          public void run()
          {
            try
            {
              Log.i(tag, "Incrementing achievement: " + achievementId + " with " + steps);
              if(gamesClient != null && gamesClient.isConnected())
              {
                gamesClient.incrementAchievement(achievementId, steps);
                Log.i(tag, "Achievement incremented: " + achievementId);
              }
              else
              {
                Log.w(tag, "GamesClient not connected to increment achievement");
              }
            }
            catch(Exception e)
            {
              handleException(e, "incrementAchievement");
            }
          }
        });
  }

  public static void showAchievements()
  {
    try
    {
      if(gamesClient != null && gamesClient.isConnected())
      {
        Log.i(tag, "Starting activity for show achievements");
        Intent intent = gamesClient.getAchievementsIntent();
        mainActivity.startActivityForResult(intent,
            GOOGLE_PLAY_SHOW_ACHIEVEMENTS_REQUEST);
      }
      else
      {
        Log.w(tag, "GamesClient not connected to show achievements");
      }
    }
    catch(Exception e)
    {
      handleException(e, "showAchievements");
    }
  }

  public static void submitScore(String leaderboardId, long score)
  {
    try
    {
      if(gamesClient != null && gamesClient.isConnected())
      {
        gamesClient.submitScore(leaderboardId, score);
        Log.i(tag, "Submit score " + score + " to " + leaderboardId);
      }
      else
      {
        Log.w(tag, "GamesClient not connected to submit score");
      }
    }
    catch(Exception e)
    {
      handleException(e, "submitScore");
    }
  }

  public static void showLeaderboard(String leaderboardId)
  {
    try
    {
      if(gamesClient != null && gamesClient.isConnected())
      {
        Intent intent = gamesClient.getLeaderboardIntent(leaderboardId);
        mainActivity.startActivityForResult(intent,
            GOOGLE_PLAY_SHOW_LEADERBOARD_REQUEST);

        Log.i(tag, "Starting activity for show leaderboard");
      }
      else
      {
        Log.w(tag, "GamesClient not connected to show leaderboard");
      }
    }
    catch(Exception e)
    {
      handleException(e, "showLeaderboard");
    }
  }

  public static void loadState(int stateKey)
  {
    try
    {
      if(appStateClient != null && appStateClient.isConnected())
      {
        appStateClient.loadState(new StateHandler("APP_STATE_CLIENT", tag), stateKey);
        Log.i(tag, "Loading state: " + stateKey);
      }
      else
      {
        Log.w(tag, "AppStates not connected to load state");
      }
    }
    catch(Exception e)
    {
      handleException(e, "loadState");
    }
  }

  public static void updateState(int stateKey, String data)
  {
    try
    {
      if(appStateClient != null && appStateClient.isConnected())
      {
        appStateClient.updateState(stateKey, data.getBytes());
        Log.i(tag, "Updating state: " + stateKey);
      }
      else
      {
        Log.w(tag, "AppStates not connected to update state");
      }
    }
    catch(Exception e)
    {
      handleException(e, "updateState");
    }
  }

  public static void resolveState(int stateKey, String version, String data)
  {
    try
    {
      if(appStateClient != null && appStateClient.isConnected())
      {
        appStateClient.resolveState(new StateHandler("APP_STATE_CLIENT", tag),
              stateKey, version, data.getBytes());
        Log.i(tag, "Resolving state: " + stateKey + " version: " + version);
      }
      else
      {
        Log.w(tag, "AppStates not connected to resolve state");
      }
    }
    catch(Exception e)
    {
      handleException(e, "resolveState");
    }
  }

  public static void deleteState(int stateKey)
  {
    try
    {
      if(appStateClient != null && appStateClient.isConnected())
      {
        appStateClient.deleteState(new StateHandler("APP_STATE_CLIENT", tag),
            stateKey);
        Log.i(tag, "Deleting state: " + stateKey);
      }
      else
      {
        Log.w(tag, "AppStates not connected to delete state");
      }
    }
    catch(Exception e)
    {
      handleException(e, "deleteState");
    }
  }

  public static boolean handleActivityResult(int rc, int resultCode,
      Intent data)
  {
    Log.i(tag, "handleActivityResult: " + rc + " " + resultCode);
    if(rc == GOOGLE_PLAY_SIGN_IN_REQUEST)
    {
      connectionEstablished("GAMES_CLIENT");
      return true;
    }
    else if(rc == GOOGLE_PLAY_APP_STATE_SIGN_IN_REQUEST)
    {
      connectionEstablished("APP_STATE_CLIENT");
      return true;
    }
    else if(rc == GOOGLE_PLAY_SHOW_ACHIEVEMENTS_REQUEST)
    {
      Log.i(tag, "Activity for show achievements handled");
      return true;
    }
    else if(rc == GOOGLE_PLAY_SHOW_LEADERBOARD_REQUEST)
    {
      Log.i(tag, "Activity for show leaderboard handled");
      return true;
    }

    return false;
  }

  public static void connectionEstablished(String what)
  {
    Log.d(tag, "Connection established");
    if(callback == null)
    {
      Log.d(tag, "Connection established, but connection callback is null");
    }
    else
    {
      callHaxe("onSignedIn", new Object[] {what});
    }

    if(what == "GAMES_CLIENT" && gamesClient != null && gamesClient.isConnected())
    {
      gamesClient.loadAchievements(new AchievementsHandler("GAMES_CLIENT", tag), false);
    }
    if(what == "APP_STATE_CLIENT" && appStateClient != null && appStateClient.isConnected())
    {
      appStateClient.listStates(new StateHandler("APP_STATE_CLIENT", tag));
    }
  }
}

class ConnectionHandler implements ConnectionCallbacks, OnConnectionFailedListener
{
  String what;
  String tag;
  int signInRequest;
  Activity mainActivity;

  ConnectionHandler(String what, String tag, int signInRequest, Activity mainActivity)
  {
    super();
    this.what = what;
    this.tag = tag;
    this.signInRequest = signInRequest;
    this.mainActivity = mainActivity;
  }

  public void onConnected(Bundle hint)
  {
    Log.i(tag, what + ": ConnectionHandler.onConnected");
    GooglePlay.connectionEstablished(what);
  }

  public void onDisconnected()
  {
    Log.i(tag, what + ": ConnectionHandler.onDisconnected");
    GooglePlay.result = ConnectionResult.SUCCESS;
  }

  public void onConnectionFailed(ConnectionResult result)
  {
    try
    {
      GooglePlay.result = result.getErrorCode();
      if(GooglePlay.result == ConnectionResult.SIGN_IN_REQUIRED)
      {
        Log.i(tag, what + ": ConnectionHandler: SignIn Required");
        result.startResolutionForResult(mainActivity, signInRequest);
      }
      else
      {
        Log.w(tag, what + ": ConnectionHandler.onConnectionFailed: " + GooglePlay.result);
      }
    }
    catch(Exception e)
    {
      Log.e(tag, what + ": ConnectionHandler.onConnectionFailed: " + e.toString());
      GooglePlay.handleException(e, "onConnectionFailed");
    }
  }
}

class AchievementsHandler implements OnAchievementsLoadedListener
{
  String what;
  String tag;

  AchievementsHandler(String what, String tag)
  {
    super();
    this.what = what;
    this.tag = tag;
  }

  public void onAchievementsLoaded(int statusCode, AchievementBuffer buffer)
  {
    Log.i(tag, what + ": AchievementsHandler.onAchievementsLoaded: " + statusCode);
    ArrayList<Achievement> achievements = new ArrayList<Achievement>();
    for(Achievement a : buffer)
    {
      //String id = a.getAchievementId();
      //int state = a.getState();
      //int type = a.getType();
      achievements.add(a);
    }

    buffer.close();
    GooglePlay.callHaxe("onAchievementsLoaded", achievements.toArray());
  }
}

class StateHandler implements OnStateListLoadedListener,
      OnStateDeletedListener,
      OnStateLoadedListener
{
  String what;
  String tag;

  StateHandler(String what, String tag)
  {
    super();
    this.what = what;
    this.tag = tag;
  }

  public void onStateListLoaded(int statusCode, AppStateBuffer buffer)
  {
    Log.i(tag, what + ": StateHandler.onStateListLoaded: " + statusCode);
    ArrayList<AppState> states = new ArrayList<AppState>();
    for(AppState s : buffer)
    {
      //String version = s.getLocalVersion();
      //int key = s.getKey();
      states.add(s);
      Log.i(tag, s.toString());
    }

    buffer.close();
    GooglePlay.callHaxe("onStateListLoaded", states.toArray());
  }

  public void onStateDeleted(int statusCode, int stateKey)
  {
    Log.i(tag, what + ": StateHandler.onStateDeleted: " +
        statusCode + " key = " + stateKey);
    GooglePlay.callHaxe("onStateDeleted", new Object[]{statusCode, stateKey});
  }

  public void onStateConflict(int stateKey, String resolvedVersion,
      byte[] localData, byte[] serverData)
  {
    try
    {
      Log.w(tag, what + ": StateHandler.onStateConflict: key = " +
          stateKey + " version = " + resolvedVersion);

      String localString = new String(localData);
      String serverString = new String(serverData);

      GooglePlay.callHaxe("onStateConflict",
          new Object[] {stateKey, resolvedVersion, localString, serverString});
    }
    catch(Exception e)
    {
      GooglePlay.handleException(e, "onStateConflict");
    }
  }

  public void onStateLoaded(int statusCode, int stateKey, byte[] localData)
  {
    try
    {
      Log.i(tag, what + ": StateHandler.onStateLoaded: " +
          statusCode + " key = " + stateKey);

      if(statusCode == AppStateClient.STATUS_OK)
      {
        String data = new String(localData);
        GooglePlay.callHaxe("onStateLoaded",
            new Object[] {stateKey, data, false});
      }
      else if(statusCode == AppStateClient.STATUS_NETWORK_ERROR_STALE_DATA)
      {
        Log.i(tag, "Load possible out-of-sync cached data");
        String data = new String(localData);
        GooglePlay.callHaxe("onStateLoaded",
            new Object[] {stateKey, data, true});
      }
      else if(statusCode == AppStateClient.STATUS_STATE_KEY_NOT_FOUND)
      {
        GooglePlay.callHaxe("onStateNotFound",
            new Object[] {stateKey});
      }
      else
      {
        GooglePlay.appStateClientError(statusCode, "onStateLoaded");
      }
    }
    catch(Exception e)
    {
      GooglePlay.handleException(e, "onStateLoaded");
    }
  }
}

