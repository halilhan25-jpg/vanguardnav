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
 @PluginMethod public void syncPlaces(PluginCall call){
  JSArray places=call.getArray("places");if(places==null){call.reject("Noktalar eksik.");return;}
  getContext().getSharedPreferences("car",0).edit().putString("places",places.toString()).apply();call.resolve();
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
