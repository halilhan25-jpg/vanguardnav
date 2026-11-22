from pathlib import Path
import shutil,sys
root=Path(sys.argv[1]); overlay=Path(__file__).parent
app=root/"android/app/src/main"
def patch(path,old,new):
 s=path.read_text()
 if old not in s:raise RuntimeError(old)
 path.write_text(s.replace(old,new))
patch(root/"android/app/build.gradle","applicationId rootProject.ext.appId","applicationId 'tr.vanguardnav.atlas'")
patch(app/"AndroidManifest.xml",'android:label="@string/app_name"','android:label="VanguardNav"')
patch(app/"AndroidManifest.xml","</application>",'<activity android:name="app.organicmaps.FieldJournal" android:exported="false" android:windowSoftInputMode="adjustResize"/>\n</application>')
src=app/"java/app/organicmaps"
shutil.copy(overlay/"FieldJournal.java",src)
m=src/"MwmActivity.java"
patch(m,"    setContentView(R.layout.activity_map);","    setContentView(R.layout.activity_map);\n    installFieldControls();")
patch(m,"    Map.onCompassUpdated(north, false);","    fieldHeading=(int)((Math.round(Math.toDegrees(north))+360)%360); updateFieldLabel();\n    Map.onCompassUpdated(north, false);")
patch(m,"    dismissLocationErrorDialog();\n\n    final RoutingController routing","    fieldFix=location;updateFieldLabel();\n    dismissLocationErrorDialog();\n\n    final RoutingController routing")
patch(m,"UiUtils.setFullscreen(this, false);","fieldImmersive();")
patch(m,"UiUtils.setFullscreen(this, isFullscreen);","fieldImmersive();")
patch(m,"  private boolean isFullscreen()", (overlay/"controls.txt").read_text()+"\n  private boolean isFullscreen()")
