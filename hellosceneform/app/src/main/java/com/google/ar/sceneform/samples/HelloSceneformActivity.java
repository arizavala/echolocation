/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.text.DecimalFormat;
import java.util.List;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
  private static final String TAG = HelloSceneformActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;

  private ArFragment arFragment;
  private ModelRenderable andyRenderable;
  private Session session;
  private int cameraHeight;
  private int cameraWidth;

    @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

    setContentView(R.layout.activity_ux);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

    // When you build a Renderable, Sceneform loads its resources in the background while returning
    // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
    ModelRenderable.builder()
        .setSource(this, R.raw.andy)
        .build()
        .thenAccept(renderable -> andyRenderable = renderable)
        .exceptionally(
            throwable -> {
              Toast toast =
                  Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
              toast.setGravity(Gravity.CENTER, 0, 0);
              toast.show();
              return null;
            });


      arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          if (andyRenderable == null) {
            return;
          }

          // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Get Pose
            Pose startPose = anchor.getPose();

          // Write coordinates to TextBox
            float x, y, z;
            x = startPose.tx();
            y = startPose.ty();
            z = startPose.tz();
            String message = "X = " + x + "\bY = " + y + "\bZ = " + z +
                    "\bDist = " + Math.sqrt(x*x + y*y + z*z);

          // Capture the layout's TextView and set the string as its text
            TextView textView = findViewById(R.id.DebugDisplay);
            textView.setText(message);

          // Create the transformable andy and add it to the anchor.
          TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
          andy.setParent(anchorNode);
          andy.setRenderable(andyRenderable);
          andy.select();
        });


    // Get Camera info from the Session and set camera to autofocus
    if(arFragment == null)
    {
        PrintDebug("Cant find arFragment");
        try{wait(10000);}
        catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }

      // Create task that runs every X time
      Handler handler = new Handler();
      Runnable runnable = new Runnable() {
          @Override
          public void run() {
              UpdateMeasurement();
              handler.postDelayed(this, 500);
          }
      };

        //Start
      handler.postDelayed(runnable, 500);
  }

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }

  void UpdateMeasurement()
  {
      if (session == null)
      {
          if(!ObtainSession());
      }

      // RAYCAST
      try {
          Frame frame = session.update();

          // Get the hit test from the midPoint
          List<HitResult> hitResults = frame.hitTest(cameraWidth/2, cameraHeight/2);
          if (hitResults.isEmpty())
          {
              PrintDebug("List of hitResults empty");
              return;
          }

          // Create the Anchor on first hitResult.
          Anchor anchor = hitResults.get(0).createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Get Pose
          Pose startPose = anchor.getPose();

          // Write coordinates to TextBox
          double x, y, z, distance;
          x = startPose.tx();
          y = startPose.ty();
          z = startPose.tz();
          //String message = "X = " + x + "\bY = " + y + "\bZ = " + z +
          //        "\bDist = " + Math.sqrt(x*x + y*y + z*z);

          distance = Math.sqrt(x*x + y*y + z*z);
          String message = "Dist = " + new DecimalFormat("#.0#").format(distance);

          // Capture the layout's TextView and set the string as its text
          PrintDebug(message);
      }
      catch (CameraNotAvailableException e) {
          e.printStackTrace();
      }
  }

  // Returns true if session was obtained;
  boolean ObtainSession()
  {
      // Get session
      ArSceneView arSceneView = arFragment.getArSceneView();
      if (arSceneView == null)
      {
          PrintDebug("Cant find arSceneView");
          try{wait(5000);}
          catch (InterruptedException e) {
              e.printStackTrace();
              return false;
          }
      }

      try{
          session = arSceneView.getSession();
          if (session == null)
          {
              PrintDebug("Session is null");
          }
      }
      catch (NullPointerException e) {
          e.printStackTrace();
          PrintDebug("Null pointer Exception getting Session");
      }

      if (session == null)
      {
          try{
              // Wait to show debug message for some time
              wait(5000);
              return false;
          }
          catch (InterruptedException e) {
              e.printStackTrace();
              return false;
          }
      }

    // Get camera pixel resolution
    cameraHeight = session.getCameraConfig().getImageSize().getHeight();
    cameraWidth = session.getCameraConfig().getImageSize().getWidth();

    // Set focus mode to auto
    Config configuration = session.getConfig();
    configuration.setFocusMode(Config.FocusMode.AUTO);

    return true;
  }

  void PrintDebug(String msg)
  {
    TextView textView = findViewById(R.id.DebugDisplay);
    textView.setText(msg);
  }
}
