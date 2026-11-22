package tr.vanguardnav.field;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

@CapacitorPlugin(name="FieldTools",permissions={@Permission(alias="location",strings={Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION})})
public class FieldTools extends Plugin {
 @PluginMethod public void startTrack(PluginCall call){
  if(getPermissionState("location")!=PermissionState.GRANTED){requestPermissionForAlias("location",call,"locationResult");return;}
  launchRecorder(call);
 }
 @PermissionCallback private void locationResult(PluginCall call){
  if(getPermissionState("location")==PermissionState.GRANTED)launchRecorder(call);else call.reject("Kesin konum izni gerekli.");
 }
 private void launchRecorder(PluginCall call){
  try{
   Context c=getContext();TrailService.prepare(c);
   Intent i=new Intent(c,TrailService.class);
   if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);
   call.resolve();
  }catch(Exception e){call.reject("Konum kaydı başlatılamadı.",e);}
 }
 @PluginMethod public void stopTrack(PluginCall call){TrailService.active=false;getContext().stopService(new Intent(getContext(),TrailService.class));call.resolve();}
 @PluginMethod public void readTrack(PluginCall call){
  try{call.resolve(TrailService.snapshot(getContext()));}catch(Exception e){call.reject("GPS taslağı okunamadı.",e);}
 }
 @PluginMethod public void clearTrack(PluginCall call){
  if(TrailService.active){call.reject("Önce kaydı durdur.");return;}
  if(!TrailService.clear(getContext(),call.getString("id",""))){call.reject("GPS taslağı değişti.");return;}
  call.resolve();
 }
 static synchronized JSONObject readMap(Context c)throws Exception{
  android.util.AtomicFile file=new android.util.AtomicFile(new File(c.getFilesDir(),"car-map.json"));
  if(!file.getBaseFile().exists())return new JSONObject();
  return new JSONObject(new String(file.readFully(),StandardCharsets.UTF_8));
 }
 @PluginMethod public void syncPlaces(PluginCall call){
  JSArray places=call.getArray("places");if(places==null){call.reject("Noktalar eksik.");return;}
  try{
   android.util.AtomicFile file=new android.util.AtomicFile(new File(getContext().getFilesDir(),"car-map.json"));
   synchronized(FieldTools.class){
    FileOutputStream out=file.startWrite();
    try{JSONObject model=new JSONObject();model.put("pins",places);model.put("roads",call.getArray("roads"));model.put("regions",call.getArray("regions"));out.write(model.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}
    catch(Exception e){file.failWrite(out);throw e;}
   }
   getContext().getSharedPreferences("car",0).edit().putString("places",places.toString()).putLong("version",System.currentTimeMillis()).apply();call.resolve();
  }catch(Exception e){call.reject("Motor ekranı verileri yazılamadı.",e);}
 }
 @PluginMethod public void saveDocument(PluginCall call){
  Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(call.getString("mime","application/octet-stream"));
  i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,call.getString("filename","VanguardNav.json"));startActivityForResult(call,i,"writeDocument");
 }
 @ActivityCallback private void writeDocument(PluginCall call,ActivityResult result){
  if(call==null)return;if(result.getResultCode()!=Activity.RESULT_OK || result.getData()==null){call.reject("Dosya kaydı iptal edildi.");return;}
  try(OutputStream out=getContext().getContentResolver().openOutputStream(result.getData().getData(),"wt")){
   out.write(call.getString("text","").getBytes(StandardCharsets.UTF_8));call.resolve();
  }catch(Exception e){call.reject("Dosya yazılamadı.",e);}
 }
}
